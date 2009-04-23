source("merge_zscores.R")
library(limma)

inPath = "/home/thomas/projects/pps1/path_results/pathvisio-z/"

zfiles = c(
	"zscore_WAT_anova_q0.005.txt",
	"zscore_WAT_anova_q0.01.txt",
	"zscore_WAT_anova_q0.05.txt"
)

zscores = mergeZscores(files = zfiles, path = inPath)
colnames(zscores) = c("q0.005", "q0.01", "q0.05")

plot(zscores[,"q0.005"], zscores[,"q0.05"])
vennDiagram(zscores >= 2)
