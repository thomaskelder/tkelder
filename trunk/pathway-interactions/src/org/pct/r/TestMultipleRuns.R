############################################################
## Compare results of multiple runs (to test permutation) ##
############################################################

src = "scripts/"
source(paste(src, "ShortestPath.R", sep=""))
source(paste(src, "PPS2Functions.R", sep=""))

load("output/multiple_runs/sps.data.RData")

## Load label data ##
#Pathway labels
pathwayTitles = readPathwayTitles("input/pathways_merged")

cleanup = function() {
	rm(sps, inherits=T)
	rm(spsdist, inherits=T)
	rm(interactions, inherits=T)
	gc()
}

analyze = function(spsFile, shufFile, secondFile) {
	load(spsFile)
	load(shufFile)

	r = list()

	r$scores = toScore(sps)
	r$scores.dist = lapply(spsdist, toScore)
	
	load(secondFile)
	r$stats = scoreTests(r$scores, r$scores.dist, spsdist)
	
	r$graph = scoresAsGraph(
		r$scores, stats = r$stats, pathways = pathways, 
		pathwayTitles = pathwayTitles
	)
	
	list(scores = r$scores, stats = r$stats, graph = r$graph)
}

runs = c(1,2)
base = "LF_time_F"
r = list()
for(i in runs) {
	r[[i]] = analyze(
		paste("output/multiple_runs/sp.pps2.", i, ".", base, ".RData", sep=""),
		paste("output/multiple_runs/sps.shuf.", i, ".RData", sep=""),
		paste("output/multiple_runs/2nd.sp.pps2.", i, ".", base, ".RData", sep="")
	)	
}

collectEdgeAttr = function(r, n1, n2, att) {
	a1 = edgeAttributesByName(r[[n1]]$graph, att)
	a2 = edgeAttributesByName(r[[n2]]$graph, att)
	allEdges = unique(c(names(a1), names(a2)))
	a = matrix(NA, nrow = length(allEdges), ncol = 2)
	rownames(a) = allEdges		
	colnames(a) = c(n1, n2)

	a[names(a1), n1] = a1
	a[names(a2), n2] = a2
	a
}

vennP = function(r, n1, n2, p, cutoff = 0.01) {
	library(limma)
	sig = collectEdgeAttr(r, n1, n2, p)
	sig = sig < cutoff

	vennDiagram(sig)
}

vennP(r, 1, 2, "permP", 0.001)

ps = collectEdgeAttr(r, 1, 2, "permP")
plot(ps[,1], ps[,2], log='xy')

library("geneplotter")
smoothScatter(log10(ps[,1]+1E-4), log10(ps[,2]+1E-4))

merged = mergeGraphs(lapply(r, function(x) x$graph), overwrite = list.vertex.attributes(r[[1]]$graph))
write.graph(merged, "output/ds_vs_gs/ds_vs_gs.gml", format="gml")
fixGml("output/ds_vs_gs/ds_vs_gs.gml", unique(c(list.vertex.attributes(merged), list.edge.attributes(merged))))
