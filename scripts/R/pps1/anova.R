library(multtest)
library(limma)

p2fdr <- function(rawPvalues, proc = "BH") {
	pList <- NULL
	rawPvalues <- cbind(rawPvalues)
	for (i in seq(along = rawPvalues[1,])) {
		.temp <- mt.rawp2adjp(rawPvalues[, i], proc = proc)
		pList <- cbind(pList, .temp$adjp[order(.temp$index), proc])
	}
   	row.names(pList) <- row.names(rawPvalues)
	invisible(pList)
}

anovaPvalues<-function(x,factor){
	ttest <- function(x){
		m<-data.frame(factor, x);
		anova(aov(x ~ factor, m));
	};
	anova.res<-apply(x, 1, ttest);
	t(data.frame(lapply(anova.res, function(x){x["Pr(>F)"][1,]})));
};

inPath = "/home/thomas/projects/pps/filtered_data/"
tissues = c("Liver", "WAT", "Muscle")
pvaluesList = list()

for(tis in tissues) {
	data = read.delim(paste(inPath, tis, "_2log_values_all.txt", sep = ""), row.names=1)

	#Subset of rows that have only NAs
	subset = vector("logical", length = nrow(data))
	for(i in 1:nrow(data)) {
		subset[i] = sum(is.na(data[i,])) != ncol(data)
	}

	time = list()
	for(col in colnames(data)) {
		res = regexpr(pattern = "t([[:digit:]]+)_", text = col)
		if(res > -1) {
			t = substr(col, res[1], attr(res, "match.length") + res[1] - 2)
			print(paste("Parsing time", t, "for column", col))
			time[[col]] = t
		}
	}

	factor = factor(as.character(time))

	#ANOVA
	pvalues = anovaPvalues(data[subset, names(time)], factor)
	row.names(pvalues) = row.names(data)[subset]
	pvalues.fdr = p2fdr(pvalues)
	colnames(pvalues.fdr) = paste("anova_pvalue_", tis, sep="")
	write.table(pvalues.fdr, file=paste(inPath, "/", tis, "_pvalues.txt", sep=""), 
		quote=FALSE, sep="\t", row.names=TRUE, col.names=NA)
		
	pvaluesList[[tis]] = pvalues.fdr
}

#Compare anova with ebayes
compare = matrix(0, nrow = nrow(data), ncol = 3)
rownames(compare) = rownames(data)
colnames(compare) = tissues

for(tis in tissues) {
	rows = row.names(pvaluesList[[tis]])[pvaluesList[[tis]] < 0.01]
	compare[rows, tis] = 1
}
vennDiagram(compare)

##Ebayes
#design = model.matrix(~0 + factor)
#colnames(design) = levels(factor)
#rownames(design) = names(time)
#fit.lm = lmFit(data[subset, names(time)], design)
#fit.ebayes = eBayes(fit.lm)
#pvalues.ebayes = fit.ebayes$F.p.value
#pvalues.ebayes.fdr = p2fdr(pvalues.ebayes)

##Compare anova with ebayes
#compare = matrix(0, nrow = nrow(data), ncol = 2)
#rownames(compare) = rownames(data)
#colnames(compare) = c("anova", "ebayes")

#rows.anova = row.names(pvalues.fdr)[pvalues.fdr < 0.01]
#rows.ebayes = row.names(pvalues.fdr)[pvalues.ebayes.fdr < 0.01]
#compare[rows.anova, "anova"] = 1
#compare[rows.ebayes, "ebayes"] = 1
#vennDiagram(compare)

##Use limma to find changed reporters
#library(limma)
#f = factor(as.character(time))
#design = model.matrix(~0+f)
#colnames(design) = levels(f)

#fit = lmFit(data[subset, names(time)], design)

#contrasts = makeContrasts("t12-t9", "t9-t6", "t6-t1", "t1-t0", "t12-t0",
#	levels = c("t0", "t1", "t6", "t9", "t12"))
#fit2 = contrasts.fit(fit, contrasts)
#fit2 = eBayes(fit2)

#results.nf = decideTests(fit2, adjust="BH", p.value=0.01, method="nestedF")
#results = decideTests(fit2, adjust="BH", p.value=0.01)

#sig = apply(results, 1, function(x) { sum(abs(x)) > 0 }) #At least one significant
#sig.nf = apply(results.nf, 1, function(x) { sum(abs(x)) > 0 }) #At least one significant
#	
#contrasts.all = makeContrasts(
#	"t0-t1", "t0-t6", "t0-t9", "t0-t12",
#	"t1-t6", "t1-t9", "t1-t12",
#	"t6-t9", "t6-t12",
#	"t9-t12",		
#	levels = c("t0", "t1", "t6", "t9", "t12"))
#fit2.all = contrasts.fit(fit, contrasts.all)
#fit2.all = eBayes(fit2.all)

#results.all = decideTests(fit2.all, adjust="BH", p.value=0.01)
#sig.all = apply(results.all, 1, function(x) { sum(abs(x)) > 0 }) #At least one significant

##Compare anova with limma approaches
#compare = matrix(0, nrow = nrow(data), ncol = 3)
#rownames(compare) = rownames(data)
#colnames(compare) = c("anova", "limma", "limma-all")

#rows.anova = row.names(pvalues.fdr)[pvalues.fdr < 0.01]
#rows.limma = names(sig.nf[sig.nf])
#rows.limma.all = names(sig.all[sig.all])
#compare[rows.anova, "anova"] = 1
#compare[rows.limma, "limma"] = 1
#compare[rows.limma.all, "limma-all"] = 1
#vennDiagram(compare)
