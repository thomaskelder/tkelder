##
# Reads the tab delimited text files, exported from the
# excel files from the nbx using xls2txt.
# Combines the data into a single matrix (containing all probesets) and
# calculates averages per timepoint.
##
inPath = "/home/thomas/projects/pps1/filtered_data/"
outPath = inPath

tissues = c(
	"Liver",
	"Muscle",
	"WAT"
)
excelFiles = c(
	"Liver_customCDF_filteredlist",
	"Muscle_customCDF_bc_filteredlist",
	"WAT 38samples_customCDF_filteredlist"
)
names(excelFiles) = tissues

## Read the excel files
#Files are converted with naming scheme fn.xls -> fn.xls_sheetnr.txt
## Specific properties of these excel files we have to deal with....:-(
#- First sheet contains log2 values (sheetnr =  0)
#- First row is empty or contains comments
#- Second row contains time headers
## Inconsistensies of these excel files we have to deal with....:-(
#- Only for WAT second row contains animal ids (?) and third contains times
#- First column are probeset ids (use as row names)
#- Last column contains t0 average and should be ignored
#- Before last column for Muscle contains expr > 5 count

rawData = list()
for(tis in tissues) {
	skip = 2
	if(tis == "WAT") skip = 3
	data = read.delim(
		paste(inPath, excelFiles[[tis]], ".xls_0.txt", sep=""), 
		as.is = TRUE, skip = skip, row.names = 1
	)
	truncateCol = 1
	if(tis == "Muscle") truncateCol = 2
	rawData[[tis]] = data[,1:(ncol(data) - truncateCol)]
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
		relData[,col] = avgData[, t0col] - avgData[, avgCol]
		col = col + 1
	}
}
colnames(relData) = relColNames
rownames(relData) = rownames(totalData)

## Get EntrezGene Ids from the rownames
entrezIds = sub("_at", "", rownames(totalData))
names(entrezIds) = rownames(totalData)

## Save the processed data
save(entrezIds, timeFactor, tissueFactor, totalData, avgData, relData,
	file = paste(outPath, "pps1_log2_combined.Rd", sep=""))
	

