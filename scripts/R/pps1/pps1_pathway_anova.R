## Perform enrichment analysis on pathways using the PGSEA library

dataPath = "/home/thomas/projects/pps1/filtered_data/"
statPath = "/home/thomas/projects/pps1/stat_results/"
pathwayPath = "/home/thomas/data/pathways/pps1-entrez/"
outPath = "/home/thomas/projects/pps1/path_results/pathway/"

## Load previously calculated data (pps1_perpare_data.R and pps1_anova.R)
load(paste(dataPath, "pps1_log2_combined.Rd", sep=""))
load(paste(statPath, "pps1_anova_pqvalues.Rd", sep=""))

library(PGSEA)

## Load the pathways
source("pathway/pgsea.R")
pathways = readPathways(pathwayPath)


## Test sets
#col = "anova_qvalue_Liver"
#pathways[["all_na"]] = entrezIds[rownames(anovaData[is.na(anovaData[,col]),])][1:50]
#pathways[["half_na"]] = c(entrezIds[rownames(anovaData[is.na(anovaData[,col]),])][1:25], 
#		entrezIds[rownames(anovaData[anovaData[,col] <= 0.001,])][1:25])
#pathways[["all_high"]] = entrezIds[rownames(anovaData[anovaData[,col] <= 0.001,])][1:50]
#pathways[["small_high"]] = entrezIds[rownames(anovaData[anovaData[,col] <= 0.001,])][1:10]


## Perform the PAGE algorithm on each tissue
geneSets = createGeneSetCollection(pathways)

cols = as.character(lapply(levels(tissueFactor), 
	function(tis) { paste("anova_qvalue", tis, sep="_") }))

gseData = 1 - anovaData[, cols] # 1-q as gene score
rownames(gseData) = entrezIds
colnames(gseData) = levels(tissueFactor)

gseResults = doPGSEA(
	geneSets = geneSets, 
	data = gseData, 
	range = c(5, 1000)
)

## Transform everything back to q (instead of q-1)
gseData.q = -(gseData - 1)

## Save the results
writePGSEA(
	pathways = pathways, 
	gseResults = gseResults, 
	gseData = gseData.q, 
	filePrefix = paste(outPath, "page_anova_q", sep="")
)

## Some testing
library(nortest)

pwName = "GPCRs, Class A Rhodopsin-like (Mm_GPCRs,_Class_A_Rhodopsin-like_WP189_21077.txt)"
pw = pathways[[pw]]
pwGenes = pw[pw %in% entrezIds]

tis = "WAT"

s = 500
mmax = 30
pvalues = list()
nt = as.numeric(lapply(1:mmax, function(m) {
	message(m)
	data = as.numeric(lapply(1:s, function(x) {
		mean(sample(gseData[,tis], size = m), na.rm = TRUE)
	}))
	ad.test(data)$p.value
}))
plot(x = 1:mmax, y = nt)

m = mean(as.numeric(lapply(pathways, length)))
s = length(pathways)
data = as.numeric(lapply(1:s, function(x) {
	mean(sample(gseData[,tis], size = m), na.rm = TRUE)
}))
hist(data, breaks = 50)
lillie.test(data)

data = as.numeric(lapply(pathways, function(x) {
	genes = x[x %in% entrezIds]
	mean(gseData[genes,tis], na.rm = TRUE)
}))
hist(data, breaks = 50)
lillie.test(data)

## Test on expression relative to t0
gseData.rel = relData
rownames(gseData.rel) = entrezIds

s = 500
mmax = 30
t = 1
pvalues = list()
nt = as.numeric(lapply(1:mmax, function(m) {
	message(m)
	data = as.numeric(lapply(1:s, function(x) {
		mean(sample(gseData.rel[,paste(tis, "relt0", t, sep="_")], size = m), na.rm = TRUE)
	}))
	ad.test(data)$p.value
}))
plot(x = 1:mmax, y = nt)

## Size bias?
pwsizes = as.numeric(lapply(pathways, length))
gse = gseResults$results[,"Liver"]

plot(x = gse, y = pwsizes)
