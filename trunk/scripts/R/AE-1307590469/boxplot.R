library(affy);

exprs.gcrma.q = exprs(eset.gcrma.q)

#Create class averages

groups = c(
	"SconSa_24wks",	"SconSa_6wks",	"SconSa_24wks",
	"SconSa_6wks",	"SconSa_24wks",	"SconSa_6wks",
	"SconSa_24wks",	"SconSa_6wks",	"SconSa_24wks",
	"SconSa_6wks",	"SHR_24wks",	"SHR_6wks",
	"SHR_24wks",	"SHR_6wks",		"SHR_24wks",
	"SHR_6wks",		"SHR_24wks",	"SHR_6wks",
	"SHR_24wks",	"SHR_6wks",		"SISA_24wks",
	"SISA_6wks",	"SISA_24wks",	"SISA_6wks",
	"SISA_24wks",	"SISA_6wks",	"SISA_24wks",
	"SISA_6wks",	"SISA_24wks",	"SISA_6wks",
	"WconSa_24wks",	"WconSa_6wks",	"WconSa_24wks",
	"WconSa_6wks",	"WconSa_24wks",	"WconSa_6wks",
	"WconSa_24wks",	"WconSa_6wks",	"WconSa_24wks",
	"WconSa_6wks",	"WKY_24wks",	"WKY_6wks",
	"WKY_24wks",	"WKY_6wks",		"WKY_24wks",
	"WKY_6wks",		"WKY_24wks",	"WKY_6wks",
	"WKY_24wks",	"WKY_6wks"
)

classes = c(
	1, 2, 1, 2, 1, 2, 1, 2, 1, 2,
	3, 4, 3, 4, 3, 4, 3, 4, 3, 4,
	5, 6, 5, 6, 5, 6, 5, 6, 5, 6,
	7, 8, 7, 8, 7, 8, 7, 8, 7, 8,
	9, 10, 9, 10, 9, 10, 9, 10, 9, 10
)

sums = matrix(data = 0, nrow=nrow(exprs.gcrma.q), ncol=length(unique(classes)))
avgs = matrix(data = 0, nrow=nrow(exprs.gcrma.q), ncol=length(unique(classes)))

for(i in 1:nrow(exprs.gcrma.q)) {
	for(j in 1:length(classes)) {
		sums[i, classes[j]] = sums[i, classes[j]] + exprs.gcrma.q[i, j];
	}
}

for(i in 1:length(unique(classes))) {
	avgs[,i] = sums[,i] / sum(classes == i) 
}

rownames(avgs) = rownames(exprs.gcrma.q)
cn = 1:ncol(avgs)
for(i in 1:length(classes)) {
	cn[classes[i]] = groups[i]
}
colnames(avgs) = cn

plot(avgs[,"WKY_6wks"],avgs[,"SHR_6wks"])
plot(avgs[,"WKY_6wks"],avgs[,"WKY_24wks"])
plot(avgs[,"SHR_6wks"],avgs[,"SHR_24wks"])

