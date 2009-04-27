## Calculate correlation matrix for the pps1 data

dataPath = "/home/thomas/projects/pps1/filtered_data/"
statPath = "/home/thomas/projects/pps1/stat_results/"

## Load previously calculated data (pps1_perpare_data.R and pps1_anova.R)
load(paste(dataPath, "pps1_log2_combined.Rd", sep=""))
load(paste(statPath, "pps1_anova_pqvalues.Rd", sep=""))

tissues = levels(tissueFactor)

correlation = list()

maxq = 0.01

for(tis in tissues) {
	message("Calculating correlation for ", tis)
	sig = names(qvalues[[tis]])[qvalues[[tis]] <= maxq]
	corData = totalData[, tissueFactor == tis]
	corData = corData[sig,]
	message(nrow(corData), " genes...")
	corData = t(corData)
	cm = cor(corData, corData, method = "pearson", use = "na.or.complete")
	correlation[[tis]] = cm
}

for(tis in tissues) {
	# Write to txt file
	cm = abs(correlation[[tis]])
	sig = names(qvalues[[tis]])[qvalues[[tis]] <= maxq]
	rownames(cm) = entrezIds[sig]
	colnames(cm) = entrezIds[sig]
	write.table(cm, 
			file = paste(statPath, "pps1_", tis, "_q", maxq, "_abs_corr.txt", sep=""), 
			col.names=NA, quote=FALSE, sep="\t"
	)
}

profiles = function(data, ...) {
	plot(x = 1:ncol(data), y = data[1,], ylim = range(data, na.rm = T))
	for(i in 2:nrow(data)) {
		lines(x = 1:ncol(data), y = data[i,])
	}
}

tis = "Liver"

corcounts = apply(correlation[[tis]], 2, function(x) sum(x >= 0.9, na.rm = T))
corcounts[order(corcounts)]

rows = correlation[[tis]][,"18563_at"] > 0.9
profiles(avgData[sig[rows], avgTissueFactor == tis])
profiles(totalData[sig[rows], tissueFactor == tis])
