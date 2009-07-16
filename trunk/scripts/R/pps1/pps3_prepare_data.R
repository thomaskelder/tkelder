##
# Reads the tab delimited text files, exported from the
# excel files from the nbx using xls2txt.
# Combines the data into a single matrix.
##
inPath = "/home/thomas/projects/pps3/"
outPath = inPath

types = c(
	"overall",
	"time",
	"diet",
	"leptin"
)
excelFiles = c(
	"PPS3 Leptin Module Proteomics Pathways by Grahams statistic.xls_0.txt",
	"PPS3 Leptin Module Proteomics Pathways by Grahams statistic.xls_1.txt",
	"PPS3 Leptin Module Proteomics Pathways by Grahams statistic.xls_2.txt",
	"PPS3 Leptin Module Proteomics Pathways by Grahams statistic.xls_3.txt"
)
names(excelFiles) = types

rawData = list()
for(t in types) {
	skip = 1
	data = read.delim(
		paste(inPath, excelFiles[[t]], sep=""), 
		as.is = TRUE, skip = skip, row.names = 11
	)
}

## Copy to a unified matrix (all probesets in either one of the three)
source("combine_matrix.R")
totalData = combineByRow(rawData)

## Create time and tissue factors
timeFactor = list()
for(col in colnames(totalData)) {
	res = regexpr(pattern = "t([[:digit:]]+)_", text = col)
	if(res > -1) {
		t = substr(col, res[1] + 1, attr(res, "match.length") + res[1] - 2)
		print(paste("Parsing time", t, "for column", col))
		timeFactor[[col]] = t
	} else {
		error("Unable to parse time from column name!")
	}
}
timeFactor = factor(as.numeric(timeFactor))

tissueFactor = vector(mode = "character", length = ncol(totalData))
c = 1
for(tis in tissues) {
	ncolTis = ncol(rawData[[tis]])
	tissueFactor[c:(c + ncolTis - 1)] = tis
	c = c + ncolTis
}
tissueFactor = factor(tissueFactor)

## Calculate averages per timepoint (for visualization in PathVisio)
# Create the column names (concatenate tissue and time)
avgColNames = vector(
	mode = "character", 
	length = length(levels(timeFactor)) * length(levels(tissueFactor))
)

avgData = array(NA, 
		dim = c(
			nrow(totalData), 
			length(levels(timeFactor)) * length(levels(tissueFactor))
		),
)

col = 1
for(tis in levels(tissueFactor)) {
	for(time in levels(timeFactor)) {
		avgColNames[col] = paste(tis, time, sep="_")
		avgData[,col] = 
			rowMeans(totalData[,timeFactor == time & tissueFactor == tis], na.rm = TRUE)
		col = col + 1
	}
}
colnames(avgData) = avgColNames
rownames(avgData) = rownames(totalData)

avgTissueFactor = factor(unlist(lapply(levels(tissueFactor), function(t) {
	rep(t, length(levels(timeFactor)))
})))

avgTimeFactor = factor(rep(levels(timeFactor), length(levels(tissueFactor))))

## Calculate expression relative to t=0
t0 = levels(timeFactor)[1]

relColNames = vector(
	mode = "character", 
	length = ncol(avgData) - length(levels(tissueFactor))
)
relData = array(NA,
		dim = c(
			nrow(totalData),
			length(relColNames)
		)
)
col = 1
for(tis in levels(tissueFactor)) {
	t0col = paste(tis, t0, sep="_")
	for(time in levels(timeFactor)[2:length(levels(timeFactor))]) {
		avgCol = paste(tis, time, sep="_")
		relColNames[col] = paste(tis, "relt0", time, sep="_")
		relData[,col] = avgData[, avgCol] - avgData[, t0col]
		col = col + 1
	}
}
colnames(relData) = relColNames
rownames(relData) = rownames(totalData)

