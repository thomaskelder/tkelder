source("go/top-go.R")
source("go/go-venn.R")

dataPath = "/home/thomas/projects/pps1/filtered_data/"
statPath = "/home/thomas/projects/pps1/stat_results/"
outPath = "/home/thomas/projects/pps1/path_results/go/"

## Load previously calculated data (pps1_perpare_data.R and pps1_anova.R)
load(paste(dataPath, "pps1_log2_combined.Rd", sep=""))
load(paste(statPath, "pps1_anova_pqvalues.Rd", sep=""))

sigq = 0.01
minZ = 2
onto = "BP"
tissues = levels(tissueFactor)
vennTerms = c("GO:0006954", "GO:0008152", "GO:0006979")

##### Classic GO enrichment using topGO, different gene set statistics
goData = list()
goResults.fisher = list()
goResults.ks = list()
goResults.t = list()

for(tis in tissues) {
	print(paste("Processing", tis))
	data = anovaData[,paste("anova_qvalue", tis, sep="_")]
	names(data) = entrezIds
	gd = createGoData(data, sig = sigq, onto = onto)
	goData[[tis]] = gd
	
	goResults.ks[[tis]] = goTests(
		gd, onto = onto, alg = "Score", test = GOKSTest, testName = "ks"
	)
	goResults.fisher[[tis]] = goTests(
		gd, onto = onto, alg = "Count", test = GOFisherTest, testName = "fisher"
	)
}

for(tis in tissues) {
	write.table(goResults.ks[[tis]]$result.summary, 
		file=paste(outPath, "topterms_ks_", tis, ".txt", sep=""), 
		row.names=FALSE, quote=FALSE, sep="\t"
	)
	printGraph(goData[[tis]], goResults.ks[[tis]]$result.elim, firstSigNodes = 10,
		goResults.ks[[tis]]$result.clas, 
		fn.prefix = paste(outPath, "ks_", tis, sep=""), useInfo = "all"
	)
	
	write.table(goResults.fisher[[tis]]$result.summary, 
		file=paste(outPath, "topterms_fisher_", tis, ".txt", sep=""), 
		row.names=FALSE, quote=FALSE, sep="\t"
	)
	printGraph(goData[[tis]], goResults.fisher[[tis]]$result.weight, firstSigNodes = 10,
		goResults.fisher[[tis]]$result.clas, 
		fn.prefix = paste(outPath, "fisher_", tis, sep=""), useInfo = "all"
	)
}

save(goData, goResults.fisher, goResults.ks,
	file=paste(outPath, "pps1_go_results.Rd", sep=""))

for(tis in tissues) {
	png(file = paste(outPath, "venn_go_all_", tis, ".png", sep=""), 
		width = 1200, height = 1000)
	createVenn(goData[[tis]], terms = vennTerms, 
		main = "GO to pathway mappings", sig = FALSE)
	dev.off()
	
	png(file = paste(outPath, "venn_go_sig_q", sigq, "_", tis, ".png", sep=""), 
		width = 1200, height = 1000)
	createVenn(goData[[tis]], terms = vennTerms, 
		main = paste("GO to pathway mappings - q <=", sigq), sig = TRUE)
	dev.off()
}
