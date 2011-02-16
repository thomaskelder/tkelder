library(TANOVA)

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

timeFactor.no18 = factor(as.character(timeFactor[timeFactor != "t18"]))
dietFactor.no18 = factor(as.character(dietFactor[timeFactor != "t18"]))

times = as.character(timeFactor.no18)
i = 1
for(f in levels(timeFactor.no18)) {
	times[times == f] = i
	i = i + 1
}
times = as.numeric(times)

diets = as.character(dietFactor.no18)
i = 1
for(f in levels(dietFactor.no18)) {
	diets[diets == f] = i
	i = i + 1
}
diets = as.numeric(diets)

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
data.no18 = data[, timeFactor != "t18"]

tan = tanova(data.no18, diets, 1:ncol(data.no18), times)
