source("tdata.R")

##Write a file with average values per timepoint and their sd

data.mean = sapply(levels(combined.no18), function(f) {
	apply(data.no18[,combined.no18 == f], 1, mean, na.rm=T)
})
colnames(data.mean) = paste("mean_", colnames(data.mean), sep="")

data.sd = sapply(levels(combined.no18), function(f) {
	apply(data.no18[,combined.no18 == f], 1, sd, na.rm=T)
})

colnames(data.sd) = paste("sd_", colnames(data.sd), sep="")

outData = cbind(rownames(rawData), data.mean, data.sd)
colnames(outData) = c("id", colnames(data.mean), colnames(data.sd))
write.table(outData, file=paste(outPath, "PPS2_filteredlist_2log.mean_sd.txt", sep=""), row.names=F, quote=F, sep="\t")
