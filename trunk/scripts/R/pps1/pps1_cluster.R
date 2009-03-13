library(plotrix)
library(limma)
library(goTools)
library(GOstats)

source("pathway-go.R")
source("top-go.R")
source("go-venn.R")

path = "/home/thomas/projects/pps/path_results/"

##### Combine all zscore files (for MeV visualization)
source("mergeZscores.R")
files = c(
	"zscore_Liver_p1.txt",
	"zscore_Liver_p5.txt",
	"zscore_Liver_p6.7.8.txt",
	"zscore_Muscle_p1.txt",
	"zscore_Muscle_p2.3.4.txt",
	"zscore_Muscle_p5.txt",
	"zscore_Muscle_p6.7.8.txt",
	"zscore_WAT_p1.txt",
	"zscore_WAT_p2.3.4.txt",
	"zscore_WAT_p5.txt",
	"zscore_WAT_p6.7.8.txt"
)
total = mergeZscores(files, path)
write(digitalize(total), "combined_zscores_lt2.txt", path)

files = c(
	"zscore_Liver_pany.txt",
	"zscore_Muscle_pany.txt",
	"zscore_WAT_pany.txt"
)
total.pany = mergeZscores(files, path)
write(digitalize(total.pany), "combined_zscores_lt2_pany.txt", path)

minZ = 2
onto = "BP"

##### GO Enrichment based on GO2Pathway mappings and pathway z-scores
output = paste(path, "topGO/", sep = "")
#Input 1: A file that maps pathways to GO categories (output from the java gpml-go-mapper)
mappingFile = "/home/thomas/projects/go-pathway-mapping/mm-mapping-proc-pruned0.5.txt"

## Enrichment with lists of z-scores
#Input 2: A list of z-scores for pathways (output from PathVisio)
zscoreFile = paste(path, "zscore_WAT_p1.txt", sep = "")

goData = pathwayGO(mappingFile, zscoreFile, onto, minZ)

test.clas <- new("classicCount", testStatistic = GOFisherTest,
	name = "Fisher test - classic")
result.clas <- getSigGroups(goData, test.clas)

test.elim <- new("elimCount", testStatistic = GOFisherTest,
	name = "Fisher test - elim", cutOff = 0.01)
result.elim <- getSigGroups(goData, test.elim)

test.weight <- new("weightCount", testStatistic = GOFisherTest,
	name = "Fisher test - weight", sigRatio = "ratio")
result.weight <- getSigGroups(goData, test.weight)

allRes <- GenTable(goData, classic = result.clas, elim = result.elim, weight = result.weight, orderBy = "weight",
	ranksOf = "classic", topNodes = 20)

prefix = paste(output, "WAT_pr5", sep="")
printGraph(goData, result.weight, firstSigNodes = 10, result.clas, fn.prefix = prefix, useInfo = "all")
printGraph(goData, result.clas, firstSigNodes = 10, result.clas, fn.prefix = prefix, useInfo = "all")
printGraph(goData, result.elim, firstSigNodes = 10, result.clas, fn.prefix = prefix, useInfo = "all")

## Overlap stuff on pathway z-score
createVenn(goData, terms = c("GO:0009987", "GO:0008152", "GO:0065007"))
graph = graph(goData)
toplevel = GOBPCHILDREN[[getGraphRoot(graph)]]
createIntersect(goData, toplevel, TRUE)

##### Classic GO enrichment based on genes in pathway-GO mappings only
outDir = paste(path, "topGO/", sep="")
onto = "BP"
dataFile = "/home/thomas/projects/pps/filtered_data/Combined_total.txt"
data = read.delim(dataFile, row.names=1, na.strings = "NaN")

testCols = c(
	"WAT_oriogen_profile",
	"Liver_oriogen_profile",
	"Muscle_oriogen_profile"
)
testClusters = list(c(1,2,3,4,5,6,7,8), 1, c(2,3,4), 5, c(6,7,8))

clusterNames = c()
for(cluster in testClusters) {
	clusterNames = append(clusterNames, paste(cluster, collapse = "."))
}
results.pwgene = array(list(), dim = c(length(testCols), length(testClusters)), dimnames = list(testCols, clusterNames))

for(col in testCols) {
	for(cluster in testClusters) {
		print(paste("Processing ", col, paste(cluster, collapse = ".")))
		goData = createGoDataAnnot(
			data, col = col, cluster = cluster,
			onto = onto, geneFile = "/home/thomas/projects/go-pathway-mapping/mm-mapping-proc-pruned0.5-xrefsL.txt")
		goRes = goTests(goData, onto = onto)
		results.pwgene[[col, paste(cluster, collapse = ".")]] = goRes
	}
}

row = testCols[1]; col = clusterNames[[1]]

createIntersect(results.pwgene[[row,col]]$goData, c("GO:0009987", "GO:0008152", "GO:0065007"), TRUE)
dev.new()
createVenn(results.pwgene[[row,col]]$goData, terms = c("GO:0009987", "GO:0008152", "GO:0065007"))
dev.new()
createVenn(results.pwgene[[row,col]]$goData, terms = c("GO:0009987", "GO:0008152", "GO:0065007"), sig = FALSE)

##### Classic GO enrichment
results = array(list(), dim = c(length(testCols), length(testClusters)), dimnames = list(testCols, clusterNames))

for(col in testCols) {
	for(cluster in testClusters) {
		goData = createGoData(data, col = col, cluster = cluster, onto = onto)
		goRes = goTests(goData, onto = onto)
		results[[col, paste(cluster, collapse = ".")]] = goRes
	}
}

row = 1
col = 1
createVenn(results[[row,col]]$goData, terms = c("GO:0009987", "GO:0008152", "GO:0065007"))
createVenn(results[[row,col]]$goData, terms = c("GO:0009987", "GO:0008152", "GO:0065007"), sig = FALSE)

