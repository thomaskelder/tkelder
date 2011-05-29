############################################################
## Calculate overlap between KEGG and WikiPathways        ##
## As input, the pathways first need to be converted      ##
## to mapped xref lists using org.pct.util.PathwayOverlap ##
## See: http://code.google.com/p/tkelder/source/browse/   ##
##      trunk/pathway-interactions/README.txt             ##
############################################################

## Recursively lits files with a given extension ##
## from multiple paths.                          ##
listFiles = function(paths, ext) {
  dirs = file.info(paths)$isdir
	files = dir(paths[dirs], pattern=paste("\\.", ext, "$", sep=""), 
		recursive=T, full.names=T, ignore.case=T)
	files = append(files, paths[!dirs])
	files
}

## Read a set of pathways (xref lists, exported with  ##
## org.pct.util.PathwayOverlap).                      ##
readPathways = function(path) {
  files = listFiles(path, "txt")
	pathways = lapply(files, function(f) {
		read.delim(f, as.is=T, header=F)
	})
	names = as.character(lapply(pathways, function(p) { p[1,1] }))
	pathways = lapply(pathways, function(p) { p[3:nrow(p),1] })
	names(pathways) = names
	pathways
}

kegg = readPathways("kegg_mapped")
wikipathways = readPathways("wikipathways_mapped")

## Check coverage of KEGG pathways in WikiPathways
overlap = t(sapply(kegg, function(pk) {
  sapply(wikipathways, function(pw) {
    sum(pk %in% pw) / length(pk)
  })
}))

keggMatch = t(sapply(names(kegg), function(nk) {
  sk = length(kegg[[nk]])
  closest = max(overlap[nk,])
  if(closest == 0) {
    nw = sw = NA
  } else {
    nw = names(which(overlap[nk,] == closest))[1]
    sw = length(wikipathways[[nw]])
  }
  c(nk, nw, closest, sk, sw)
}))
colnames(keggMatch) = c(
    "KEGG", "Closest WikiPathways match", 
    "overlap", "KEGG size", "WikiPathways size"
)
keggMatch = as.data.frame(keggMatch, stringsAsFactors=F)
keggMatch$overlap = as.numeric(keggMatch$overlap)
keggMatch[,"KEGG size"] = as.numeric(keggMatch[,"KEGG size"])
keggMatch[,"WikiPathways size"] = as.numeric(keggMatch[, "WikiPathways size"])
keggMatch = keggMatch[order(keggMatch[,"overlap"], decreasing=T),]

write.table(keggMatch, file = "kegg_matches.txt", quote = F, sep = "\t", row.names=F)

hist(keggMatch$overlap, breaks=30)

## Calculate total overlap
xKegg = unique(unlist(kegg))
xWikipathways = unique(unlist(wikipathways))
overlap = intersect(xKegg, xWikipathways)

message("Nr xrefs in Kegg: ", length(xKegg))
message("Nr xrefs in WikiPathways: ", length(xWikipathways))
message("Nr xrefs matching: ", length(overlap))