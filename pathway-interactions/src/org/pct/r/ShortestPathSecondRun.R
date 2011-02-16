###############################################################
## This script performs the second run of iterations for the ##
## pps2 pathway interactions analysis.                       ##
## In this run, another set of permutations is run for the   ##
## most significant results, to increase the resolution of   ##
## the p-value.                                              ##
###############################################################

src = "scripts/"
source(paste(src, "ShortestPath.R", sep=""))
source(paste(src, "PathwaySampler.R", sep=""))
source(paste(src, "PPS2Functions.R", sep=""))

## The gene-level statistics to run the analysis for
task = getTestNames() # Run on all statistics

## Analysis parameters
maxP = 0.1 #Run another set of permutations on all pairs with p-value <= maxP
strength = T
nit = 1000 #Number of iterations

dataFile = "sps.data.RData"
load(dataFile)

if(!strength) { #Only need to sample pathways once
	perm = generatePermutations("sps.shuf.ds.2nd.RData", pathways, interactions, nit, strength)
	shuffled = perm$shuffled
	sampler = perm$sampler
}

for(t in task) {
	message("Second run for ", t)
	f = paste("sp.pps2.", ifelse(strength, "gs", "ds"), ".", t, ".RData", sep="")
	load(f)

	#Calculate the p-values
	scores = toScore(sps)
	scores.dist = lapply(spsdist, toScore)
	stats = scoreTests(scores, scores.dist)

	#Find the pairs that have permP > maxP, don't include these in further analysis
	exclude = each(stats$permP, function(p) { p > maxP })
	message("Excluding ", sum(exclude), " pairs")
	message("Including ", sum(!exclude), " pairs")
	
	if(strength) { #Need to sample pathways for each set of weights
		shuf.file = paste("sps.shuf.gs.2nd.", basename(f), sep="")
		perm = generatePermutations(shuf.file, pathways, interactions, nit, strength)
		shuffled = perm$shuffled
		sampler = perm$sampler
	}

	pxgraph = weightPathwayXrefGraph(interactions, pathways)

	message("Running permutations")
	start = date()
	spsdist = foreach(i=1:nit, .inorder=T, .packages=getPackages()) %dopar% {
		registerDoSEQ()
		message(date(), "Permutation:\t", i)
		p = shuffled[[i]]
		s = shortestPaths(interactions, pathways, sampledPathways = p, pxgraph = pxgraph, max.length = max.length, neighborhood = neighborhood, excludePairs = exclude, scoreFun = pathToScore)
		matrix.as.numeric(s)
	}

	save(max.length, neighborhood, sps, strength, spsdist, exclude, interactions, file = paste("2nd.", basename(f), sep=""))

	message("Started at: ", start)
	message("Ended at: ", date())
}