graph = graph(results[[row,col]]$goData)
toplevel = GOBPCHILDREN[[getGraphRoot(graph)]]

createIntersect(results[[row,col]]$goData, toplevel, TRUE)

##### Comparison of 3 GO approaches
outDir = paste(path, "topGO/compare/", sep="")
onto = "BP"
cluster = c(1,2,3,4,5,6,7,8)
tissue = "WAT"
minZ = 2

col = paste(tissue, "_oriogen_profile", sep="")
zscoreFile = paste(path, "zscore_", tissue, "_pany.txt", sep = "")
dataFile = "/home/thomas/projects/pps/filtered_data/Combined_total.txt"
data = read.delim(dataFile, row.names=1, na.strings = "NaN")

goData.p = pathwayGO("/home/thomas/projects/go-pathway-mapping/mm-mapping-proc-pruned0.5.txt", zscoreFile, onto, minZ)
goData.pg = goData = createGoDataAnnot(data, col = col, cluster = cluster,
				onto = onto, geneFile = "/home/thomas/projects/go-pathway-mapping/mm-mapping-proc-pruned0.5-xrefsL.txt")
goData.g = createGoData(data, col = col, cluster = cluster, onto = onto)

res.p = goTests(goData.p, onto = onto)
res.pg = goTests(goData.pg, onto = onto)
res.g = goTests(goData.g, onto = onto)

results = list(res.p = res.p, res.pg = res.pg, res.g = res.g)

for(rn in names(results)) {
	write.table(results[[rn]]$result.all, file=paste(outDir, "topterms", rn, ".txt", sep = ""), row.names=FALSE, quote = FALSE, sep = "\t")
	printGraph(results[[rn]]$goData, results[[rn]]$result.weight, firstSigNodes = 10, 
		results[[rn]]$result.clas, fn.prefix = paste(outDir, rn, sep=""), useInfo = "all", title=)
}

png(file = paste(output, "venn-pathway-p.png", sep=""), width = 1200, height = 1000)
createVenn(goData.p, terms = c("GO:0009987", "GO:0008152", "GO:0065007"), main = "GO to pathway mappings", sig = TRUE)
dev.off()

png(file = paste(output, "venn-pathway-pg.png", sep=""), width = 1200, height = 1000)
createVenn(goData.pg, terms = c("GO:0009987", "GO:0008152", "GO:0065007"), main = "GO to gene mappings (genes based on pathways)", sig = TRUE)
dev.off()

png(file = paste(output, "venn-pathway-g.png", sep=""), width = 1200, height = 1000)
createVenn(goData.g, terms = c("GO:0009987", "GO:0008152", "GO:0065007"), main = "GO to gene mappings", sig = TRUE)
dev.off()

#### Coverage control of GO - Pathway mapping
# Print a complete GO graph to 2nd level (previous graph only includes terms that map to pathway)
## TODO3: perform enrichment study with all GO genes as reference and Pathway genes as subset
leaves = CustomEndNodeList("GO:0008150",rank=1)
leaves = append(leaves, CustomEndNodeList("GO:0008152"))
leaves = append(leaves, CustomEndNodeList("GO:0009987"))
graph = GOGraph(unique(leaves), GOBPPARENTS)
ns = nodes(graph)

##Gather gene counts
go.n = vector("character", length=length(ns))
pg.n = vector("character", length=length(ns))
p.n = vector("character", length=length(ns))
for(i in 1:length(ns)) {
	go.g = genesInTerm(goData.g, whichGO = ns[i])
	go.pg = genesInTerm(goData.pg, whichGO = ns[i])
	go.p = genesInTerm(goData.p, whichGO = ns[i])
	
	go.n[i] = ifelse(length(go.g) > 0, length(go.g[[1]]), 0)
	pg.n[i] = ifelse(length(go.pg) > 0, length(go.pg[[1]]), 0)
	p.n[i] = ifelse(length(go.p) > 0, length(go.p[[1]]), 0)
}

names = vector("character", length=length(ns))
for(i in 1:length(names)) {
	l = genesInTerm(goData.p, whichGO = ns[i])
	if(length(l) == 0) { 
		l = 0
	} else {
		l = length(l[[1]]) 
	}
	pct = ifelse(pg.n[i] == "0", 0, as.numeric(pg.n[i]) / as.numeric(go.n[i])) * 100
	names[[i]] = paste(substr(Term(GOTERM[[ns[i]]]), 1, 10), "\\\n", p.n[i], "\\\n", round(pct, 3), "%", sep="")
}
natt = makeNodeAttrs(graph, shape = "box", label = names, fontsize = 15)

colors = colorRamp(c("white", "blue"), space="rgb")
for(i in 1:length(ns)) {
	npg = as.numeric(pg.n[i])
	if(npg > 0) {
		clr = colors(npg/ as.numeric(go.n[i]))
	} else {
		clr = c(255, 255, 255)
	}
	natt$fillcolor[[ns[i]]] = rgb(clr[1], clr[2], clr[3], maxColorValue = 255)
}
#fixInNamespace(drawTxtLabel, "Rgraphviz") #add cex = 2 to text() call

lgraph = agopen(graph, layoutType = "dot", nodeAttrs = natt, name = "")
png(file = paste(output, "pathway-go-coverage3rd.png", sep=""), width = 3000, height = 2000)
plot(lgraph)
dev.off()

png(file = paste(output, "venn-pathway-go.png", sep=""), width = 1200, height = 1000)
createVenn(goData.noz, terms = c("GO:0009987", "GO:0008152", "GO:0065007"), main = "GO to pathway mappings", sig = FALSE)
dev.off()
