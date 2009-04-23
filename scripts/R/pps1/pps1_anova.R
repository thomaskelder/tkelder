## Perform anova and calculate q-values for the PPS1 data

library(multtest)
library(limma)
library(qvalue)

anovaPvalues<-function(x,factor){
	ttest <- function(x){
		m<-data.frame(factor, x);
		anova(aov(x ~ factor, m));
	};
	anova.res<-apply(x, 1, ttest);
	as.numeric(lapply(anova.res, function(x){x["Pr(>F)"][1,]}));
};

inPath = "/home/thomas/projects/pps1/filtered_data/"
outPath = "/home/thomas/projects/pps1/stat_results/"

## Load the variables calculated in pps1_prepare_data.R
load(paste(inPath, "pps1_log2_combined.Rd", sep=""))
tissues = levels(tissueFactor)

qvalues = list()
pvalues = list()

for(tis in tissues) {
	tisData = totalData[, tissueFactor == tis]
	#Find subset that excludes rows with only NAs
	subset = apply(tisData, 1, function(row) {
		sum(is.na(row)) != length(row)
	})

	#Perform ANOVA
	p = anovaPvalues(tisData[subset,], timeFactor[tissueFactor == tis])
	q = qvalue(p)$qvalue
	names(p) = row.names(tisData)[subset]
	names(q) = row.names(tisData)[subset]
	pvalues[[tis]] = p
	qvalues[[tis]] = q
}

# Put p and q values in a matrix with all tissues and probesets
anovaNames = vector(mode = "character", length = 2 * length(tissues))
for(i in 1:length(tissues)) {
	anovaNames[2*i - 1] = paste("anova", "pvalue", tissues[[i]], sep="_")
	anovaNames[2*i] = paste("anova", "qvalue", tissues[[i]], sep="_")
}
anovaData = array(NA, 
		dim = c(nrow(totalData), length(anovaNames)),
		dimnames = list(rownames(totalData), anovaNames)
)
for(tis in tissues) {
	pcol = paste("anova", "pvalue", tis, sep="_")
	qcol = paste("anova", "qvalue", tis, sep="_")
	anovaData[names(pvalues[[tis]]), pcol] = pvalues[[tis]]
	anovaData[names(qvalues[[tis]]), qcol] = qvalues[[tis]]
}

# Save the results
save(anovaData, qvalues, pvalues, file = paste(outPath, "pps1_anova_pqvalues.Rd", sep=""))

# Write a text file for PathVisio
txtData = cbind(entrezIds, avgData, relData, anovaData)
write.table(txtData, file = paste(outPath, "pps1_expr_anova.txt", sep=""),
	row.names = FALSE, sep = "\t", quote = FALSE, na = "NaN")
