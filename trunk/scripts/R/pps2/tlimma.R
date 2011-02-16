library(limma)
library(qvalue)

source("tdata.R")

mm = model.matrix(~0 + combined.no18)
colnames(mm) = levels(combined.no18)
lm.fit = lmFit(data.no18, mm)

## LF vs HF
mc = makeContrasts(
	HFvsLFt0 = HFt0-LFt0, HFvsLFt0.6 = HFt0.6-LFt0.6, HFvsLFt2 = HFt2-LFt2, HFvsLFt48 = HFt48-LFt48, 
	levels=combined.no18
)
c.fit = contrasts.fit(lm.fit, mc)
eb = eBayes(c.fit)

#plotRankedProfiles(data.no18, rank(eb$F))

q = apply(eb$p.value, 2, function(x) qvalue(x)$qvalue)

## Response in LF
mc.lf = makeContrasts(
	LFt0.6-LFt0, LFt2-LFt0, LFt48-LFt0,
	LFt2-LFt0.6, LFt48-LFt0.6,
	LFt48-LFt2, 
	levels=combined.no18
)
c.fit.lf = contrasts.fit(lm.fit, mc.lf)
eb.lf = eBayes(c.fit.lf)

#plotRankedProfiles(data.no18, rank(eb.lf$F))

q.lf = qvalue(eb.lf$F.p.value)$qvalue

## Response in HF
mc.hf = makeContrasts(
	HFt0.6-HFt0, HFt2-HFt0, HFt48-HFt0,
	HFt2-HFt0.6, HFt48-HFt0.6,
	HFt48-HFt2, 
	levels=combined.no18
)
c.fit.hf = contrasts.fit(lm.fit, mc.hf)
eb.hf = eBayes(c.fit.hf)

#plotRankedProfiles(data.no18, rank(eb.hf$F))

q.hf = qvalue(eb.hf$F.p.value)$qvalue

## Different time response
mc.td = makeContrasts(
	dt0.6 = (HFt0.6-HFt0)-(LFt0.6-LFt0),
	dt2 = (HFt2-HFt0)-(LFt2-LFt0),
	dt48 = (HFt48-HFt0)-(LFt48-LFt0),
	levels=combined.no18
)
c.fit.td = contrasts.fit(lm.fit, mc.td)
eb.td = eBayes(c.fit.td)

#plotRankedProfiles(data.no18, rank(eb.td$F))

q.td = qvalue(eb.td$F.p.value)$qvalue

## Difference relative to t = 0
mc.t0 = makeContrasts(
	LFt0vst0.6 = LFt0.6-LFt0, 
	LFt0vst2 = LFt2-LFt0, 
	LFt0vst48 = LFt48-LFt0,
	HFt0vst0.6 = HFt0.6-HFt0, 
	HFt0vst2 = HFt2-HFt0, 
	HFt0vst48 = HFt48-HFt0,
	levels=combined.no18
)
c.fit.t0 = contrasts.fit(lm.fit, mc.t0)
eb.t0 = eBayes(c.fit.t0)

q.t0 = apply(eb.t0$p.value, 2, function(x) qvalue(x)$qvalue)

vennDiagram(q.t0[,1:3] < 0.05)
dev.new()
vennDiagram(q.t0[,4:6] < 0.05)

## Venn diagram for response in HF vs LF
vc = cbind(as.numeric(q.hf <= 0.01), as.numeric(q.lf <= 0.01), as.numeric(q.td <= 0.01))
colnames(vc) = c("HF Time", "LF Time", "Diet.Time")
vennDiagram(vc)
dev.new()

## Venn diagram for HFvsLF at each timepoint
library(gplots)
vc_hflf = sapply(levels(time.no18), function(t) as.numeric(q[,paste("HFvsLF", t, sep="")] < 0.01))

png("venn_HFvsLF_q0.01.png", 1024, 1024)
par(cex=2)
venn(as.data.frame(vc_hflf), small=1)
title("Genes differentially expressed (q < 0.01) between HF and LF diet")
dev.off()

## Significant counts per test
shl = sapply(levels(time.no18), function(t) {
	sum(q[,paste("HFvsLF", t, sep="")] <= 0.01)
})
st = sapply(list(HF_time = q.hf, LF_time = q.lf, time_diet = q.td), function(t) {
	sum(t <= 0.01)
})
100 * shl / nrow(data.no18) #Percentages
100 * st / nrow(data.no18)

if(plot) {
	## Plot the profiles of the venn slices
	## Only HF
	plotProfiles(data.no18, which(vc[,"HF Time"] & !vc[,"LF Time"] & !vc[,"Diet.Time"]))
	## Only LF
	plotProfiles(data.no18, which(!vc[,"HF Time"] & vc[,"LF Time"] & !vc[,"Diet.Time"]))
	## Only Diet+Time
	plotProfiles(data.no18, which(!vc[,"HF Time"] & !vc[,"LF Time"] & vc[,"Diet.Time"]))
	## HF and LF but not Diet+Time
	plotProfiles(data.no18, which(vc[,"HF Time"] & vc[,"LF Time"] & !vc[,"Diet.Time"]))
}
## Write file with stats
outData = cbind(rownames(rawData), eb$coefficient, eb$t, q, eb.lf$F, q.lf, eb.hf$F, q.hf, eb.td$F, q.td)
colnames(outData) = c("id", paste(colnames(eb$t), "fc", sep="_"), paste(colnames(eb$t), "T", sep="_"), paste(colnames(q), "q", sep="_"), 
	"LF_time_F", "LF_time_q", "HF_time_F", "HF_time_q", "diet_time_F", "diet_time_q")
write.table(outData, file=paste(outPath, "PPS2_filteredlist_2log.stats.txt", sep=""), row.names=F, quote=F, sep="\t")

outData = cbind(rownames(rawData), eb$coefficient, eb$t, q)
outData = cbind(outData, eb.t0$coefficient, eb.t0$t, q.t0)
colnames(outData) = c(
	"id", 
	paste(colnames(eb$t), "fc", sep="_"), 
	paste(colnames(eb$t), "T", sep="_"), 
	paste(colnames(q), "q", sep="_"), 
	paste(colnames(eb.t0$t), "fc", sep="_"), 
	paste(colnames(eb.t0$t), "T", sep="_"), 
	paste(colnames(q.t0), "q", sep="_")
)
write.table(outData, file=paste(outPath, "pps2.2log.stats.txt", sep=""), row.names=F, quote=F, sep="\t")

