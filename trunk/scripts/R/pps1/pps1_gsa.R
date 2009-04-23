dataPath = "/home/thomas/projects/pps1/filtered_data/"
statPath = "/home/thomas/projects/pps1/stat_results/"
pathwayPath = "/home/thomas/data/pathways/pps1-entrez/"
outPath = "/home/thomas/projects/pps1/path_results/pathway/"

## Load previously calculated data (pps1_perpare_data.R and pps1_anova.R)
load(paste(dataPath, "pps1_log2_combined.Rd", sep=""))
load(paste(statPath, "pps1_anova_pqvalues.Rd", sep=""))

library(GSA)

gsaData = totalData
rownames(gsaData) = entrezIds

source("pathway/pgsea.R")
pathways = readPathways(pathwayPath)

gsaResults = list()

tis = "WAT"
tisCols = tissueFactor == tis

#Find subset of rows with only nas
all.na = apply(gsaData[, tisCols], 1, function(row) {
	sum(is.na(row)) >= length(row)
})
	
gsaResults[[tis]] = GSA(
	x = gsaData[!all.na, tisCols], 
	y = as.numeric(timeFactor[tisCols]),
	genesets = pathways,
	genenames = entrezIds[!all.na],
	method = "maxmean",
	resp.type = "Quantitative",
	minsize = 5,
	maxsize = 1000,
)
GSA.listsets(gsaResults[[tis]], geneset.names=names(pathways),FDRcut=1, maxchar = 30)

f = timeFactor[tisCols]
y = vector("numeric", length = length(f))
for(i in 1:length(levels(f))) {
	y[f == levels(f)[i]] = i
}
gsaResults.mc = list()
gsaResults.mc[[tis]] = GSA(
	x = gsaData[!all.na, tisCols], 
	y = as.numeric(timeFactor[tisCols]),
	genesets = pathways,
	genenames = entrezIds[!all.na],
	method = "maxmean",
	resp.type = "Multiclass",
	minsize = 5,
	maxsize = 1000,
)
GSA.listsets(gsaResults.mc[[tis]], geneset.names=names(pathways),FDRcut=1, maxchar = 30)

