###############################################################
## This script performs the first run of iterations for the  ##
## pps2 pathway interactions analysis.                       ##
###############################################################

src = "scripts/"
source(paste(src, "ShortestPath.R", sep=""))
source(paste(src, "PathwaySampler.R", sep=""))
source(paste(src, "PPS2Functions.R", sep=""))

## The gene-level statistics to run the analysis for
task = getTestNames() # Run on all statistics

## Analysis parameters
max.length = 0.9
neighborhood = 5 # maximal 3 proteins in between a pathway pair
nit = 100 # number of iterations
strength = T # use graph.strength for sampling genes (instead of degree)

if(file.exists("sps.data.RData")) {
	message("Loading data from cache")
	load("sps.data.RData")
} else {
	message("Reading interaction networks")
	interactions = readInteractionsDirected("input/interactions/mouse-directed")
	interactions = simplify(interactions) #Remove loops and multiple edges
	#Delete nodes without connections
	interactions = delete.vertices(
		interactions, which(degree(interactions) == 0) - 1
	)

	weights = list()
	for(t in task) {
			weights[[t]] = readWeights("input/weights/pps2.2log.stats.mapped.txt", interactions, t)
	}

	pathways = readPathways("input/pathways_merged")

	save(interactions, weights, pathways, file = "sps.data.RData")
}

message("Setting up permutations")

if(!strength) { #Only need to sample pathways once
	perm = generatePermutations("sps.shuf.ds.RData", pathways, interactions, nit, strength)
	shuffled = perm$shuffled
	sampler = perm$sampler
}

for(t in task) {
	message("Running ", t)
	#Make sure weights are not 0, may cause igraph shortestpath to hang
	V(interactions)$weight = weights[[t]]$sigmNodeWeights + 1E-10
	E(interactions)$weight = weights[[t]]$sigmEdgeWeights + 1E-10
	
	if(strength) { #Need to sample pathways for each set of weights
		shuf.file = paste("sps.shuf.gs.", t, ".RData", sep="")
		perm = generatePermutations(shuf.file, pathways, interactions, nit, strength)
		shuffled = perm$shuffled
		sampler = perm$sampler
	}

	pxgraph = weightPathwayXrefGraph(interactions, pathways)

	message("Running first shortest path calculation")
	sps = shortestPaths(interactions, pathways, max.length = max.length, neighborhood = neighborhood, pxgraph = pxgraph)
	
	exclude = each(sps, function(x) is.na(x[1]))
	message("Running permutations")
	start = date()
	spsdist = foreach(i=1:nit, .inorder=T, .packages=getPackages()) %dopar% {
		registerDoSEQ()
		message(date(), "Permutation:\t", i)
		p = shuffled[[i]]
		shortestPaths(interactions, pathways, sampledPathways = p, pxgraph = pxgraph, max.length = max.length, neighborhood = neighborhood, excludePairs = exclude)
	}

	save(max.length, neighborhood, sps, strength, spsdist, exclude, interactions, file = paste("sp.pps2.", ifelse(strength, "gs", "ds"), ".", t, ".RData", sep=""))

	message("Started at: ", start)
	message("Ended at: ", date())
}
