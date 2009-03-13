library(plotrix)
library(limma)
library(goTools)
library(GOstats)

source("pathway-go.R")
source("top-go.R")
source("go-venn.R")

path = "/home/thomas/projects/pps/path_results/"
outDir = paste(path, "topGO/compare/anova/", sep="")
minZ = 2
onto = "BP"
tissues = c("Muscle", "Liver", "WAT")
vennTerms = c("GO:0009987", "GO:0008152", "GO:0065007")

##### Combine all zscore files (for MeV visualization)
source("mergeZscores.R")
files = c(
	"zscore_Liver_anov0.05.txt",
	"zscore_Muscle_anov0.05.txt",
	"zscore_WAT_anov0.05.txt"
)
total = mergeZscores(files, path)
write(total, "combined_zscores_anov0.05.txt", path)
write(digitalize(total), "combined_zscores_anov0.05_z2.txt", path)

##### GO Enrichment based on GO2Pathway mappings and pathway z-scores
#Input 1: A file that maps pathways to GO categories (output from the java gpml-go-mapper)
mappingFile = "/home/thomas/projects/go-pathway-mapping/mm-mapping-proc-pruned0.5.txt"

## Enrichment with lists of z-scores
#Input 2: A list of z-scores for pathways (output from PathVisio)
results.p = list()

for(tis in tissues) {
	print(paste("Processing", tis))
	zscoreFile = paste(path, "zscore_", tis, "_anov0.05.txt", sep = "")
	goData = pathwayGO(mappingFile, zscoreFile, onto, minZ)
	goRes = goTests(goData, onto = onto)
	results.p[[tis]] = goRes
	
	prefix = paste(outDir, tis, "_anova0.05", sep="")
	printGraph(goData, goRes$result.weight, firstSigNodes = 10, goRes$result.clas, fn.prefix = prefix, useInfo = "all")
	printGraph(goData, goRes$result.clas, firstSigNodes = 10, goRes$result.clas, fn.prefix = prefix, useInfo = "all")
	printGraph(goData, goRes$result.elim, firstSigNodes = 10, goRes$result.clas, fn.prefix = prefix, useInfo = "all")
}

## Overlap stuff on pathway z-score
createVenn(results.p[[tis]]$goData, terms = vennTerms)

##### Classic GO enrichment based on genes in pathway-GO mappings only
dataFile = "/home/thomas/projects/pps/filtered_data/Combined_total.txt"
data = read.delim(dataFile, row.names=1, na.strings = "NaN")

results.pg = list()

for(tis in tissues) {
	print(paste("Processing", tis))
	col = paste("anova_pvalue_", tis, sep="");
	goData = createGoDataAnnot(
		data, col = col, pvalue = 0.05,
		onto = onto, geneFile = "/home/thomas/projects/go-pathway-mapping/mm-mapping-proc-pruned0.5-xrefsL.txt")
	goRes = goTests(goData, onto = onto)
	results.pg[[tis]] = goRes
}

createIntersect(results.pg[[tis]]$goData, vennTerms, TRUE)
dev.new()
createVenn(results.pg[[tis]]$goData, terms = vennTerms)
dev.new()
createVenn(results.pg[[tis]]$goData, terms = vennTerms, sig = FALSE)

##### Classic GO enrichment
results.g = list()

for(tis in tissues) {
	print(paste("Processing", tis))
	col = paste("anova_pvalue_", tis, sep="")
	goData = createGoData(data, col = col, pvalue = 0.05, onto = onto)
	goRes = goTests(goData, onto = onto)
	results.g[[tis]] = goRes
}

tis = "Muscle"
createIntersect(results.g[[tis]]$goData, vennTerms, TRUE)
dev.new()
createVenn(results.g[[tis]]$goData, terms = vennTerms)
dev.new()
createVenn(results.g[[tis]]$goData, terms = vennTerms, sig = FALSE)

##### Comparison of 3 GO approaches
for(tis in tissues) {
	col = paste("anova_pvalue_", tis, sep="")
	zscoreFile = paste(outDir, "zscore_", tis, "_anova.txt", sep = "")

	goData.p = results.p[[tis]]$goData
	goData.pg = results.pg[[tis]]$goData
	goData.g = results.g[[tis]]$goData

	results = list(results.p = results.p[[tis]], results.pg = results.pg[[tis]], results.g = results.g[[tis]])

	for(rn in names(results)) {
		write.table(results[[rn]]$result.all, file=paste(outDir, "topterms_", rn, "_", tis, ".txt", sep = ""), row.names=FALSE, quote = FALSE, sep = "\t")
		printGraph(results[[rn]]$goData, results[[rn]]$result.weight, firstSigNodes = 10, 
			results[[rn]]$result.clas, fn.prefix = paste(outDir, rn, tis, sep=""), useInfo = "all")
	}

	png(file = paste(outDir, "venn-pathway-p-anova.png", sep=""), width = 1200, height = 1000)
	createVenn(goData.p, terms = c("GO:0009987", "GO:0008152", "GO:0065007"), main = "GO to pathway mappings", sig = TRUE)
	dev.off()

	png(file = paste(outDir, "venn-pathway-pg.png", sep=""), width = 1200, height = 1000)
	createVenn(goData.pg, terms = c("GO:0009987", "GO:0008152", "GO:0065007"), main = "GO to gene mappings (genes based on pathways)", sig = TRUE)
	dev.off()

	png(file = paste(outDir, "venn-pathway-g.png", sep=""), width = 1200, height = 1000)
	createVenn(goData.g, terms = c("GO:0009987", "GO:0008152", "GO:0065007"), main = "GO to gene mappings", sig = TRUE)
	dev.off()
}
