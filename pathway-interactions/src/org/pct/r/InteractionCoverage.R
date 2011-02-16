################################################################
## Script to generate stats on the coverage of genes/proteins ##
## in the pps2 analysis.                                      ##
################################################################

library(limma)
src = "scripts/"
source(paste(src, "ShortestPath.R", sep=""))

message("Loading interaction networks")
interactions = readInteractionsDirected("input/interactions/mouse-directed")
interactions = simplify(interactions) #Remove loops and multiple edges
#Delete nodes without connections
interactions = delete.vertices(
	interactions, which(degree(interactions) == 0) - 1
)
interactions.orig = interactions

#Remove falsely annotated genes (especially from Pazar)
correct = grep("EnMm:ENSMUS", V(interactions)$name) - 1
interactions = subgraph(interactions, correct)

message("Reading pathways")
pathways = readPathways("input/pathways_merged")

message("Reading weights")
rawWeights = read.delim("input/weights/pps2.2log.stats.mapped.txt", as.is=T)
testCols = colnames(rawWeights)[grep("_q", colnames(rawWeights))]

pathwayXrefs = unique(unlist(pathways))
intXrefs = V(interactions)$name
weightXrefs = unique(rawWeights[,'mapped_xref'])

library(gplots)
xrefs = list(pathway = pathwayXrefs, interactions = intXrefs, data = weightXrefs)
venn(xrefs)

allXrefs = unique(c(pathwayXrefs, intXrefs, weightXrefs))

gvenn = t(sapply(allXrefs, function(x) {
	c(x %in% pathwayXrefs, x %in% intXrefs, x %in% weightXrefs)
}))
colnames(gvenn) = c('Pathways', 'Interactions', 'Transcriptomics dataset')

png("output/images/coverage_venn.png", 1024, 1024)

par(cex=2, cex.main=1.5, mar=c(1,1,1,1) + 0.1, oma=c(0,0,1,0))
vennDiagram(gvenn, main="Gene coverage of different resources")

dev.off()

