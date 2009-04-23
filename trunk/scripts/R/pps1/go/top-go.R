# Functions to run topGO for the NuGO PPS1 data (rows are entrez gene ids)
library(topGO)
library(org.Mm.eg.db)
org.Mm.egGO2PROBE = org.Mm.egGO2EG #Hack to make entrez gene annotation easier

## Create a GoData object
# data is a vector containing p-values, with entrez gene ids as names
# sig is the significance cutoff to use (data <= sig will be significant)
createGoData = function(data, sig = 0.01, onto = "BP") {
	data[is.na(data)] = sig + 1 #Make sure NA's don't end up in the siggenes list
	
	goData = new("topGOdata", 
		ontology = onto, 
		allGenes = data, 
		geneSel = function(scores) { scores <= sig },
		annotationFun = annFUN.db,
		affyLib = "org.Mm.eg.db"
	)
	goData
}

## Perform the go tests, based on a GoData object
goTests = function(goData, alg = "Count", test = GOFisherTest, onto = "BP", testName = "") {
	test.clas <- new(paste("classic", alg, sep=""), testStatistic = test,
		name = paste(testName, " - classic"))
	result.clas <- getSigGroups(goData, test.clas)

	test.elim <- new(paste("elim", alg, sep=""), testStatistic = test,
		name = paste(testName, " - elim"), cutOff = 0.01)
	result.elim <- getSigGroups(goData, test.elim)

	if(alg == "Score") { # No weight algorithm for score
		result.weight = NULL
		
		sumres <- GenTable(goData, classic = result.clas, elim = result.elim,
			orderBy = "elim", 
			topNodes = 100, ranksOf = "classic")		
	} else {
		test.weight <- new(paste("weight", alg, sep=""), testStatistic = test,
			name = paste(testName, " - weight"), sigRatio = "ratio")
		
		result.weight <- getSigGroups(goData, test.weight)
		
		sumres <- GenTable(goData, classic = result.clas, elim = result.elim,
			weight = result.weight, orderBy = "elim", 
			topNodes = 100, ranksOf = "classic")
	}

	
	return(list(
		goData = goData, result.summary = sumres, 
		result.clas = result.clas, result.elim = result.elim, 
		result.weight = result.weight
	))
}
