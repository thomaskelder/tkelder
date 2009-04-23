library(PGSEA)
library(GSEABase)

source("network/readSif.R")

## Reads in pathways exported by the java script pps.pathwayexport.TranslateXrefExporter
readPathways = function(path) {
	pathways = list()
	
	for(file in dir(path)) {
		message("reading ", file, "...")
		p = readLines(paste(path, file, sep=""))
		name = paste(substring(p[1], 2), " (", file, ")", sep="")
		pathways[[name]] = p[2:length(p)]
	}
	pathways
}

## Create geneset collection based on output from readPathways
createGeneSetCollection = function(pathways) {
	geneSets = list()
	for(name in names(pathways)) {
		geneSets[[name]] = GeneSet(pathways[[name]], geneIdType=EntrezIdentifier(), 
			setName = name)
	}
	GeneSetCollection(geneSets)
}

## Perform pgsea on all tissues
doPGSEA = function(geneSets, data, range = c(5, 1000)) {
	cols = colnames(data)
	gseResults = lapply(cols, function(tis) {
		PGSEA(as.matrix(data[,tis]), geneSets,
			range = range,
			ref = NULL,
			center = FALSE,
			enforceRange = TRUE,
			p.value = TRUE
		)
	})
	names(gseResults) = cols
	gseResults
}

## Write pgsea results
writePGSEA = function(pathways, gseResults, gseData, filePrefix) {
	delta = sd(gseData, na.rm = TRUE)
	pwsizes = as.numeric(lapply(pathways, length))

	for(tis in colnames(gseData)) {
		mu = mean(gseData[,tis], na.rm = TRUE)
		pwmean = as.numeric(lapply(pathways, function(genes) {
			dgenes = genes[genes %in% rownames(gseData)]
			mean(gseData[dgenes, tis], na.rm = TRUE)
		}))
		pwmsizes = as.numeric(lapply(pathways, function(genes) {
			not.na = rownames(gseData)[!is.na(gseData[,tis])]
			sum(genes %in% not.na)
		}))	
	
		o = order(gseResults[[tis]]$results, decreasing = TRUE)
		results = cbind(
			rownames(gseResults[[tis]]$results)[o],
			round(gseResults[[tis]]$results[o], 3),
			pwsizes[o],
			pwmsizes[o],
			round(pwmean[o], 3)
		)
		colnames(results) = c("pathway", "z-score", "size", "measured", "mean")
	
		con = file(paste(filePrefix, "_", tis, ".txt", sep=""), open="wt")
		writeLines(paste("#", tis, sep=""), con)
		writeLines(paste("#dataset mean:", mu), con)
		writeLines(paste("#dataset sd:", delta[tis]), con)
		write.table(results, 
			file=con, 
			row.names=FALSE, quote=FALSE, sep="\t"
		)
		close(con)
	}
}


