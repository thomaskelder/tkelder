# Runs topGO for the NuGO PPS1 data, clustered with Oriogen
library(topGO)
library(org.Mm.eg.db)
org.Mm.egGO2PROBE = org.Mm.egGO2EG #Hack to make entrez gene annotation easier

createGoData = function(data, col, cluster = FALSE, pvalue = 0.05, onto = "BP") {
	
	totalGenes = data[,col]
	totalGenes[is.na(totalGenes)] = FALSE #Filter out NA's
	names(totalGenes) = rownames(data)
		if(cluster) {
		geneSel = function(scores) { scores %in% cluster }
	} else {
		geneSel = function(scores) { scores < pvalue }
	}
	goData = new("topGOdata", 
		ontology = onto, 
		allGenes = totalGenes, 
		geneSel = geneSel, 
		annotationFun = annFUN.db,
		affyLib = "org.Mm.eg.db"
	)
	goData
}

createGoDataAnnot = function(data, col, cluster = FALSE, pvalue = 0.05, onto = "BP", geneFile) {
	mappings = read.delim(geneFile, as.is=TRUE, header=FALSE)
	mappings[,1] = as.character(mappings[,1])
	gene2go = list()
	for(i in 1:nrow(mappings)) {
		go = gene2go[[ mappings[i,1] ]]
		gene2go[[ mappings[i,1] ]] = c(go, mappings[i, 2])
	}
	
	totalGenes = data[,col]
	totalGenes[is.na(totalGenes)] = FALSE #Filter out NA's
	names(totalGenes) = rownames(data)
	if(cluster) {
		geneSel = function(scores) { scores %in% cluster }
	} else {
		geneSel = function(scores) { scores < pvalue }
	}
	goData = new("topGOdata", 
		ontology = onto, 
		allGenes = totalGenes, 
		geneSel = geneSel, 
		annotationFun = annFUN.gene2GO,
		gene2GO = gene2go
	)
	goData
}

goTests = function(goData, onto = "BP") {
	test.clas <- new("classicCount", testStatistic = GOFisherTest,
	name = "Fisher test - classic")
	result.clas <- getSigGroups(goData, test.clas)

	test.elim <- new("elimCount", testStatistic = GOFisherTest,
		name = "Fisher test - elim", cutOff = 0.01)
	result.elim <- getSigGroups(goData, test.elim)

	test.weight <- new("weightCount", testStatistic = GOFisherTest,
		name = "Fisher test - weight", sigRatio = "ratio")
	result.weight <- getSigGroups(goData, test.weight)

	allRes <- GenTable(goData, classic = result.clas, elim = result.elim, weight = result.weight, orderBy = "weight",
		ranksOf = "classic",  topNodes = 50)

	res = list(goData = goData, result.all = allRes, result.clas = result.clas, result.elim = result.elim, result.weight = result.weight)
	res
}
