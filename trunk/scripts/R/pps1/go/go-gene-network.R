library(org.Mm.eg.db)
library(topGO)

writeSif = function(sifFile, sigGenes, goTerms, maxSize = 350, minConn = 2) {
	# Filter out large terms
	goTerms.s = goTerms
	for(term in goTerms) {
		genes = org.Mm.egGO2ALLEGS[[term]]
		nr = sum(genes %in% sigGenes)
		if(nr > maxSize) {
			message("Leaving out ", Term(GOTERM[[term]], " - too many genes:", nr))
			goTerms.s = goTerms.s[!goTerms.s == term]
		}
	}
	goTerms = goTerms.s

	# Filter out child terms
	goTerms.np = goTerms
	for(term in goTerms) {
		parents = GOBPANCESTOR[[term]]
		if(sum(parents %in% goTerms) > 0) {
			message("Leaving out ", Term(GOTERM[[term]]), " - child of other term in list")
			goTerms.np = goTerms.np[!goTerms.np == term]
		}
	}
	goTerms = goTerms.np

	# Create term-gene mapping matrix
	allGenes = c()
	allMappings = c()
	for(term in goTerms) {
		genes = org.Mm.egGO2ALLEGS[[term]]
		genes = genes[genes %in% sigGenes]
		terms = rep(term, times=length(genes))
		allGenes = append(allGenes, genes)
		allMappings = append(allMappings, terms)
	}
	allMappings = cbind(allMappings, names(allGenes), allGenes)
	
	# Filter out all genes that do not belong to n or more GO terms
	includeGenes = rep(FALSE, times = length(allGenes))
	for(i in 1:length(allGenes)) {
		gene = allGenes[[i]]
		terms = allMappings[allGenes == gene, 1]
		includeGenes[i] = length(unique(terms)) >= minConn
	}
	message(sum(includeGenes), " genes passed criteria of minimal ", minConn, " connections.")
	allMappings = allMappings[includeGenes,]

	# Write the SIF file, defining the network
	write.table(allMappings, file = sifFile, row.names=FALSE, col.names=FALSE, sep="\t", quote=FALSE)
}

writeAttributes = function(filePrefix, genes, terms) {
	message("Looking up go term names")
	goAttr = sapply(terms, function(t) {
		c(t, Term(GOTERM[[t]]), "go")
	})

	attrs = c("id", "label", "type")
	goAttr = t(goAttr)

	message("Looking up gene names")
	geneAttr = sapply(genes, function(g) {
		symbol = org.Mm.egSYMBOL[[g]]
		if(is.null(symbol)) {
			symbol = g
		}
		c(g, symbol, "gene")
	})
	geneAttr = t(geneAttr)

	# Write the files
	for(col in 2:length(attrs)) {
		f = paste(filePrefix, attrs[col], ".txt", sep="")
		cat(paste(attrs[col], "\n", sep=""), file=f)
		write.table(geneAttr[,c(1,col)], file = f, 
			col.names=FALSE, row.names=FALSE, sep=" = ", quote=FALSE, append = TRUE)
		write.table(goAttr[,c(1,col)], file = f, 
			col.names=FALSE, row.names=FALSE, sep=" = ", quote=FALSE, append = TRUE)
	}
}

path = "/home/thomas/projects/pps/path_results/"
resultDir = paste(path, "go-network/", sep="")

onto = "BP"
tissues = c("Muscle", "Liver", "WAT")
maxSize = 350 # Maximal annotated, significant genes
minConn = 2
p.threshold = 0.05
weight.threshold = 0.01

# Load the GO enrichment results
load(paste(path, "topGO/compare/anova/results.Rd", sep=""))

# Load the PPS data
dataFile = "/home/thomas/projects/pps/filtered_data/Combined_total.txt"
data = read.delim(dataFile, row.names=1, na.strings = "NaN")

# Write attribute files for all go terms and genes
writeAttributes(filePrefix = paste(resultDir, "attr_", sep=""), genes = rownames(data), terms = names(as.list(GOBPTerm)))

# Create a network of top go terms for each tissue and one for any of the tissues
goTerms = c()
allGenes = c()

for(tis in tissues) {
	results = results.g[[tis]]$result.all
	
	col = paste("anova_pvalue_", tis, sep="")
	sigGenes = rownames(data)[which(data[,col] <= p.threshold)]
	term = results$GO.ID[as.numeric(results$weight) <= weight.threshold]
	
	message("Network for ", tis, "; ", length(sigGenes), " sig genes; ", length(term), " sig terms")
	
	# Write sif for this tissue only
	writeSif(sifFile = paste(resultDir, "go_network_", tis, ".sif", sep=""), sigGenes = sigGenes, goTerms = term, 
		maxSize = maxSize, minConn = minConn)
	
	# Collect info for all tissues
	allGenes = c(allGenes, sigGenes)
	goTerms = c(goTerms, term)
}
allGenes = unique(allGenes)
goTerms = unique(goTerms)

writeSif(sifFile = paste(resultDir, "go_network_all.sif", sep=""), sigGenes = allGenes, goTerms = goTerms, maxSize = maxSize, minConn = minConn)
