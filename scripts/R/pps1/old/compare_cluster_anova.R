library(limma)

dataFile = "/home/thomas/projects/pps/filtered_data/Combined_total.txt"
data = read.delim(dataFile, row.names=1, na.strings = "NaN")


tissues = c("WAT", "Muscle", "Liver")

for(tis in tissues) {
	col.cluster = paste(tis, "_oriogen_profile", sep="")
	data[is.na(data[[col.cluster]]), col.cluster] = -1
	in.cluster = data[[col.cluster]] > -1

	col.anova = paste("anova_pvalue_", tis, sep="")
	data[is.na(data[[col.anova]]), col.anova] = 1
	in.anova = data[[col.anova]] < 0.01

	counts = matrix(c(in.cluster, in.anova), nrow = length(in.anova), ncol = 2)
	colnames(counts) = c("cluster", "anova")
	dev.new()
	vennDiagram(vennCounts(counts), main = paste(tis, ", p < 0.01", sep=""))
}
