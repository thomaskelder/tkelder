##########################################################
## Installs R-packages that are required to run the     ##
## pathway interaction analysis on the pps2 data.       ##
##########################################################
pkg = c(
	"igraph",
	"hash",
	"ggplot2",
	"gplots",
	"foreach",
	"doMC"
)

install.packages(pkg)

source("http://bioconductor.org/biocLite.R")
biocLite(c("limma", "org.Mm.eg.db"))
