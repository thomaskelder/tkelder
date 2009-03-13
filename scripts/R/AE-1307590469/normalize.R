library("affy");
library("affycomp");
library("affyPLM");
library("vsn");
library("affypdnn");
library("RColorBrewer");
library("simpleaffy");
library("affyQCReport");

rawdata<-ReadAffy(celfile.path="/home/thomas/projects/HBP_schotland/AE-1307590469/raw_data/AE-1307590469A");
data.bg.gcrma<-bg.correct.gcrma(rawdata);
data.bg.norm.gcrma.quantiles<-normalize.AffyBatch.quantiles(data.bg.gcrma);

eset.gcrma.q<-expresso(data.bg.norm.gcrma.quantiles, normalize=FALSE, bg.correct=FALSE, pmcorrect.method="pmonly", summary.method="medianpolish");

write.table(exprs(eset.gcrma.q), sep="\t", eol="\n", file="expr.txt", row.names=TRUE);
