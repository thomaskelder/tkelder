library(org.Mm.eg.db)
library(topGO)

path = "/home/thomas/projects/pps/path_results/"
resultDir = paste(path, "topGO/compare/anova/", sep="")

onto = "BP"
tissues = c("Muscle", "Liver", "WAT")

# Load the GO enrichment results
load(paste(resultDir, "results.Rd", sep=""))

# Load the PPS data
dataFile = "/home/thomas/projects/pps/filtered_data/Combined_total.txt"
data = read.delim(dataFile, row.names=1, na.strings = "NaN")

# Load the z-scores
zscores = list()
for(tis in tissues) {
	zscoreFile = paste(path, "zscore_", tis, "_anov0.05.txt", sep = "")
	zscores[[tis]] = read.delim(zscoreFile, as.is = TRUE, skip = 8)
}

# GOData object for getting pathway - go annotations
goData = results.p[["Muscle"]]$goData

# Create a network of top go terms from any of the tissues
allTerms = c()
p.threshold = 0.05
for(tis in tissues) {
	results = results.p[[tis]]$result.all
	
	col = paste("anova_pvalue_", tis, sep="")
	term = results$GO.ID[1:10]
	allTerms = c(allTerms, term)
}
allTerms = unique(allTerms)

# Filter out child terms
allTerms.np = allTerms
for(term in allTerms) {
	parents = GOBPANCESTOR[[term]]
	if(sum(parents %in% allTerms) > 0) {
		print(paste("Leaving out", Term(GOTERM[[term]]), " - child of other term in list"))
		print(parents)
		allTerms.np = allTerms.np[!allTerms.np == term]
	}
}
allTerms = allTerms.np

# Create term-pathway mapping matrix
allPws = c()
allMappings = c()
for(term in allTerms) {
	pathways = genesInTerm(goData, whichGO = term)[[term]]
	terms = rep(term, times=length(pathways))
	allPws = append(allPws, as.character(pathways));
	allMappings = append(allMappings, terms);
}
allMappings = cbind(allMappings, "pathway2go", allPws)

# Filter out all pathways that do not belong to n or more GO terms
n = 2
includePws = rep(FALSE, times = length(allPws))
for(i in 1:length(allPws)) {
	pathway = allPws[[i]]
	terms = allMappings[allPws == pathway, 1]
	includePws[i] = length(unique(terms)) >= n
}
allMappings = allMappings[includePws,]

# Write the SIF file, defining the network
write.table(allMappings, file = paste(resultDir, "go-pathway-network.sif", sep=""), row.names=FALSE, col.names=FALSE, sep="\t", quote=FALSE)

# Write an attribute file for the GO terms
goAttr = sapply(allTerms, function(t) {
	c(t, Term(GOTERM[[t]]), 1)
})

goAttr = t(goAttr)
colnames(goAttr) = c("id", "label", "is_go")
for(col in 2:length(colnames(goAttr))) {
	write.table(goAttr[,c(1,col)], file = paste(resultDir, "go-attributes-", colnames(goAttr)[col], ".txt", sep=""), row.names=FALSE, sep=" = ", quote=FALSE)
}

# Write an attribute file for the pathways
zscoresById = array("NaN", dim = c(nrow(zscores$Liver), length(tissues)))
rownames(zscoresById) = apply(zscores$Liver, 1, function(row) {
	paste(row["Pathway"],"(",row["File"],")",sep="")
})
for(i in 1:length(tissues)) {
	for(j in 1:nrow(zscores[[ tissues[i] ]])) {
		row = zscores[[ tissues[i] ]][j,]
		pwid = paste(row["Pathway"],"(",row["File"],")",sep="")
		if(!is.na(row$Z.Score)) {
			zscoresById[pwid, i] = row$Z.Score
		}
	}
}
colnames(zscoresById) = c("zscore_liver", "zscore_wat", "zscore_muscle")
for(col in 1:length(colnames(zscoresById))) {
	write.table(zscoresById[,col], file = paste(resultDir, "pathway-attributes-", colnames(zscoresById)[col], ".txt", sep=""), row.names=TRUE, col.names = colnames(zscoresById)[col], sep=" = ", quote=FALSE)
}
write.table(allPws, file = paste(resultDir, "pathway-attributes-label.txt", sep=""), row.names=allPws, col.names = c("id = label"), sep=" = ", quote=FALSE)
	
	
	
	
	
	
	
