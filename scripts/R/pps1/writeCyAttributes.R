dataPath = "/home/thomas/projects/pps1/filtered_data/"
statPath = "/home/thomas/projects/pps1/stat_results/"
zscorePath = "/home/thomas/projects/pps1/path_results/pathvisio-z/"
outPath = "/home/thomas/projects/pps1/stat_results/cytoscape/"

## Load previously calculated data (pps1_perpare_data.R and pps1_anova.R)
load(paste(dataPath, "pps1_log2_combined.Rd", sep=""))
load(paste(statPath, "pps1_anova_pqvalues.Rd", sep=""))

writeAttributes = function(file, name, nodes, values) {
	con = file(file, open="wt")
	writeLines(name, con)
	write.table(file = con, cbind(nodes, values), col.names = F, row.names = F, sep = " = ", quote = F, na = "NaN")
	close(con)
}

#Z-scores
zscoreFiles = c(
	"zscore_Liver_anova_q0.01.txt",
	"zscore_Muscle_anova_q0.01.txt",
	"zscore_WAT_anova_q0.01.txt",
	"zscore_Liver_anova_q0.01_maxfc0.25.txt",
	"zscore_Muscle_anova_q0.01_maxfc0.25.txt",
	"zscore_WAT_anova_q0.01_maxfc0.25.txt"
)
for(zf in zscoreFiles) {
	z = read.delim(paste(zscorePath, zf, sep=""), as.is = TRUE, skip = 8)
	writeAttributes(paste(outPath, "cy_", zf, sep=""), zf, z[,"File"], z[,"Z.Score"])
}

#Relative expression
for(tis in levels(tissueFactor)) {
	for(time in levels(timeFactor)[2:length(levels(timeFactor))]) {
		col = paste(tis, "relt0", time, sep="_")
		writeAttributes(
			paste(outPath, "cy_", col, ".txt", sep=""),
			col,
			entrezIds,
			relData[,col]
		)
	}
}
