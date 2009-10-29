## Gene-level statistics for PPS2 kidney data ##
data.dir = "/home/thomas/projects/pps2/heeringa/";
data.file = paste(data.dir, "Gopi(24-06-2009) normalized.txt", sep="")

library(beadarray)
library(illuminaMousev2BeadID.db)

# Read sample annotations
sample.annot = read.delim(
	paste(data.dir, "sample_annot.txt", sep=""),
	as.is = TRUE, row.names = 1
)

# Read data
data.raw = read.delim(
	file = data.file, as.is = TRUE, row.names = 2, dec= ",", skip = 7, na.strings = "NaN"
)

# Extract AVG_Signal columns
avg.cols = grep("AVG_Signal", colnames(data.raw))
data.avg = data.raw[,avg.cols]

# Remove "AVG_Signal-" prefix
colnames(data.avg) = substr(colnames(data.avg), 12, 1000)

# Log2 transform average expression
# First add lowest value + 1, so the minimum expression will be 1 to prevent NAs after log transformation
data.avg.nozero = data.avg - min(data.avg) + 1
data.avg.log = log2(data.avg.nozero)

# Build factor vectors
diet.factor = factor(sample.annot[colnames(data.avg.log), "Diet"])
tissue.factor = factor(sample.annot[colnames(data.avg.log), "Tissue"])

#### QC ####
dev.new()
boxplot(as.data.frame(data.avg.log), outline = F, las = 2, ylab = "log2(intensity)")
############

#### Cluster ####
d = dist(t(data.avg.log))
dev.new()
plot(hclust(d), labels = paste(diet.factor, tissue.factor, sep="_"))
#################



# Gene level statistics
vars = paste(tissue.factor, diet.factor, sep=".")
vars = factor(vars)
design = model.matrix(~0 + vars)
colnames(design) = levels(vars)
fit = lmFit(data.avg.log, design)
cont.matrix = makeContrasts(
	MED.HFvsLF = MED.HF - MED.LF,
	COR.HFvsLF = COR.HF - COR.LF,
	WK.HFvsLF = WK.HF - WK.LF,
	levels = design
)
fit2 = contrasts.fit(fit, cont.matrix)
ebFit = eBayes(fit2)

# Venn diagram
png(filename = paste(data.dir, "venn_p0.05.png", sep=""), width=1200, height=1200)
vennDiagram(decideTests(ebFit, adjust.method=NULL, p.value = 0.05), main="Probes with p <= 0.05 in HF vs LF comparison", names=c("Med", "Cor", "Wk"), cex.main = 3, cex = 3, mar=c(5,5,5,5))
dev.off()

# Annotate to Ensembl
ids = rownames(data.avg.log)
entrez = mget(ids, illuminaMousev2BeadIDENTREZID, ifnotfound = NA)
genename = mget(ids, illuminaMousev2BeadIDGENENAME, ifnotfound = NA)
anno = cbind(Ill_ID = as.character(ids),
		Entrez_ID = as.character(entrez),
		Name = as.character(genename))
ebFit$genes = anno

write.fit(fit = ebFit, file = paste(data.dir, "pps2_kidney_illumina.txt", sep=""), adjust="BH")
#data.txt = cbind(ebFit$genes, data.mean, data.ratio, ebFit$coefficient, ebFit$p.value, ebFit$t);
#colnames(data.txt) = c(
#	colnames(ebFit$genes),
#	colnames(data.mean),
#	colnames(data.ratio),
#	paste(colnames(ebFit$coefficient), "coefficient", sep="."),
#	paste(colnames(ebFit$p.value), "p.value", sep="."),
#	paste(colnames(ebFit$t), "T", sep="_")
#)
#write.table(data.txt, file = paste(data.dir, "pps2_kidney_illumina.txt", sep=""),
#	row.names = FALSE, sep = "\t", quote = FALSE, na = "NaN")










### OLD
# Calculate averages
colnames.mean = vector(
	mode = "character", 
	length = length(levels(tissue.factor)) * length(levels(diet.factor))
)

data.mean = array(NA, 
		dim = c(
			nrow(data.avg.log), 
			length(levels(tissue.factor)) * length(levels(diet.factor))
		),
)

col = 1
for(diet in levels(diet.factor)) {
	for(tissue in levels(tissue.factor)) {
		colnames.mean[col] = paste("log2", diet, tissue, sep="_")
		print(colnames.mean[col])
		data.mean[,col] = 
			rowMeans(data.avg.log[,tissue.factor == tissue & diet.factor == diet], na.rm = TRUE)
		col = col + 1
	}
}
colnames(data.mean) = colnames.mean
rownames(data.mean) = rownames(data.avg.log)

# Calculate ratios HFvsLF
colnames.ratio = vector(
	mode = "character", 
	length = length(levels(tissue.factor))
)

data.ratio = array(NA, 
		dim = c(
			nrow(data.avg.log), 
			length(levels(tissue.factor))
		),
)

col = 1
for(tissue in levels(tissue.factor)) {
		colnames.ratio[col] = paste("HF_vs_LF", tissue, sep="_")
		print(colnames.ratio[col])
		data.ratio[,col] = 
			data.mean[,paste("log2", "HF", tissue, sep="_")] - data.mean[,paste("log2", "LF", tissue, sep="_")]
		col = col + 1
}
colnames(data.ratio) = colnames.ratio
rownames(data.ratio) = rownames(data.avg.log)
