inPath = "/home/thomas/data/pps2/"
outPath = inPath

dietFactor = factor(c(
	"LF","LF","LF","LF","LF","LF","LF","LF",
	"LF","LF","LF","LF","LF","LF","LF","LF",
	"LF","LF","LF","LF","LF","LF","LF","LF",
	"LF","LF","LF","LF","LF","LF","LF","LF",
	"LF","LF","LF","LF","LF","LF","LF","LF",
	"HF","HF","HF","HF","HF","HF","HF","HF",
	"HF","HF","HF","HF","HF","HF","HF","HF",
	"HF","HF","HF","HF","HF","HF","HF","HF",
	"HF","HF","HF","HF","HF","HF","HF","HF",
	"HF","HF","HF","HF","HF","HF","HF","HF"
))
timeFactor = factor(c(
	"t0","t0","t0","t0","t0","t0","t0","t0",
	"t0.6","t0.6","t0.6","t0.6","t0.6","t0.6","t0.6","t0.6",
	"t2","t2","t2","t2","t2","t2","t2","t2",
	"t18","t18","t18","t18","t18","t18","t18","t18",
	"t48","t48","t48","t48","t48","t48","t48","t48",
	"t0","t0","t0","t0","t0","t0","t0","t0",
	"t0.6","t0.6","t0.6","t0.6","t0.6","t0.6","t0.6","t0.6",
	"t2","t2","t2","t2","t2","t2","t2","t2",
	"t18","t18","t18","t18","t18","t18","t18","t18",
	"t48","t48","t48","t48","t48","t48","t48","t48"
))

combinedFactor = as.factor(paste(dietFactor, timeFactor, sep=""))

rawData = read.delim(
	paste(inPath, "PPS2_filteredlist_2log.txt", sep=""), 
	as.is = TRUE, row.names = 1
)
# Get the gene names
geneInfo = rawData[,1:2]
geneNames = geneInfo[,2]
geneDescriptions = geneInfo[,1]
# Remove first two columns
data = rawData[,3:ncol(rawData)]
# Remove t18
data.no18 = data[, timeFactor != "t18"]
time.no18 = factor(as.character(timeFactor[timeFactor != "t18"]))
diet.no18 = factor(as.character(dietFactor[timeFactor != "t18"]))
combined.no18 = as.factor(paste(
	dietFactor[timeFactor!="t18"], time.no18, sep=""))

# Normalize signals to filter out constant differences
data.zero = t(apply(data.no18, 1, function(x) {
	lf = diet.no18 == "LF"
	hf = diet.no18 == "HF"
	lft0 = time.no18 == "t0" & lf
	hft0 = time.no18 == "t0" & hf
	x[lf] = x[lf] - median(x[lft0])
	x[hf] = x[hf] - median(x[hft0])
	x
}))

plotProfiles = function(data, which) {
	par(mfrow = c (3, 3), cex = 0.5, ask=T)

	for (indx in which){
		id = rownames(data)[indx]
		name = geneNames[indx]
		genetitle = paste(id, name)
		plotProfile(data, indx, genetitle)
	}
}

plotRankedProfiles = function(data, ranks, top=100) {
	par(mfrow = c (3, 3), cex = 0.5, ask=T)

	for (i in 1:top){
		indx = which(ranks == max(ranks) - (i - 1))
		id = rownames(data)[indx]
		name = geneNames[indx]
		genetitle = paste(id, name, 'Rank =', i)
		plotProfile(data, indx, genetitle)
	}
}

plotProfile = function(data, indx, genetitle) {
	ds = c("HF", "LF")
	cs = c("red", "blue")
	
	exprs.row = data[indx, ]

	plot(0, pch = NA, xlim = range(0, 4), ylim = range(exprs.row), 
		ylab = 'Expression', xlab = 'Time', main = genetitle
	)
	axis(1, at = 1:4, labels = levels(time.no18))
	for(d in 1:length(ds)) {
		for(r in 1:8) {
			exprs.row.d = exprs.row[diet.no18 == ds[d]]
			points(1:4, exprs.row.d[seq(r,r+3*8,by=8)], 
				type='b', pch = ds[d], col=cs[d])
		}
	}
}
