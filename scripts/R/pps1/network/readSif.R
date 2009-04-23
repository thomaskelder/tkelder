## Read a sif file as a list, where the names are the left nodes,
## the values are the connected right nodes.
readSif = function(file) {
	sif = list()
	sifTable = read.delim(file, header = FALSE, as.is = TRUE)
	for(i in 1:nrow(sifTable)) {
		if(i %% 500 == 0) {
			message(paste(i, "out of", nrow(sifTable)))
		}
		l = as.character(sifTable[i, 1])
		r = as.character(sifTable[i, 3])
		sif[[l]] = c(sif[[l]], r)
	}
	lapply(sif, unique)
}

toSymbols = function(ids, includeId = TRUE) {
	library(org.Mm.eg.db)
	lapply(ids, function(i) {
		tryCatch(
			if(includeId) {
				paste(as.character(org.Mm.egSYMBOL[i]), " (", i, ")", sep="")
			} else {
				as.character(org.Mm.egSYMBOL[i])
			}, error = function(e) i)
	})
}

invertSif = function(sif) {
	inv = list()
	for(key in names(sif)) {
		for(value in sif[[key]]) {
			inv[[value]] = c(inv[[value]], key)
		}
	}
	lapply(inv, unique)
}

writeSif = function(sif, file) {
	con = file(file, open="wt")
	for(key in names(sif)) {
		for(value in sif[[key]]) {
			writeLines(paste(key, "unknown", value, sep="\t"), con)
		}
	}
	close(con)
}
