library(qvalue)

inPath = "/home/thomas/projects/pps2/stat_results/"
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

# Perform a t-test for HF vs LF for each timepoint
# Output:
# F statistic
# q-value
# log2 ratio
rawData = read.delim(
	paste(inPath, "PPS2_filteredlist_2log.txt", sep=""), 
	as.is = TRUE, row.names = 1
)
# Remove first two columns
rawData = rawData[,3:ncol(rawData)]

## Calculate averages per timepoint (for visualization in PathVisio)
# Create the column names (concatenate tissue and time)
avgColNames = vector(
	mode = "character", 
	length = length(levels(timeFactor)) * length(levels(dietFactor))
)

avgData = array(NA, 
		dim = c(
			nrow(rawData), 
			length(levels(timeFactor)) * length(levels(dietFactor))
		),
)

col = 1
for(diet in levels(dietFactor)) {
	for(time in levels(timeFactor)) {
		avgColNames[col] = paste(diet, time, sep="_")
		print(avgColNames[col])
		avgData[,col] = 
			rowMeans(rawData[,timeFactor == time & dietFactor == diet], na.rm = TRUE)
		col = col + 1
	}
}
colnames(avgData) = avgColNames
rownames(avgData) = rownames(rawData)

# Calculate ratios HFvsLF
ratioColNames = vector(
	mode = "character", 
	length = length(levels(timeFactor))
)

ratioData = array(NA, 
		dim = c(
			nrow(rawData), 
			length(levels(timeFactor))
		),
)

col = 1
for(time in levels(timeFactor)) {
		ratioColNames[col] = paste("HF_vs_LF", time, sep="_")
		print(ratioColNames[col])
		ratioData[,col] = 
			avgData[,paste("HF", time, sep="_")] - avgData[,paste("LF", time, sep="_")]
		col = col + 1
}
colnames(ratioData) = ratioColNames
rownames(ratioData) = rownames(rawData)

# Calculate q-values for HFvsLF for each timepoint
applyTtest<-function(x,v1,v2) {
	ttest <- function(x){
		t.test(x[v1], x[v2], alternative = "two.sided", paired = FALSE, var.equal = FALSE)
	};
	apply(x, 1, ttest);	
}
ttestPvalues<-function(ttest.res){
	as.numeric(lapply(ttest.res, function(x){x$p.value}));
};
ttestTvalues<-function(ttest.res){
	as.numeric(lapply(ttest.res, function(x){x$statistic}));
};

tvalues = list()
pvalues = list()
qvalues = list()
for(time in levels(timeFactor)) {
	message(time)
	hf = timeFactor == time & dietFactor == "HF"
	lf = timeFactor == time & dietFactor == "LF"
	t.res = applyTtest(rawData, hf, lf)
	t = ttestTvalues(t.res)
	p = ttestPvalues(t.res)
	q = qvalue(p)$qvalue
	names(p) = row.names(rawData)
	names(q) = row.names(rawData)
	pvalues[[time]] = p
	qvalues[[time]] = q
	tvalues[[time]] = t
}
names(qvalues) = levels(timeFactor)
names(tvalues) = levels(timeFactor)

txtData = cbind(rownames(rawData), avgData, ratioData, qvalues[["t0"]], qvalues[["t0.6"]], qvalues[["t2"]], qvalues[["t18"]], qvalues[["t48"]],
tvalues[["t0"]], tvalues[["t0.6"]], tvalues[["t2"]], tvalues[["t18"]], tvalues[["t48"]]);
colnames(txtData) = c("geneId", colnames(avgData), colnames(ratioData), paste("q", levels(timeFactor), sep="_"), paste("T", levels(timeFactor), sep="_"))
write.table(txtData, file = paste(outPath, "pps2_HFvsLF_alltimes.txt", sep=""),
	row.names = FALSE, sep = "\t", quote = FALSE, na = "NaN")
	
	
#Compare ttest across tissues
compare = matrix(0, nrow = nrow(rawData), ncol = length(levels(timeFactor)))
rownames(compare) = rownames(rawData)
colnames(compare) = levels(timeFactor)

for(time in levels(timeFactor)) {
	rows = names(qvalues[[time]])[qvalues[[time]] < 0.05]
	compare[rows, time] = 1
}
vennDiagram(compare[,c("t0", "t2", "t48")])