## Calculate raw expression relative to average t=0
relTotalColNames = vector(
	mode = "character", 
	length = ncol(totalData) - sum(timeFactor == t0)
)
relTotalData = array(NA,
		dim = c(
			nrow(totalData),
			length(relTotalColNames)
		)
)
col = 1
for(tis in levels(tissueFactor)) {
	t0AvgCol = paste(tis, t0, sep="_")
	for(time in levels(timeFactor)[2:length(levels(timeFactor))]) {
		timeCols = timeFactor == time & tissueFactor == tis
		for(tc in colnames(totalData)[timeCols]) {
			relTotalColNames[col] = paste(tis, "relt0_t", time, col, sep="_")
			relTotalData[, col] = totalData[, tc] - avgData[, t0AvgCol]
			col = col + 1
		}
	}
}
colnames(relTotalData) = relTotalColNames
rownames(relTotalData) = rownames(totalData)

relTissueFactor = tissueFactor[timeFactor != t0]
relTimeFactor = timeFactor[timeFactor != t0]

## Get EntrezGene Ids from the rownames
entrezIds = sub("_at", "", rownames(totalData))
names(entrezIds) = rownames(totalData)

## Add a column with the max foldchange (relative to t=0)
maxAbsFc = array(NA, dim = c(nrow(avgData), length(levels(tissueFactor))))

maxAbsFcList = lapply(levels(tissueFactor), function(tis) {
	cols = avgTissueFactor == tis
	cols = cols[avgTimeFactor != t0]
	apply(abs(relData[, cols]), 1, function(row) {
		max(row, na.rm = TRUE)
	})
})
for(i in 1:length(maxAbsFcList)) {
	maxAbsFc[,i] = maxAbsFcList[[i]]
}
colnames(maxAbsFc) = paste("max_abs_fc_", levels(tissueFactor), sep="")
rownames(maxAbsFc) = rownames(avgData)
maxAbsFc[maxAbsFc == -Inf] = NA

## Save the processed data
save(entrezIds, timeFactor, tissueFactor, totalData, avgData, relData, relTotalData, relTissueFactor, relTimeFactor, avgTissueFactor, avgTimeFactor, maxAbsFc,
	file = paste(outPath, "pps1_log2_combined.Rd", sep=""))
	
	
### Preparation of the Oslo data ###
inPath = "/home/thomas/projects/pps1/stat_results/adipose tissue data_Oslo/"
outPath = inPath

tissues = c(
	"sub",
	"vis",
	"gon"
)
excelFiles = c(
	"AT_Oslo_filteredlist 2log_stats results Firenze.xls_1.txt",
	"AT_Oslo_filteredlist 2log_stats results Firenze.xls_2.txt",
	"AT_Oslo_filteredlist 2log_stats results Firenze.xls_3.txt"
)
names(excelFiles) = tissues

## Read the excel files
#Files are converted with naming scheme fn.xls -> fn.xls_sheetnr.txt
## Specific properties of these excel files we have to deal with....:-(
#- Second line is used as header
#- Row names are entrez ids, appended with _at, we should remove this on export

rawData = list()
for(tis in tissues) {
	skip = 1
	data = read.delim(
		paste(inPath, excelFiles[[tis]], sep=""), 
		as.is = TRUE, skip = skip, row.names = 1
	)
	# Only select these columns
	cols = c(
		"t0","t1","t6","t9","t12","t1.t0","t6.t0","t9.t0","t12.t0","qvalue_F"
	)
	print(colnames(data))
	data = data[,cols]
	colnames(data) = paste(tis, cols, sep="_")
	rawData[[tis]] = data
}

source("combine_matrix.R")
totalData = combineByRow(rawData)

## Get EntrezGene Ids from the rownames
entrezIds = sub("_at", "", rownames(totalData))
names(entrezIds) = rownames(totalData)

# Write a text file for PathVisio
txtData = cbind(entrezIds, totalData)
write.table(txtData, file = paste(outPath, "AT_Oslo_combined.txt", sep=""),
	row.names = FALSE, sep = "\t", quote = FALSE, na = "NaN")
	

