library(qvalue)

exprPath = "/home/thomas/projects/pps2/stat_results/"
inPath = "/home/thomas/projects/pps2/stat_results/bigcat/correlation/"
outPath = inPath

diets = c("LF", "HF")

# Read insulin/glucose/HOMA data
metData = list();
for(diet in diets) {
	metData[[diet]] = read.delim(
		paste(inPath, diet, "_t12.txt", sep=""), as.is = TRUE, 
		row.names = 1, check.names = FALSE
	)
}

# Read in gene expression data
exprData = read.delim(
	paste(exprPath, "PPS2_filteredlist_2log.txt", sep=""), 
	as.is = TRUE, row.names = 1
)
# Remove first two columns
geneNames = exprData[,2]
names(geneNames) = rownames(exprData)
geneDescr = exprData[,1]
exprData = exprData[,3:ncol(exprData)]

#Format of exprData headers: DIET_TIME_ANIMAL ("HF_t0_256")
#Format of metData headers: ANIMAL ("256")
exprAnimals = gsub("[HLF]{2}_t.+_", "", colnames(exprData))
exprTimes = gsub("[HLF]{2}_", "", colnames(exprData))
exprTimes = gsub("_[[:digit:]]{3}", "", exprTimes)
exprColors = exprTimes
legendNames = c("t0", "t0.6", "t2" ,"t18" ,"t48")
legendColors = c("red", "green", "blue", "cyan", "black")
for(i in 1:length(legendNames)) {
	exprColors[exprTimes == legendNames[i]] = legendColors[i]
}

# Calculate correlation for each gene with insulin, glucose and HOMA
tExpr = c("t0") #Only take expression values from t0 (before glucose challenge)

corrData.p = list()
corrData.q = list()
for(diet in diets) {
	metData.diet = metData[[diet]]
	corr.p = array(NA, dim = c(nrow(exprData), nrow(metData.diet)))
	colnames(corr.p) = rownames(metData.diet)
	rownames(corr.p) = rownames(exprData)
	
	corr.q = corr.p
	
	exprCols = exprAnimals %in% colnames(metData.diet)
	exprCols = exprCols & (exprTimes %in% tExpr)
	metCols = colnames(metData.diet) %in% exprAnimals[exprCols]
	
	for(met in rownames(metData.diet)) {
		pvalues = apply(exprData, 1, function(expr) {
			x = as.numeric(metData.diet[met,metCols])
			y = as.numeric(expr[exprCols])
			cor.test(x, y, method="pearson", alternative = "two.sided")$p.value
		})
		corr.p[,met] = as.numeric(pvalues)
		corr.q[,met] = qvalue(pvalues)$qvalue
	}
	corrData.p[[diet]] = corr.p
	corrData.q[[diet]] = corr.q
}

# Control plots for top significant correlations
p.threshold = 0.001

for(diet in diets) {
	metData.diet = metData[[diet]]
	exprCols = exprAnimals %in% colnames(metData.diet)
	exprCols = exprCols & (exprTimes %in% tExpr)
	metCols = colnames(metData.diet) %in% exprAnimals[exprCols]
	for(met in rownames(metData.diet)) {
		# Get the top q-values
		p = corrData.p[[diet]][,met]
		topgenes = names(p[p <= p.threshold])
		# Plot against metabolite
		for(gene in topgenes) {
			png(file = paste(outPath, "plots/", paste("plot", diet, met, round(p[gene], 3), gene, sep="_"), ".png", sep=""), width = 500, height = 500)
			x = as.numeric(metData.diet[met,metCols])
			names(x) = colnames(metData.diet[,metCols])
			y = as.numeric(exprData[gene, exprCols])
			# Color by timepoint
			colors = exprColors[exprCols]
			plot(x, y, xlab = met, ylab = "log2 intensity", main = paste(geneNames[gene], "p = ", round(p[gene], 3)), col = colors)
			text(x, y, labels=colnames(metData.diet)[metCols], offset = 0.5, pos = 3)
			legend("topright", legend = legendNames, fill = legendColors)
			dev.off()
		}
	}
}

txtData = cbind(rownames(exprData), geneNames, geneDescr, corrData.p[["LF"]], corrData.p[["HF"]])
colnames(txtData) = c("geneID", "geneName", "geneDescr", 
	paste("HF", colnames(corrData.p[["HF"]]), sep="_"),
	paste("LF", colnames(corrData.p[["LF"]]), sep="_")
)
write.table(txtData, file = paste(outPath, "corr_", tExpr, "_p_t12.txt", sep=""),
	row.names = FALSE, sep = "\t", quote = FALSE, na = "NaN")
