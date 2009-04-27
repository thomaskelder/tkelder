source("merge_zscores.R")
library(limma)

inPath = "/home/thomas/projects/pps1/path_results/pathvisio-z/"
outPath = inPath

zfiles = c(
	"zscore_Liver_anova_q0.01.txt",
	"zscore_Muscle_anova_q0.01.txt",
	"zscore_WAT_anova_q0.01.txt"
)

zscores = mergeZscores(files = zfiles, path = inPath)
colnames(zscores) = c("Liver", "Muscle", "WAT")

#plot(zscores[,"WAT"], zscores[,"Liver"])

png(file = paste(outPath, "venn_z2_q0.01.png", sep=""),
		pointsize = 30, width = 1500, height = 1500)
	
vennDiagram(zscores >= 2, main = "z >= 2 (anova q<=0.01)")

dev.off()

top = zscores
ntop = 20
for(col in colnames(top)) {
	o = order(zscores[,col], decreasing = T)
	top[,col] = 0
	top[o[1:ntop], col] = 1
}

png(file = paste(outPath, "venn_top20_q0.01.png", sep=""),
		pointsize = 30, width = 1500, height = 1500)
vennDiagram(top, main = paste("top", ntop, "(anova q<=0.01)"))

dev.off()
