##################################################
## Perform gene set tests for the pps2 analysis ##
## Exports results and generates heatmaps       ##
##################################################

library(gplots) #For venn diagrams
library(limma)

src = "scripts/"
source(paste(src, "ShortestPath.R", sep=""))
source(paste(src, "PPS2Functions.R", sep=""))

# Generates a heatmap from the gene set test results ##
doHeatmap = function(gst, prefix, p1 = 0.05, p2 = 0.01, cp = colorRampPalette(c('green', 'white', 'red'))(128), cexRow = 2, sym = T, ...) {
	sig = abs(gst) < p2
	sig.1 = abs(gst) < p1
	onesig = apply(sig, 1, function(x) sum(x) > 0)
	
	if(min(gst[onesig,]) == 0) {
		nm = sort(unique(as.numeric(gst[onesig,])))[2] / 10
		print(nm)
		gst[onesig,] = each(gst[onesig,], function(x) {
			if(x == 0) nm
			else x
		})
		print(range(gst[onesig,]))
	}
	signed.log = -log10(abs(gst[onesig,]))
	signed.log = signed.log * sign(gst[onesig,])
	rownames(signed.log) = pathwayTitles[onesig]
	write.table(signed.log, paste(prefix, ".txt", sep=""), sep="\t", col.names=NA, quote=F)

	hr = hclust(dist(signed.log), method="average")
	dg = as.dendrogram(hr)

	notes = each(sig.1[onesig,], function(x) if(x) "*" else "")
	for(x in 1:nrow(notes)) for(y in 1:ncol(notes)) {
		if(sig[onesig,][x,y]) notes[x,y] = "**"
	}
	notes = notes[order.dendrogram(dg),]
	notes = t(notes)
	addtext <<- function() {
		text(x = c(row(notes)), y = c(col(notes)), labels=notes, col='black', cex=3)
	}
	
	zlim = max(abs(range(signed.log)))
	if(sym) {
		zlim = c(-zlim, zlim)
	} else {
		zlim = c(0, zlim)
	}
	print(zlim)
	
	svg(paste(prefix, ".svg", sep=""), 25, 30)
	par(cex.main = 3, mar = c(0, 0, 0, 0), oma = c(0,15,0,0))
	heatmap(
		signed.log[order.dendrogram(dg),], Colv=NA, Rowv = NA, col=cp, keep.dendro=F,
		add.expr=addtext(),	cexCol = 5, cexRow = cexRow, margins=c(0,75),
		scale = "none", zlim = zlim, ...
	)
	dev.off()
}

pathways = readPathways("input/pathways_merged")
pathwayTitles = readPathwayTitles("input/pathways_merged")

task = getTestNames()
gst.list = ppsGeneSetTests(pathways)

gst.list.mixed = ppsGeneSetTests(pathways, mixed = T, ranks.only=F)

gst = foreach(tn = task, .combine = cbind) %do% {
	ps = as.numeric(gst.list[[tn]])
}
rownames(gst) = names(pathways)
colnames(gst) = task

gst.mixed = foreach(tn = task, .combine = cbind) %do% {
	ps = as.numeric(gst.list.mixed[[tn]])
}
rownames(gst.mixed) = names(pathways)
colnames(gst.mixed) = task

#Compare mixed vs updown
#Plot venn diagram for overlap in each test
p = 0.05
par(mfrow=c(2,5))
for(t in task) {
	mi = gst.mixed[,t] < p
	up = gst[,t] > 0 & abs(gst[,t]) < p
	down = gst[,t] < 0 & abs(gst[,t]) < p
	vdata = cbind(mixed = mi, up = up, down = down)
	vennDiagram(vdata, main=t)
}

#Compare HF vs LF counts
gst.hflf = gst[,grep("HFvsLF", colnames(gst))]
colnames(gst.hflf) = c("t0", "t0.6", "t2", "t48")

png("output/images/venn_HFvsLF_pws_p0.05.png", 1024, 1024)
par(cex=2)
venn(as.data.frame(abs(gst.hflf) < 0.05), small=1)
title("Enriched pathways (p < 0.05) between HF and LF diet")
dev.off()

#Write significant HF vs LF pathways to table
doHeatmap(gst.hflf, "output/images/heatmap_pws_HFvsLF", 
	main="Pathway enrichment for\nHF vs LF differential expression"
)

#Write LF response
gst.lf = gst[,grep("LFt0vs", colnames(gst))]
doHeatmap(gst.lf, "output/images/heatmap_pws_lf.", 
	main="Pathway enrichment for\ndifferential expression relative to t=0 in LF"
)

#Write HF response
gst.hf = gst[,grep("HFt0vs", colnames(gst))]
doHeatmap(gst.hf, "output/images/heatmap_pws_hf", 
	main="Pathway enrichment for\ndifferential expression relative to t=0 in HF"
)

#Both HF and LF in one heatmap
doHeatmap(cbind(gst.lf, gst.hf), "output/images/heatmap_pws_lf_hf", 
	main="Pathway enrichment for\ndifferential expression relative to t=0"
)

#All in one heatmap
gst.total = cbind(gst.hflf[,1], gst.lf, gst.hf)
colnames(gst.total) = c("HFvsLF t=0",
	 paste("LF t0vst", c("0.6", "2", "48"), sep=""),
	 paste("HF t0vst", c("0.6", "2", "48"), sep="")
)
doHeatmap(gst.total, "output/images/heatmap_pws_all", 
	main="Pathway enrichment for\ndifferential expression."
)

gst.total.mixed = cbind(gst.mixed[,c(1,5,6,7,8,9,10)])
colnames(gst.total.mixed) = c("HFvsLF t=0",
	 paste("LF t0vst", c("0.6", "2", "48"), sep=""),
	 paste("HF t0vst", c("0.6", "2", "48"), sep="")
)
doHeatmap(gst.total.mixed, "output/images/heatmap_pws_all_mixed", 
	main="Pathway enrichment for\ndifferential expression.",
	cp = colorRampPalette(c('white', 'blue'))(128), sym=F
)

## Test results
file = "input/weights/pps2.2log.stats.mapped.txt"
data = read.delim(file, as.is=T)
p = pathways[[197]]

dobarplots = function(p, data, task, ...) {
	par(mfrow=c(2,5))
	for(t in task) {
		stats = data[,t]
		sel = data[,'mapped_xref'] %in% p
		pup = round(geneSetTest(sel, stats, alternative="up", ...), 3)
		pdo = round(geneSetTest(sel, stats, alternative="down", ...), 3)
		pmi = round(geneSetTest(sel, stats, alternative="mixed", ...), 3)
		pei = round(geneSetTest(sel, stats, alternative="either", ...), 3)
		barcodeplot(sel, stats, main=paste(t, "\nup: ", pup, ", down: ", pdo, ", mix: ", pmi, ", eith: ", pei, sep=""))
	}
}

