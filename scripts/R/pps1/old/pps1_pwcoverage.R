library(plotrix)
library(limma)
library(goTools)
library(GOstats)
library(Rgraphviz)

source("pathway-go.R")
source("top-go.R")
source("go-venn.R")

path = "/home/thomas/projects/pps/path_results/"
outDir = paste(path, "topGO/coverage-kegg/", sep="")
minZ = 2
onto = "BP"
vennTerms = c("GO:0009987", "GO:0008152", "GO:0065007")

#### Coverage control of GO - Pathway mapping
# Print a complete GO graph to 2nd level (previous graph only includes terms that map to pathway)
## TODO3: perform enrichment study with all GO genes as reference and Pathway genes as subset
dataFile = "/home/thomas/projects/pps/filtered_data/Combined_total.txt"
data = read.delim(dataFile, row.names=1, na.strings = "NaN")

goData.g = goData = createGoData(data, col = "anova_pvalue_Liver", pvalue = 1, onto = onto)
goData.pg = createGoDataAnnot(data, col = "anova_pvalue_Liver", pvalue = 1,
		onto = onto, geneFile = "/home/thomas/projects/go-pathway-mapping/mm-kegg-mapping-proc-pruned0.5-xrefsL.txt")
goData.p = pathwayGO("/home/thomas/projects/go-pathway-mapping/mm-kegg-mapping-proc-pruned0.5.txt", onto = onto)

# TOP
leaves = CustomEndNodeList("GO:0008150",rank=1)

# Plus 3 highest coverage
#leaves = append(leaves, CustomEndNodeList("GO:0008152"))
#leaves = append(leaves, CustomEndNodeList("GO:0009987"))

# Cellular process + first children
#leaves = CustomEndNodeList("GO:0009987",rank=1)

graph = GOGraph(unique(leaves), GOBPPARENTS)
ns = nodes(graph)

##Gather gene counts
go.n = vector("character", length=length(ns))
pg.n = vector("character", length=length(ns))
p.n = vector("character", length=length(ns))
for(i in 1:length(ns)) {
	go.g = genesInTerm(goData.g, whichGO = ns[i])
	go.pg = genesInTerm(goData.pg, whichGO = ns[i])
	go.p = genesInTerm(goData.p, whichGO = ns[i])
	
	go.n[i] = ifelse(length(go.g) > 0, length(go.g[[1]]), 0)
	pg.n[i] = ifelse(length(go.pg) > 0, length(go.pg[[1]]), 0)
	p.n[i] = ifelse(length(go.p) > 0, length(go.p[[1]]), 0)
}

names = vector("character", length=length(ns))
for(i in 1:length(names)) {
	l = genesInTerm(goData.p, whichGO = ns[i])
	if(length(l) == 0) { 
		l = 0
	} else {
		l = length(l[[1]]) 
	}
	pct = ifelse(pg.n[i] == "0", 0, as.numeric(pg.n[i]) / as.numeric(go.n[i])) * 100
	names[[i]] = paste(substr(Term(GOTERM[[ns[i]]]), 1, 10), "\\\n", p.n[i], "\\\n", pg.n[i], "/", go.n[i], sep="")
}
natt = makeNodeAttrs(graph, shape = "box", label = names, fontsize = 15)

colors = colorRamp(c("white", "blue"), space="rgb")
for(i in 1:length(ns)) {
	npg = as.numeric(pg.n[i])
	if(npg > 0) {
		clr = colors(npg/ as.numeric(go.n[i]))
	} else {
		clr = c(255, 255, 255)
	}
	natt$fillcolor[[ns[i]]] = rgb(clr[1], clr[2], clr[3], maxColorValue = 255)
}
#fixInNamespace(drawTxtLabel, "Rgraphviz") #add cex = 2 to text() call

lgraph = agopen(graph, layoutType = "dot", nodeAttrs = natt, name = "")
png(file = paste(outDir, "pathway-go-coverage-cellproc.png", sep=""), width = 3000, height = 2000)
plot(lgraph)
dev.off()

png(file = paste(outDir, "venn-p-go.png", sep=""), width = 1200, height = 1000)
createVenn(goData.p, terms = c("GO:0009987", "GO:0008152", "GO:0065007"), main = "GO to pathway mappings", sig = FALSE)
dev.off()

png(file = paste(outDir, "venn-pg-go.png", sep=""), width = 1200, height = 1000)
createVenn(goData.pg, terms = c("GO:0009987", "GO:0008152", "GO:0065007"), main = "GO to pathway mappings", sig = FALSE)
dev.off()

png(file = paste(outDir, "venn-g-go.png", sep=""), width = 1200, height = 1000)
createVenn(goData.g, terms = c("GO:0009987", "GO:0008152", "GO:0065007"), main = "GO to pathway mappings", sig = FALSE)
dev.off()
