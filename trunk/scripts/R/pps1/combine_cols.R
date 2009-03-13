inPath = "/home/thomas/projects/pps/filtered_data/"
rowNameFile = "all_genes.txt"

rownames = read.delim(paste(inPath, rowNameFile, sep=""), as.is=TRUE, header=FALSE)

#Rownames should be in first column, will be looked up using rowNameFile
#and combined in total file

colFiles = c(
	"WAT_total.txt", "Liver_total.txt", "Muscle_total.txt",
	"WAT_pvalues.txt", "Liver_pvalues.txt", "Muscle_pvalues.txt"
)
#colFiles = c("WAT_2log_values.txt")

tables = list()

#Read the data files
ncols = 0
for(i in 1:length(colFiles)) {
	tables[[i]] = read.delim(paste(inPath, colFiles[i], sep=""), row.names=1)
	ncols = ncols + ncol(tables[[i]])
}


total = array(dim = c(dim(rownames)[1], ncols + 1))
total[,1] = rownames[,1]

#Fill the rows
for(r in 1:dim(total)[1]) {
	if(r %% 100 == 0) {
		print(r)
	}
	c = 2
	for(t in 1:length(tables)) {
		d = tables[[t]]
		total[r, c:(c + ncol(d) - 1)] = as.character(d[as.character(total[r, 1]), ])
		c = c + ncol(d)
	}
}

colnames = c("id")
for(i in 1:length(tables)) {
	colnames = c(colnames, colnames(tables[[i]]))
}
colnames(total) = colnames

total = replace(total, total == "NA", "NaN")
write.table(total, file="Combined_total.txt", quote=FALSE, sep="\t", row.names=FALSE)
#write.table(total, file="WAT_2log_values_all.txt", quote=FALSE, sep="\t", row.names=FALSE)
