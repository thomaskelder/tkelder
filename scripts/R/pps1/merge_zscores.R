mergeZscores = function(files, path = "") {
	stats = list()

	for(i in 1:length(files)) {
		#Read the z-score file (modified, 2nd column is file name)
		stats[[i]] = read.delim(paste(path, files[i], sep=""), as.is = TRUE, skip = 8)
		pwIds = apply(stats[[i]], 1, function(row) { 
			return(paste(row["Pathway"],"(",row["File"],")"))
		})
		rownames(stats[[i]]) = pwIds
	}

	#Gather vector of unique pathways
	pathways = c()
	for(s in stats) {
		pathways = c(pathways, rownames(s))
	}
	pathways = unique(pathways)

	#Paste the z-scores together
	total = array(dim = c(length(pathways), length(stats)), dimnames = list(pathways, files))
	for(i in 1:length(pathways)) {
		p = pathways[i]
		for(j in 1:length(stats)) {
			s = stats[[j]]
			total[i, j] = s[p, "Z.Score"]
		}
	}
	return(total)
}

write = function(total, file, path) {
	write.table(total, file=paste(path, file, sep=""), quote=FALSE, sep="\t", col.names=NA)
}

digitalize = function(total, threshold = 2) {
	for(x in 1:nrow(total)) {
		for(y in 1:ncol(total)) {
			if(!is.na(total[x,y]) && total[x,y] >= threshold) {
				total[x,y] = 1
			} else {
				total[x,y] = 0
			}
		}
	}
	total
}

positives = function(total) {
	for(x in 1:nrow(total)) {
		for(y in 1:ncol(total)) {
			if(!is.na(total[x,y]) && total[x,y] < 0) {
				total[x,y] = 0
			}
		}
	}
	total
}
