## Performs GSEA on gene sets that are centered around a single gene
source("network/readSif.R")
source("pathway/pgsea.R")

dataPath = "/home/thomas/projects/pps1/filtered_data/"
statPath = "/home/thomas/projects/pps1/stat_results/"
pathwayPath = "/home/thomas/data/pathways/pps1-entrez/"
outPath = "/home/thomas/projects/pps1/path_results/pathway/"
outPath.sif = paste(outPath, "network/", sep="")

## Load previously calculated data (pps1_perpare_data.R and pps1_anova.R)
load(paste(dataPath, "pps1_log2_combined.Rd", sep=""))
load(paste(statPath, "pps1_anova_pqvalues.Rd", sep=""))

cols = as.character(lapply(levels(tissueFactor), 
	function(tis) { paste("anova_qvalue", tis, sep="_") }))

gseData = 1 - anovaData[, cols] # 1-q as gene score
rownames(gseData) = entrezIds
colnames(gseData) = levels(tissueFactor)

#Sets based on number of pathways the node occurs in
nodePw = readSif("/home/thomas/projects/pps1/path_results/pathway/network/mc1_all.sif")

nodePw.n = lapply(nodePw, length)
nodePw.order = order(as.numeric(nodePw.n), decreasing = TRUE)

top = nodePw.n[nodePw.order[1:30]]
top.s = top
names(top.s) = as.character(toSymbols(names(top)))

hist(as.numeric(nodePw.n), breaks = 50)

pwNode = invertSif(nodePw)

#Create a set that contains all genes that are in the same pathway
#as the gene the set is based on
minInPws = 5
sets.pw = lapply(nodePw[nodePw.n >= minInPws], function(pws) {
	unique(as.character(unlist(pwNode[pws])))
})
names(sets.pw) = toSymbols(names(sets.pw))

hist(as.numeric(lapply(sets.pw, length)), breaks = 50)

gsets.pw = createGeneSetCollection(sets.pw)

gseResults = doPGSEA(
	geneSets = gsets.pw, 
	data = gseData, 
	range = c(5, 1000)
)
gseData.q = -(gseData - 1)
writePGSEA(
	pathways = sets.pw, 
	gseResults = gseResults, 
	gseData = gseData.q, 
	filePrefix = paste(outPath, "hubsets_pw_page_anova_q", sep="")
)

#Sets based on connections defined in pathways
minCon = 5
nodeInt = readSif("/home/thomas/data/networks/pps1_pathways.sif")
intNode = invertSif(nodeInt)
nodeInt.n = lapply(nodeInt, length)
nodeInt.order = order(as.numeric(nodeInt.n), decreasing = TRUE)

sets.int = nodeInt[nodeInt.n >= minCon]
sets.int.symbol = sets.int
names(sets.int.symbol) = toSymbols(names(sets.int.symbol))

gsets.int.symbol = createGeneSetCollection(sets.int.symbol)

gseResults.int = doPGSEA(
	geneSets = gsets.int.symbol, 
	data = gseData, 
	range = c(5, 1000)
)
gseData.q = -(gseData - 1)
writePGSEA(
	pathways = sets.int.symbol, 
	gseResults = gseResults.int, 
	gseData = gseData.q, 
	filePrefix = paste(outPath, "hubsets_int_page_anova_q", sep="")
)

topZ = 2
for(tis in levels(tissueFactor)) {
	## Write a sif file containing all nodes with z >= 2
	writeSif(sets.int[gseResults.int[[tis]]$results >= 2], file = paste(outPath.sif, "nodes.interactions.z2_", tis, ".sif", sep=""))
}

## Per time point on foldchange (relative to t0)
gseData = relTotalData
rownames(gseData) = entrezIds

gseResults = doPGSEA(
	geneSets = gsets.int.symbol, 
	data = gseData, 
	range = c(10, 1000)
)

library(qvalue)

anovaPvalues<-function(x,factor){
	ttest <- function(x){
		m<-data.frame(factor, x);
		anova(aov(x ~ factor, m));
	};
	anova.res<-apply(x, 1, ttest);
	as.numeric(lapply(anova.res, function(x){x["Pr(>F)"][1,]}));
};

gse.anova.p = list()

gseResults.table = NULL
for(col in names(gseResults)) {
	if(is.null(gseResults.table)) {
		gseResults.table = gseResults[[col]]$results
	} else {
		gseResults.table = cbind(gseResults.table, gseResults[[col]]$results)
	}
}
colnames(gseResults.table) = names(gseResults)
	
for(tis in levels(tissueFactor)) {
	factor = relTimeFactor[relTissueFactor == tis]
	data = gseResults.table[, names(gseResults)[relTissueFactor == tis]]
	subset = apply(data, 1, function(row) {
		sum(is.na(row)) != length(row)
	})
	gse.anova.p[[tis]] = anovaPvalues(data[subset,], factor)
	names(gse.anova.p[[tis]]) = rownames(data[subset,])
}

tis = "WAT"
o = names(gse.anova.p[[tis]])[order(gse.anova.p[[tis]])]
smcPlot(gseResults.table[o[1:30],], scale = c(-10, 10))

writeSif(sets.int[c("12845", "21828", "640441")], file = paste(outPath.sif, "nodes.interactions.top3_", tis, ".sif", sep=""))
