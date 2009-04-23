library(topGO)

#GO analysis on pathway level
#Will return topGO object based on the pathway <-> GO mappings to find which terms are enriched with 
#pathways that have a high z-score
pathwayGO = function(mappingFile, zscoreFile = NULL, onto = "BP", minZ = 2) {
	#Read the GO/Pathway mapping file
	pwMappings = read.delim(mappingFile, as.is=TRUE, header=FALSE)

	pw2go = list()
	for(i in 1:nrow(pwMappings)) {
		go = pw2go[[ pwMappings[i,1] ]]
		pw2go[[ pwMappings[i,1] ]] = c(go, pwMappings[i, 2])
	}

	if(!is.null(zscoreFile)) {
		#Read the z-score file (modified, 2nd column is file name)
		stats = read.delim(zscoreFile, as.is = TRUE, skip = 8)
		pwIds = apply(stats, 1, function(row) { 
			return(paste(row["Pathway"],"(",row["File"],")",sep=""))
		})
		pwZscores = stats[,"Z.Score"]
		names(pwZscores) = pwIds
		allGenes = pwZscores
		geneSel = function(z) { return(z >= minZ) }
	} else {
		pws = unique(pwMappings[,1])
		allGenes = vector("numeric", length = length(pws))
		names(allGenes) = pws
		geneSel = function(z) { return(!is.na(z)) }
	}
	new("topGOdata", 
		ontology = onto,
		allGenes = allGenes,
		geneSel = geneSel,
		annotationFun = annFUN.gene2GO,
		gene2GO = pw2go
	)
}
