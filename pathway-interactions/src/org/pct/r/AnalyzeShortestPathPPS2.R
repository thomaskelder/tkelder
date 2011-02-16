############################################################################
## Several commands used to analyze the pps2 pathway intractions analysis ##
## and generate the networks, figures and stats for the manuscript.       ##
##                                                                        ##
## This script runs directly on the output of the scripts                 ##
## ShortestPathFirstRun.R and ShortestPathSecondRun.R                     ##
############################################################################

strength = T
max.length = 0.9
neighborhood = 5

loadScripts = function() {
	src = "scripts/"
	source(paste(src, "ShortestPath.R", sep=""))
	source(paste(src, "PathwaySampler.R", sep=""))
	source(paste(src, "PPS2Functions.R", sep=""))
}
loadScripts()

time.lf = getTestNames(tzero.hf = F, tzero.lf = T, hflf = F)
time.hf = getTestNames(tzero.hf = T, tzero.lf = F, hflf = F)
hflf = getTestNames(tzero.hf = F, tzero.lf = F, hflf = T)
all = getTestNames()
zero.time = all[c(1,5,6,7,8,9,10)]

dataFile = "sps.data.RData"
load(dataFile)

## Load label data ##
#Pathway labels
pathwayTitles = readPathwayTitles("input/pathways_merged")
#Xref labels
library(org.Mm.eg.db)
ens2eg = as.list(org.Mm.egENSEMBL2EG)
eg2sym = as.list(org.Mm.egSYMBOL)
symbols = lapply(ens2eg, function(e) eg2sym[[e[1]]])
names(symbols) = paste("EnMm:", names(symbols), sep="")

##Gene set tests ##
library(doMC)
registerDoMC()
gst = ppsGeneSetTests(pathways, mixed=T, ranks.only=F)

writeGraphAttr = function(v, a, f) {
	v = paste(names(v), v, sep=" = ")
	v = c(a, v)
	write.table(v, f, row.names=F, col.names=F, sep="\t", quote=F)
}

gst.urls.hflf = getGstChartUrls(gst, hflf)
writeGraphAttr(gst.urls.hflf, "gstgraph.hflf", "output/pps2.gst.graphs.hflf.txt")
gst.urls.lf = getGstChartUrls(gst, time.lf)
writeGraphAttr(gst.urls.lf, "gstgraph.lf", "output/pps2.gst.graphs.lf.txt")
gst.urls.hf = getGstChartUrls(gst, time.hf)
writeGraphAttr(gst.urls.hf, "gstgraph.hf", "output/pps2.gst.graphs.hf.txt")

## Number of significant genes ##
rawData = read.delim("input/weights/pps2.2log.stats.mapped.txt", as.is=T)
testCols = colnames(rawData)[grep("_q", colnames(rawData))]
geneCounts = sapply(testCols, function(t) {
	sum(rawData[,t] < 0.05)
})
geneCounts.pct = sapply(testCols, function(t) {
	sum(rawData[,t] < 0.05) * 100 / nrow(rawData)
})

## Collect results and calculate p-values ##
doTests = function(t) {
	r = list() #Results for this task
	
	## Process 1st run
	f = paste("sp.pps2.", ifelse(strength, "gs", "ds"), ".", t, ".RData", sep="")
	load(f)
	
	r$scores = toScore(sps)
	r$scores.dist = lapply(spsdist, toScore)
	r$interactions = interactions

	pathCounts = each(sps, function(x) if(length(x) == 0 || is.na(x)) 0 else length(x))

	## Process 2nd run
	f = paste("2nd.sp.pps2.", ifelse(strength, "gs", "ds"), ".", t, ".RData", sep="")
	load(f)
	
	spsdist.num = lapply(spsdist, matrix.as.numeric)
	
	r$stats = scoreTests(r$scores, r$scores.dist, spsdist.num)
	
	r$graph = scoresAsGraph(
		r$scores, stats = r$stats, pathways = pathways, 
		pathwayTitles = pathwayTitles, pathCounts = pathCounts
	)
	if(!is.null(gst)) r$graph = addGeneSetTests(r$graph, gst)

	#Add unique id for edge (so attributes will not be overwritten
	#when loading multiple graphs in Cytoscape
	library(digest)
	code = digest(t, 'crc32')
	E(r$graph)$label = as.character(code)
	
	r
}

results.hf = foreach(t = time.hf) %dopar% doTests(t)
names(results.hf) = time.hf
results.lf = foreach(t = time.lf) %dopar% doTests(t)
names(results.lf) = time.lf
results.hflf = foreach(t = hflf) %dopar% doTests(t)
names(results.hflf) = hflf

results.all = append(results.hflf, results.lf)
results.all = append(results.all, results.hf)
intCounts = sapply(results.all, function(x) {
	g = x$graph
	sum(E(g)$permP < 0.001)
})

plot(geneCounts, intCounts)

for(t in names(results.all)) {
	f = paste("output/sp.pps2.", ifelse(strength, "gs", "ds"), ".", t, ".gml", sep="")
	g = results.all[[t]]$graph
	write.graph(g, f, format="gml")
	fixGml(f, graph = g)
}

#Number of nodes/edges in weighted interactions
#network resulting after filtering for weight <= max.length
ecounts = lapply(weights, function(w) {
	i = interactions
	V(i)$weight = w$sigmNodeWeights
	E(i)$weight = w$sigmEdgeWeights
	i = delete.edges(i, E(i)[weight > max.length])
	i = delete.vertices(i, which(degree(i) == 0) - 1)
	list(remaining.edges = ecount(i), remaining.nodes = vcount(i))
})
#Percentages
lapply(ecounts, function(c) 100 * c$remaining.edges / ecount(interactions))

#Overlap in weights <= max.length
overlapNodeWeights = function(labels, results) {
	wsv = matrix(NA, ncol = length(labels), nrow = vcount(results[[1]]$interactions))
	colnames(wsv) = labels
	for(t in labels) wsv[,t] = V(results[[t]]$interactions)$weight
	dev.new()
	vennDiagram(wsv <= max.length)
}
overlapNodeWeights(time.lf, results.lf)
overlapNodeWeights(time.hf, results.hf)

##Combine everything in a single graph
doMerge = function(results, out) {
	atts = list.vertex.attributes(results[[1]]$graph)
	merged = mergeGraphs(lapply(results, function(x) x$graph), overwrite = 
		c("name", "pathwayDegree", "nrXrefs", "title", atts[grep("^gst", atts)]))
	V(merged)$label = V(merged)$name
	
	write.graph(merged, out, format="gml")
	fixGml(out, graph = merged)
	
	merged
}
merged.hflf = doMerge(results.hflf, "output/sp.pps2.hflf.gml")
merged.lf = doMerge(results.lf, "output/sp.pps2.lf.gml")
merged.hf = doMerge(results.hf, "output/sp.pps2.hf.gml")
merged.resp = doMerge(append(results.lf, results.hf), "output/sp.pps2.response.gml")
##Create a graph containing only the edges that are significant in all
##conditions
filterAllSigEdges = function(g, minSig, out, p = 0.005) {
	fg = subGraphByAttr(g, high = p, att = 'permP', vertices = T)
	removeEdges = sapply(E(fg), function(e) {
		e = get.edge(fg, e)
		all = E(fg)[e[1] %--% e[2]]
		length(unique(all$sourceGraph)) < minSig
	})
	
	fg = delete.edges(fg, E(fg)[removeEdges])
	
	write.graph(fg, out, format="gml")
	fixGml(out,  graph = fg)
	
	fg
}
allsig.hflf = filterAllSigEdges(merged.hflf, length(hflf), "output/sp.pps2.hflf.allsig.gml", 0.005)
allsig.lf = filterAllSigEdges(merged.lf, length(time.lf), "output/sp.pps2.lf.allsig.gml", 0.005)
allsig.hf = filterAllSigEdges(merged.hf, length(time.hf), "output/sp.pps2.hf.allsig.gml", 0.005)

for(t in c("t0.6", "t2", "t48")) {
	t.hf = paste("HFt0vs", t, "_T", sep="")
	t.lf = paste("LFt0vs", t, "_T", sep="")
	m = doMerge(results.all[c(t.lf, t.hf)], paste("output/sp.pps2.t0vs", t, sep=""))
	filterAllSigEdges(m, 2, 
		paste("output/sp.pps2.", "t0vs", t, "allsig.gml", sep=""), 0.001)
}

#Write data graph attribute file
meanData = read.delim("input/weights/PPS2_filteredlist_2log.mean_sd_mapped.txt", as.is=T)
urls = getChartUrls(meanData)
urlAttrs = paste(names(urls), urls, sep=" = ")
urlAttrs = c("node.customGraphics1", urlAttrs)
write.table(urlAttrs, "output/pps2.data.graphs.txt", row.names=F, col.names=F, sep="\t", quote=F)

#Plot how degree of pathway nodes changes over time in HF vs LF
library(ggplot2)

mode = c("in", "out")
min.degree = 5

filterPathwayGraph = function(g, p = 0.001, pathCount = 0, vertices = F) {
	#Filter by interaction p-value
	fg = subGraphByAttr(g, high = p, att = 'permP', vertices = vertices)
	#Filter by minimum number of paths
	subGraphByAttr(fg, low = pathCount, att = 'nrPaths', vertices = vertices)
}

#For how many pathways could we find an interaction?
#For how many enriched pathways could we find an interaction?
nodeCounts = list()
gstCounts = list()
for(t in names(results.all)) {
	print(t)
	fg = filterPathwayGraph(results.all[[t]]$graph)
	d = degree(fg)
	names(d) = V(fg)$name
	
	f = sum(d > 0)
	a = vcount(fg)
	message(
		t, ", all: ", f, " / ", a, " (", round(100*f/a, 2), ")"
	)

	nodeCounts[[t]] = f
	
	gstsig = names(which(abs(unlist(gst[[t]])) < 0.05))
	f = sum(gstsig %in% names(which(d > 0)))
	a = length(gstsig)
	gstCounts[[t]] = a
	p = length(pathways)
	message(t, ", enr total: ", a, " / ", p, " (", round(100*a/p, 2), ")")
	message(
		t, ", enr: ", f, " / ", a, " (", round(100*f/a, 2), ")"
	)
}

library(ggplot2)
cols = c(1,5,6,7,8,9,10)
counts = cbind(
	test = all,
	sigGenes = geneCounts,
	enrPws = gstCounts,
	sigEdges = lapply(ecounts, function(x) x$remaining.edges),
	nodePws = nodeCounts
)[cols,]

#Write filtered graphs
#Write graph filtered by gst filter: target must be enriched
filteredGraphGst = function(g, t) {
	fg = filterPathwayGraph(g, vertices=T)
	
	fgfile = paste("output/filtered_", t, ".gml", sep="")
	write.graph(fg, fgfile, format="gml")
	fixGml(fgfile,  graph = fg)
	
	gstSig = names(which(abs(unlist(gst[[t]])) < 0.05))
	gstSig = V(fg)[name %in% gstSig]
	edges = E(fg)[to(gstSig)]
	fg = subgraphFromEdges(fg, edges)
	fgfile = paste("output/toGst0.05_", t, ".gml", sep="")
	write.graph(fg, fgfile, format="gml")
	fixGml(fgfile,  graph = fg)
	
	fg
}
togst.hflf = list()
for(t in hflf) filteredGraphGst(results.all[[t]]$graph, t)
togst.lf = list()
for(t in time.lf) filteredGraphGst(results.all[[t]]$graph, t)
togst.hf = list()
for(t in time.hf) filteredGraphGst(results.all[[t]]$graph, t)

trans = matrix(ncol=2, nrow=length(zero.time))
rownames(trans) = zero.time
colnames(trans) = c("global", "average")

for(t in zero.time) {
	fg = filterPathwayGraph(results.all[[t]]$graph, vertices=T)
	fg.rnd = fg
	for(i in 1:100) fg.rnd = rewire.edges(fg, 1)
	message(t)
	message("\tTransitivity (global): ", transitivity(fg, "global"))
	message("\tTransitivity (avg): ", transitivity(fg, "average"),
		" / ", transitivity(fg.rnd, "average"))
	message("\tAverage path length: ", average.path.length(fg, directed=F),
		" / ", average.path.length(fg.rnd, directed=F))
		
	trans[t, "global"] = transitivity(fg, "global")
	trans[t, "average"] = transitivity(fg, "average")
}

#Try to fit a power law
for(t in zero.time) {
	message(t)
	fg = filterPathwayGraph(results.all[[t]]$graph, vertices=T)
	din = degree(fg, mode="in")
	dout = degree(fg, mode="out")
	pfin = power.law.fit(din, xmin=1)
	pfout = power.law.fit(dout, xmin=1)
	message("in: ", round(coef(pfin), 2), " (", paste(round(confint(pfin), 2), collapse=', '), ")")
	message("out: ", round(coef(pfout), 2), " (", paste(round(confint(pfout), 2), collapse=', '), ")")
}

#Plot degree vs enrichment to look for correlation
par(mfrow=c(2, 4))
for(t in zero.time) {
	message(t)
	fg = filterPathwayGraph(results.all[[t]]$graph, vertices=T)
	
	enr = as.numeric(gst[[t]][V(fg)$name]) + 1E-5
	din = degree(fg, mode="in")
	dout = degree(fg, mode="out")
	plot(-log10(enr), din, ylim=range(c(din, dout)), main=t)
	points(-log10(enr), dout, col='red')
}

doHeatmap = function(values, file, text = values, ...) {
	hr = hclust(as.dist(1-cor(t(values), method="pearson")), method="average")
	#hr = hclust(dist(values), method="average")
	
	dg = as.dendrogram(hr)

	mycl = cutree(hr, h=max(hr$height)/2)
	mycol = sample(rainbow(max(mycl)))[as.vector(mycl)]

	cp = colorRampPalette(c('white', 'blue'))(128)

	if(!is.null(text)) {
		mynotes = text[order.dendrogram(dg),]
		colnames(mynotes) = names(values)
		if(is.numeric(mynotes)) mynotes = round(mynotes, 1)
		for(y in colnames(mynotes)) {
			for(x in rownames(mynotes)) {
				if(abs(gst[[y]][[x]]) < 0.05) mynotes[x,y] = paste(mynotes[x,y], '*', sep="")
			}
		}
		mynotes = t(mynotes)
		nts <<- mynotes
	} else {
		nts <<- NULL
	}
	addtext <<- function() {
		if(!is.null(nts)) {
			text(
				x = c(row(nts)), y = c(col(nts)), 
				labels=nts, col='black', cex=3
			)
		}
	}

	svg(file, 25,30)
	par(cex.main = 3, mar = c(0, 1, 0, 1))
	heatmap(
		values, Colv=NA, Rowv = dg, col=cp,
		RowSideColors = mycol, add.expr=addtext(), reorderfun=function(d,w) dg,
		cexCol = 4, cexRow = 4, margins=c(0,75), 
		scale = "none", labRow = pathwayTitles[rownames(values)],
		...
	)
	dev.off()
}

#Betweenness
doBtw = function(results, type, labels = names(results), min.btw=0) {
	btw = sapply(results, function(r) {
		g = r$graph
		fg = filterPathwayGraph(g)
		b = betweenness(fg, directed = T)
		names(b) = V(fg)$name
		#Normalize by dividing through the number of pairs of vertices not including the node we calculate the betweenness for
		b = b / ((vcount(fg)-1)*(vcount(fg)-2))
		b
	})
	btw = btw[rowSums(btw) > 0,] #Remove pathways that are always 0
	#Remove pathways with betweenness < threshold
	btw = btw[apply(btw,1,function(x) sum(x >= min.btw) > 0),]
	colnames(btw) = labels
	
#	inout = sapply(results, function(r) {
#		g = r$graph

#		fg = filterPathwayGraph(g)
#		din = degree(fg, mode = "in")
#		dout = degree(fg, mode = "out")
#		dinout = paste(din, dout, sep="/")
#		dinout = replace(dinout, dinout == "0/0", "")
#		names(dinout) = V(fg)$name
#		dinout[rownames(btw)]
#	})
	doHeatmap(btw, paste("output/images/btw.", type, ".hist.svg",sep=""),
		main="Betweenness centrality per pathway", text=round(btw*1E4, 2))
	btw
}

btw = doBtw(results.all[zero.time], "t0lfhf", min.btw=1E-3)
doBtw(results.hflf, "hflf",  min.btw=1E-3)

#Degree
doDegree = function(results, type, labels = names(results), min.degree=4) {
	mode = c("in", "out", "all")
	for(m in mode) {
		degrees = sapply(results, function(r) {
			g = r$graph

			fg = filterPathwayGraph(g)
			d = degree(fg, mode = m)
			names(d) = V(fg)$name
			d
		})
		degrees = degrees[rowSums(degrees) > 0,] #Remove pathways without connections
		#Remove pathways with only degree >= min.degree
		degrees = degrees[apply(degrees,1,function(x) sum(x >= min.degree) > 0),]

		degrees.c = degrees
		colnames(degrees.c) = labels
	
		inout = ifelse(m == "in", "incoming", "outgoing")
		inout = ifelse(m == "all", "", inout)
		file = paste("output/images/degree.", type, ".hist.", m, ".svg", sep="")
		doHeatmap(degrees.c, file, doNotes = T,
			main=paste("Number of ", inout, " interactions per pathway at\ndifferent time points"))
	}
}

doDegree(results.all[zero.time], "t0lfhf", min.degree=5)
doDegree(results.hflf, "hflf")

## Plot graphs for nodes with highest degrees ##
mode = c("in", "out")
for(t in hflf) {
	r = results.hflf[[t]]
	for(m in mode) {
		g = r$graph
		fg = filterPathwayGraph(g)
		d = degree(fg, mode=m)
		names(d) = V(fg)$name
		top = names(which(d >= min.degree))
		print(top)
		if(m == "out") tope = E(fg)[from(V(fg)[name %in% top])]
		else tope = E(fg)[to(V(fg)[name %in% top])]
		
		fg = subgraphFromEdges(fg, tope)
		fgfile = paste("output/topdegree_", t, "_", m, ".gml", sep="")
		write.graph(fg, fgfile, format="gml")
		fixGml(fgfile,  graph = fg)
	}
}

#Collect all interactions that play role in cross-talk network
igraphs = loadInteractionGraphs("input/interactions/mouse-directed")

allDetails = function(results) {
	details = list()
	for(t in names(results)) {
		r = results[[t]]
		include = r$stats$permP < 0.001
		allPaths = detailGraph(include = include, file = paste("output/sp.det.p0.001.", t, ".gml", sep=""), max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs = igraphs)
		allPaths = subGraphByAttr(allPaths, low = 1, att = 'nrPaths', vertices = T)

		#Get the xrefs
		pathXrefs = V(allPaths)[!(name %in% names(pathways))]
		pathXrefs.degree = degree(allPaths, pathXrefs)
		names(pathXrefs.degree) = symbols[pathXrefs$name]
	
		details[[t]] = list(
			allPaths = allPaths,
			pathXrefs = pathXrefs
		)
	}
	
	details
}

details.all = allDetails(results.all)

for(t in names(details.all)) {
	g = details.all[[t]]$allPaths
	file = paste("output/sp.det.p0.001.", t, ".gml", sep="")
	write.graph(g, file, format="gml")
	fixGml(file, graph = g)
}

mainTypes = list(
	list(m = "catalysis",
		alt = c()
	),
	list(m = "binding",
		alt = c()
	),
	list(m = "TF / expression",
		alt = c("TF", "expression")
	),
	list(m = "reaction / interaction",
		alt = c("reaction")
	)
)
types.all = NULL
for(t in zero.time) {
	det = details.all[[t]]
	counts = table(E(det$allPaths)$Interaction, exclude="")
	
	for(mt in mainTypes) {
		tocount = unlist(mt)
		cols = sapply(tocount, grep, names(counts))
		cols = unique(unlist(cols))
		r = c(t, mt$m, sum(counts[cols]))
		types.all = rbind(types.all, r)
	}
}
rownames(types.all) = NULL
colnames(types.all) = c("Time", "Interaction", "Count")
types.all = as.data.frame(types.all)
types.all$Count = as.numeric(types.all$Count)
types.all$Time = gsub("_T", "", types.all$Time)
types.all$Time = gsub("HFvsLF", "", types.all$Time)

library(ggplot2)
ggplot(types.all, aes(Count, x = Time, fill = Interaction)) +
	geom_bar() +
	facet_grid(.~Interaction) +
	opts(legend.position = "none", axis.text.x=theme_text(angle=-90, hjust=0))

for(t in all) {
	det = details.all[[t]]
	
	fc = gsub("_T", "", t)
	fc = gsub("LFt", "LF_t", fc)
	pathXrefs.fc = get.vertex.attribute(det$allPaths, paste('log2_fc_', fc, sep=""), det$pathXrefs)
	names(pathXrefs.fc) = names(det$pathXrefs.degree)
	pathXrefs.fc = pathXrefs.fc[order(det$pathXrefs.degree)]
		
	#Find out the foldchange distributions of these xrefs
	hist(det$pathXrefs.fc, breaks = 100)
	hist(abs(det$pathXrefs.fc), breaks = 100)
}

# Statistics on all paths
#Count all protein interactions
lapply(details.all[zero.time], function(x) {
	g = x$allPaths
	pws = V(g)[name %in% names(pathways)]
	g = delete.vertices(g, pws)
	ecount(g)
})
#Count all proteins in paths
lapply(details.all[zero.time], function(x) length(x$pathXrefs)) 
#Count all proteins in paths, not in any pathway
notInPw = lapply(details.all[zero.time], function(x) {
	g = x$allPaths
	pws = V(g)[name %in% names(pathways)]
	pwXrefs = V(g)[nei(pws)]
	g = delete.vertices(g, c(pws, pwXrefs))
	V(g)$name
})
lapply(notInPw, length)

sgk3 = "EnMm:ENSMUSG00000025915"
lapply(notInPw, function(gs) sgk3 %in% gs)

#Detailed paths
details.all.paths = list()
for(t in names(results.all)) {
	r = results.all[[t]]
	include = r$stats$permP < 0.001
	
	sources = names(pathways)[rowSums(include) > 0]
	targets = names(pathways)[colSums(include) > 0]

	pwx = unique(unlist(pathways))
	pws = pathways[union(sources,targets)]
	sps = shortestPaths(r$interactions, pws, value.only=F, pathwayXrefs = pwx, exclude = !include, max.length = max.length, neighborhood = neighborhood)
	
	pathinfo = list()
	for(src in sources) {
		for(tgt in targets) {
			if(!include[[src, tgt]]) next;
			if(is.na(sps[[src,tgt]])) next
			if(length(sps[[src,tgt]]$paths) == 0) next
	
			## Extract the paths
			g = sps[[src, tgt]]$graph
			vpaths = lapply(sps[[src, tgt]]$paths, function(x) V(g)[x]$name)
			pathinfo = append(pathinfo, vpaths)
		}
	}
	details.all.paths[[t]] = pathinfo
}

#Average path length
library(ggplot2)
pathLengthDist = lapply(details.all.paths[zero.time], function(paths) {
	table(as.numeric(lapply(paths, length)) - 3)
})
pathLengthDist = as.data.frame(melt(pathLengthDist))
colnames(pathLengthDist) = c("InteractionsInPath", "NrPaths", "Test")

pathLengthDist$TestType = "Response in HF"
pathLengthDist$TestType[grep("LFt0", pathLengthDist$Test)] = "Response in LF"
pathLengthDist$TestType[grep("HFvsLF", pathLengthDist$Test)] = "HF vs LF"

pathLengthDist$Test = gsub("HFvsLF", "", pathLengthDist$Test)
pathLengthDist$Test = gsub("LFt0vs", "", pathLengthDist$Test)
pathLengthDist$Test = gsub("HFt0vs", "", pathLengthDist$Test)
pathLengthDist$Test = gsub("_T", "", pathLengthDist$Test)

ggplot(pathLengthDist, aes(Test, y = NrPaths, fill = as.factor(InteractionsInPath))) + 
	geom_bar() +
	facet_wrap(~TestType, scales="free_x", ncol=3) +
	opts(axis.text.x=theme_text(angle=-90, hjust=0), title = "Number of paths per pathway interaction network\nfor all significant pathway interactions.") +
	labs(x = "", y = "Number of paths", fill = "Interactions in path")
ggsave("output/images/nrpaths.svg", scale = 0.8)

#Find proteins through which most paths cross
proteinNrPaths = list() #Total number of paths through protein (including multiple within single pathway interaction)
proteinInInt = list() #Total number of pathway interactions protein is involved in

for(t in names(details.all.paths[zero.time])) {
	proteinNrPaths[[t]] = list()
	proteinInInt[[t]] = list()
	for(path in details.all.paths[[t]]) {
		if(length(path) > 3) {
			proteins = path[2:(length(path) - 1)]
			pwStart = path[1]
			pwEnd = path[length(path)]
			for(p in proteins) {
				if(p %in% names(proteinNrPaths[[t]])) proteinNrPaths[[t]][[p]] = proteinNrPaths[[t]][[p]] + 1
				else proteinNrPaths[[t]][[p]] = 0
				
				if(!(p %in% names(proteinInInt[[t]]))) proteinInInt[[t]][[p]] = c()
				proteinInInt[[t]][[p]] = union(proteinInInt[[t]][[p]], paste(pwStart, pwEnd, sep=" -> "))
			}
		}
	}
}
proteinNrPaths = lapply(proteinNrPaths, unlist)
proteinNrPaths.sym = lapply(proteinNrPaths, function(p) { names(p) = symbols[names(p)]; p })
lapply(proteinNrPaths.sym, function(p) sort(p,decreasing=T)[1:10])

#Create table of proteins with most paths and in which pathway interactions they play a role
proteinInInt.sym = lapply(proteinInInt, function(p) { names(p) = symbols[names(p)]; p })
top = lapply(proteinInInt.sym, function(p) sort(sapply(p, length),decreasing=T)[1:10])

top.pws = list()
for(t in names(proteinInInt.sym)) {
	top.pws[[t]] = lapply(proteinInInt.sym[[t]][names(top[[t]])], function(inpws) {
		if(is.null(inpws)) return(c())
		t = table(unlist(strsplit(inpws, " -> ")))
		t = sort(t, decreasing=T)
		names(t) = pathwayTitles[names(t)]
		t
	})
}

cm = rep("", 3)
for(t in names(top)) {
	cm = rbind(cm, c(t, "Nr. interactions", "Involved in interactions with pathways"))
	prots = top[[t]]
	
	pws.str = sapply(top.pws[[t]], function(pws) {
		pws.incl = pws[pws > 1]
		pws.incl = pws.incl[1:min(3,length(pws.incl))]
		print(pws.incl)
		more = length(pws) - length(pws.incl)	
	
		pws.txt = paste(names(pws.incl), " (", pws.incl, ")", sep="")
		pws.txt = paste(pws.txt, collapse=", ")
		if(more > 0) pws.txt = paste(pws.txt, ", ", more, " more.", sep="")
		pws.txt
	})
	cm = rbind(cm, cbind(names(prots), as.numeric(prots), pws.str))
}

write.table(cm, "output/protein.top.nrpath.txt", sep="\t", row.names=F, col.names=F, quote=F)

#For proteins that are in path but not in pathway, find out in how many
#paths they are active and which pathways they connect
notInPwInInt = list()
for(t in names(notInPw)) {
	notInPwInInt[[t]] = lapply(notInPw[[t]], function(g) {
		proteinInInt[[t]][[g]]
	})
}

notInPwGraphs = list()
for(t in names(details.all.paths[zero.time])) {
	g = details.all[[t]]$allPaths
	V(g)$indirect = 0
	V(g)$include = 0
	E(g)$indirect = 0
	E(g)$inPathway = 0
	for(p in details.all.paths[[t]]) {
		if(length(p) > 4) {
			vp = rep(-1, length(p))
			for(i in 1:length(p)) vp[i] = V(g)[name == p[i]]
			E(g, path=vp)$indirect = 1
			V(g)[name %in% p]$include = 1
			V(g)[name %in% unique(p[3:(length(p) - 2)])]$indirect = 1
			
			ispw = V(g)[name %in% names(pathways)]
			E(g)[adj(ispw)]$inPathway = 1
		}
	}
	if(sum(V(g)$include) > 0) {
		g = subgraph(g, which(V(g)$include == 1) - 1)
		notInPwGraphs[[t]] = g
		
		f = paste("output/indirect.", t, ".gml", sep="")
		write.graph(g, f, format="gml")
		fixGml(f,  graph = g)
	}
}

#Find interactions that contribute to paths but are
#not in any pathway (indirect)
inPathNoPathway = list()
for(t in names(details.all.paths)) {
	for(path in details.all.paths[[t]]) {
		if(length(path) > 4) {
			proteins = path[3:(length(path) - 2)]
			pwStart = path[1]
			pwEnd = path[length(path)]
			for(p in proteins) {
				info = list()
				info$start = c()
				info$end = c()
				info$times = 0
				if(p %in% names(inPathNoPathway)) {
					info = inPathNoPathway[[p]]
				}
				info$times = info$times + 1
				info$start = union(info$start, pwStart)
				info$end = union(info$end, pwEnd)
				
				inPathNoPathway[[t]][[p]] = info
			}
		}
	}
}
lapply(inPathNoPathway, length)


## Find cliques ##
cliques = list()
for(t in all) {
	r = results.all[[t]]
	fg = filterPathwayGraph(r$graph)
	cl = lapply(largest.cliques(fg), function(c) {
		V(fg)[c]$name
	})
	cl = cl[which(lapply(cl, length) > 2)]
	
	#Save graph with the clique nodes
	fg = subgraph(fg, V(fg)[name %in% unlist(cl)])
	
	f = paste("output/sp.clique.", t, ".gml", sep="")
	write.graph(fg, f, format="gml")
	fixGml(f,  graph = fg)
	
	cliques[[t]] = cl
}

### Detail graphs ###

## Clique at t = 0
t = hflf[[1]]
r = results.all[[t]]
pws = cliques[[t]][[1]]
include = r$stats$permP
include[,] = 0
include[pws, pws] = r$stats$permP[pws,pws] < 0.001

dg = detailGraph(include = include > 0, file = paste("output/sp.det.clique.", t, ".gml", sep=""), max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T)

# Highest betweenness
for(t in colnames(btw)) {
	pw = rownames(btw)[which(btw[,t] == max(btw[,t]))]
	
	r = results.all[[t]]
	include = r$stats$permP
	include[,] = 0
	include[pw,] = r$stats$permP[pw,] < 0.001
	include[,pw] = r$stats$permP[,pw] < 0.001
	dg = detailGraph(include = include > 0, file = paste("output/sp.det.btw.", t, ".", pw, ".gml", sep=""), max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T)
}


## Stress response pathways for HFvsLF t0
t = "HFvsLFt0_T"
r = results.all[[t]]
fg = filterPathwayGraph(r$graph)
tgtPw = c(
	"Mm_FAS_pathway_and_Stress_induction_of_HSP_regulation_WP571_38241.gpml",
	"mmu_Apoptosis.gpml|Mm_Apoptosis_Modulation_by_HSP70_WP166_32716.gpml",
	"Mm_Oxidative_Damage_WP1496_36318.gpml|Mm_Apoptosis_WP1254_35103.gpml"
)
srcPw = c(
"mmu_Gap junction.gpml",
"mmu_Fc epsilon RI signaling pathway.gpml",
"mmu_Long-term potentiation.gpml",
"mmu_Progesterone-mediated oocyte maturation.gpml",
"mmu_Chemokine signaling pathway.gpml",
"mmu_Dorso-ventral axis formation.gpml",
"mmu_B cell receptor signaling pathway.gpml",
"mmu_VEGF signaling pathway.gpml",
"Mm_Wnt_Signaling_Pathway_and_Pluripotency_WP723_34411.gpml|Mm_Wnt_Signaling_Pathway_WP403_35124.gpml",
"Mm_Wnt_Signaling_Pathway_NetPath_WP539_34371.gpml",
"mmu_Long-term depression.gpml",
"mmu_Melanogenesis.gpml",
"mmu_Natural killer cell mediated cytotoxicity.gpml",
"mmu_T cell receptor signaling pathway.gpml"
)
dg = detailGraph(srcPw, tgtPw, "output/sp.det.stress.sub.HFvsLF.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

tgtPw = "Mm_ESC_Pluripotency_Pathways_WP339_34812.gpml"
srcPw = "mmu_Proteasome.gpml|Mm_Proteasome_Degradation_WP519_33047.gpml"
dg = detailGraph(srcPw, tgtPw, "output/sp.det.prot.esc.HFvsLF.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, deletePaths = F, igraphs=igraphs)

srcPw = "Mm_ESC_Pluripotency_Pathways_WP339_34812.gpml"
tgtPw = c(
"mmu_B cell receptor signaling pathway.gpml",
"mmu_Fc epsilon RI signaling pathway.gpml",
"mmu_VEGF signaling pathway.gpml",
"mmu_Long-term potentiation.gpml",
"mmu_Chemokine signaling pathway.gpml",
"mmu_Progesterone-mediated oocyte maturation.gpml",
"mmu_Natural killer cell mediated cytotoxicity.gpml",
"mmu_T cell receptor signaling pathway.gpml"
)

dg = detailGraph(srcPw, tgtPw, "output/sp.det.esc.src.HFvsLF.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

srcPw = c(
"mmu_Progesterone-mediated oocyte maturation.gpml",
"mmu_Chemokine signaling pathway.gpml",
"mmu_Long-term potentiation.gpml",
"mmu_Fc epsilon RI signaling pathway.gpml",
"mmu_T cell receptor signaling pathway.gpml",
"mmu_B cell receptor signaling pathway.gpml",
"mmu_Natural killer cell mediated cytotoxicity.gpml",
"mmu_VEGF signaling pathway.gpml",
"Mm_Wnt_Signaling_Pathway_NetPath_WP539_34371.gpml"
"Mm_Wnt_Signaling_Pathway_and_Pluripotency_WP723_34411.gpml|Mm_Wnt_Signaling_Pathway_WP403_35124.gpml"
)
tgtPw = c(
"Mm_FAS_pathway_and_Stress_induction_of_HSP_regulation_WP571_38241.gpml",
"mmu_Apoptosis.gpml|Mm_Apoptosis_Modulation_by_HSP70_WP166_32716.gpml",
"Mm_Oxidative_Damage_WP1496_36318.gpml|Mm_Apoptosis_WP1254_35103.gpml"
)

dg = detailGraph(srcPw, tgtPw, "output/sp.det.stress.esc.HFvsLF.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

srcPw = c(
"Mm_TNF-alpha_NF-kB_Signaling_Pathway_WP246_34428.gpml"
)
tgtPw = c(
"Mm_Wnt_Signaling_Pathway_and_Pluripotency_WP723_34411.gpml|Mm_Wnt_Signaling_Pathway_WP403_35124.gpml",
"Mm_Wnt_Signaling_Pathway_NetPath_WP539_34371.gpml"
)
dg = detailGraph(srcPw, tgtPw, "output/sp.det.tnfa.wnt.HFvsLF.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

tgtPw = "Mm_Insulin_Signaling_WP65_33395.gpml"
srcPw = "mmu_Axon guidance.gpml"
dg = detailGraph(srcPw, tgtPw, "output/sp.det.ax.ins.HFvsLF.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

# Bile acid for HF vs LF t=0
srcPw = c("mmu_Primary bile acid biosynthesis.gpml", "mmu_Peroxisome.gpml")
tgtPw = V(fg)[nei(V(fg)[name %in% srcPw])]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.bile.HFvsLF.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

# LF response, t=0.6
t = "LFt0vst0.6_T"
r = results.all[[t]]
fg = filterPathwayGraph(r$graph)
srcPw = c(
"Mm_FAS_pathway_and_Stress_induction_of_HSP_regulation_WP571_38241.gpml",
"Mm_Selenium_metabolism-Selenoproteins_WP108_28130.gpml"
)
tgtPw = V(fg)[nei(V(fg)[name %in% srcPw])]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.fas.sel.LFt0.6.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

srcPw = c(
"Mm_Oxidative_Stress_WP412_33386.gpml"
)
tgtPw = V(fg)[nei(V(fg)[name %in% srcPw])]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.ox.LFt0.6.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

srcPw = c(
"mmu_Toll-like receptor signaling pathway.gpml|Mm_NLR_proteins_WP1256_34370.gpml"
)
tgtPw = V(fg)[nei(V(fg)[name %in% srcPw], mode="out")]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.toll.LFt0.6.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

fos = "EnMm:ENSMUSG00000021250"
jun = "EnMm:ENSMUSG00000052684"
fos.n = V(g)[nei(V(g)[name == fos])]$name
jun.n = V(g)[nei(V(g)[name == jun])]$name
fos.n.q = as.numeric(sapply(fos.n, function(x) rawData[rawData[,'mapped_xref'] == x, 'LFt0vst0.6_q']))
names(fos.n.q) = symbols[fos.n]
jun.n.q = as.numeric(sapply(jun.n, function(x) rawData[rawData[,'mapped_xref'] == x, 'LFt0vst0.6_q']))
names(jun.n.q) = symbols[jun.n]

# LF response, t=48, p < 0.005
srcPw = "Mm_IL-6_signaling_Pathway_WP387_34384.gpml"
tgtPw = "Mm_Leptin_Insulin_Overlap_WP578_39408.gpml|mmu_Insulin signaling pathway.gpml"
t = "LFt0vst48_T"
r = results.all[[t]]
dg = detailGraph(srcPw, tgtPw, "output/sp.il6.ins.LFt48.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

#HF response t=0.6
t = "HFt0vst0.6_T"
r = results.all[[t]]
fg = filterPathwayGraph(r$graph)
srcPw = c(
"Mm_FAS_pathway_and_Stress_induction_of_HSP_regulation_WP571_38241.gpml",
"Mm_Selenium_metabolism-Selenoproteins_WP108_28130.gpml"
)
tgtPw = V(fg)[nei(V(fg)[name %in% srcPw])]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.fas.sel.HFt0.6.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

srcPw = "Mm_Hypertrophy_Model_WP202_32968.gpml"
tgtPw = V(fg)[nei(V(fg)[name %in% srcPw])]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.hyp.HFt0.6.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

srcPw = "Mm_Amino_Acid_metabolism_-_Sokolovic_WP662_32748.gpml"
tgtPw = V(fg)[nei(V(fg)[name %in% srcPw])]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.aa.HFt0.6.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

include = r$stats$permP
include[,] = 0
nr = "Mm_Nuclear_Receptors_WP509_35767.gpml"
s1p = "Mm_Signal_Transduction_of_S1P_Receptor_WP57_33389.gpml"
ins = "Mm_Insulin_Signaling_WP65_33395.gpml"
rib = "Mm_Cytoplasmic_Ribosomal_Proteins_WP163_34552.gpml|mmu_Ribosome.gpml"
include[nr,ins] = 1
include[nr,rib] = 1
include[s1p,nr] = 1
dg = detailGraph(include = include > 0, file="output/sp.det.s1p.nr.HFt0.6.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

tgtPw = V(fg)[nei(V(fg)[name %in% srcPw])]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.s1p.HFt0.6.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

tgtPw = "mmu_Lysosome.gpml"
srcPw = V(fg)[nei(V(fg)[name %in% tgtPw])]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.lys.HFt0.6.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

srcPw = "mmu_Basal transcription factors.gpml"
tgtPw = V(fg)[nei(V(fg)[name %in% srcPw])]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.btf.HFt0.6.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

hsp72 = "EnMm:ENSMUSG00000067283"
i = results.all[[t]]$interactions
nb = V(i)[nei(V(i)[name == hsp72])]
nbw = nb$weight
names(nbw) = symbols[nb$name]
sort(nbw)

#LF response t=2
t = "LFt0vst2_T"
r = results.all[[t]]
fg = filterPathwayGraph(r$graph)
srcPw = c(
	"Mm_Signal_Transduction_of_S1P_Receptor_WP57_33389.gpml",
	"mmu_mTOR signaling pathway.gpml"
)
tgtPw = V(fg)[nei(V(fg)[name %in% srcPw])]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.s1p.mtor.LFt2.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

srcPw = "Mm_EGFR1_Signaling_Pathway_WP572_35717.gpml"
tgtPw = V(fg)[nei(V(fg)[name %in% srcPw], mode="out")]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.egfr.LFt2.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

srcPw = "Mm_Senescence_and_Autophagy_WP1267_34383.gpml"
tgtPw = V(fg)[nei(V(fg)[name %in% srcPw], mode="out")]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.sene.LFt2.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

srcPw = "mmu_Vasopressin-regulated water reabsorption.gpml"
tgtPw = V(fg)[nei(V(fg)[name %in% srcPw], mode="out")]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.vaso.LFt2.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

tgtPw = "Mm_Cytoplasmic_Ribosomal_Proteins_WP163_34552.gpml|mmu_Ribosome.gpml"
srcPw = V(fg)[nei(V(fg)[name %in% tgtPw])]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.rib.LFt2.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

srcPw = "Mm_Selenium_metabolism-Selenoproteins_WP108_28130.gpml"
tgtPw = "Mm_Oxidative_Stress_WP412_33386.gpml"
dg = detailGraph(srcPw, tgtPw, "output/sp.det.sel.LFt2.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

include = r$stats$permP
include[,] = 0
ss = "mmu_Spliceosome.gpml"
ak = "mmu_Adipocytokine signaling pathway.gpml|Mm_Leptin_and_adiponectin_WP683_33406.gpml"
rl = "mmu_RIG-I-like receptor signaling pathway.gpml"
ly = "mmu_Lysosome.gpml"
sn = "mmu_SNARE interactions in vesicular transport.gpml"
ag = "mmu_Antigen processing and presentation.gpml"
ec = "mmu_Endocytosis.gpml"
pe = "mmu_Protein export.gpml"
include[ss,c(ly, ak, pe, sn, rl)] = 1
include[pe, ec] = 1
include[sn, ag] = 1
dg = detailGraph(include = include > 0, file="output/sp.det.sp.LFt2.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

include = r$stats$permP
include[,] = 0
s1p = "Mm_Signal_Transduction_of_S1P_Receptor_WP57_33389.gpml"
mtor = "mmu_mTOR signaling pathway.gpml"
arg = "mmu_Arginine and proline metabolism.gpml|Mm_Urea_cycle_and_metabolism_of_amino_groups_WP426_21578.gpml"
erb = "Mm_ErbB_signaling_pathway_WP1261_32870.gpml|mmu_ErbB signaling pathway.gpml"
rib = "Mm_Cytoplasmic_Ribosomal_Proteins_WP163_34552.gpml|mmu_Ribosome.gpml"
ppar = "mmu_PPAR signaling pathway.gpml"
sene = "Mm_Senescence_and_Autophagy_WP1267_34383.gpml"
include[s1p, c(mtor, rib, erb)] = 1
include[mtor, arg] = 1
include[erb, rib] = 1
include[sene, ppar] = 1
dg = detailGraph(include = include > 0, file="output/sp.det.s1p.ppar.LFt2.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

#HF response t=2
t = "HFt0vst2_T"
r = results.all[[t]]
fg = filterPathwayGraph(r$graph)

cy = "mmu_Cytokine-cytokine receptor interaction.gpml|Mm_Cytokines_and_Inflammatory_Response_(BioCarta)_WP222_38654.gpml"
en = "mmu_Endocytosis.gpml"
do = "mmu_Dorso-ventral axis formation.gpml"
cy.nb = V(fg)[nei(V(fg)[name == cy])]$name
en.nb = V(fg)[nei(V(fg)[name == en])]$name
do.nb = V(fg)[nei(V(fg)[name == do])]$name
library(gplots)
venn(list(cy = cy.nb, en = en.nb, do = do.nb))
unb = unique(c(cy.nb, en.nb, do.nb))
overlap = matrix(F, ncol = 3, nrow = length(unb))
dimnames(overlap) = list(unb, c("cytokine", "endocytosis", "dorso"))
overlap[cy.nb, "cytokine"] = T
overlap[en.nb, "endocytosis"] = T
overlap[do.nb, "dorso"] = T
which(rowSums(overlap) == 3)

tgtPw = V(fg)[nei(V(fg)[name == cy], mode="out")]$name
srcPw = V(fg)[nei(V(fg)[name == cy], mode="in")]$name
include = r$stats$permP
include[,] = 0
include[cy,tgtPw] = 1
include[srcPw,cy] = 1
dg = detailGraph(include = include > 0, file="output/sp.det.cy.HFt2.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

tgtPw = V(fg)[nei(V(fg)[name == en], mode="out")]$name
srcPw = V(fg)[nei(V(fg)[name == en], mode="in")]$name
include = r$stats$permP
include[,] = 0
include[en,tgtPw] = 1
include[srcPw,en] = 1
dg = detailGraph(include = include > 0, file="output/sp.det.en.HFt2.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

tgtPw = V(fg)[nei(V(fg)[name == en], mode="out")]$name
srcPw = V(fg)[nei(V(fg)[name == en], mode="in")]$name
include = r$stats$permP
include[,] = 0
include[cy,tgtPw] = 1
include[srcPw,cy] = 1
include[en,tgtPw] = 1
include[srcPw,en] = 1
dg = detailGraph(include = include > 0, file="output/sp.det.cy.en.HFt2.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

tgtPw = V(fg)[nei(V(fg)[name == do], mode="out")]$name
srcPw = V(fg)[nei(V(fg)[name == do], mode="in")]$name
include = r$stats$permP
include[,] = 0
include[do,tgtPw] = 1
include[srcPw,do] = 1
dg = detailGraph(include = include > 0, file="output/sp.det.do.HFt2.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

srcPw = "mmu_Aldosterone-regulated sodium reabsorption.gpml"
tgtPw = V(fg)[nei(V(fg)[name %in% srcPw], mode="out")]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.ald.HFt2.r.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs, deletePaths = F)

srcPw = "Mm_ESC_Pluripotency_Pathways_WP339_34812.gpml"
tgtPw = V(fg)[nei(V(fg)[name %in% srcPw], mode="out")]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.esc.HFt2.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

srcPw = "mmu_Proteasome.gpml|Mm_Proteasome_Degradation_WP519_33047.gpml"
tgtPw = V(fg)[nei(V(fg)[name %in% srcPw], mode="out")]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.prot.HFt2.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

srcPw = "Mm_ESC_Pluripotency_Pathways_WP339_34812.gpml"
tgtPw = V(fg)[nei(V(fg)[name %in% srcPw], mode="out")]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.esc.HFt2.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

srcPw = "Mm_Insulin_Signaling_WP65_33395.gpml"
tgtPw = "mmu_Cytokine-cytokine receptor interaction.gpml|Mm_Cytokines_and_Inflammatory_Response_(BioCarta)_WP222_38654.gpml"
dg = detailGraph(srcPw, tgtPw, "output/sp.det.ins.cy.HFt2.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs, deletePaths = T)

#LF response t=48
t = "LFt0vst48_T"
r = results.all[[t]]
fg = filterPathwayGraph(r$graph)

tgfb = "Mm_TGF_Beta_Signaling_Pathway_WP113_31754.gpml"
s1p = "Mm_Signal_Transduction_of_S1P_Receptor_WP57_33389.gpml"
gly = "Mm_Glycogen_Metabolism_WP317_35726.gpml"
wnt = "mmu_Wnt signaling pathway.gpml"
include = r$stats$permP
include[,] = 0
include[c(tgfb, wnt), s1p] = 1
include[s1p, gly] = 1
dg = detailGraph(include = include > 0, file = "output/sp.det.s1p.LFt48.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

il6 = "Mm_IL-6_signaling_Pathway_WP387_34384.gpml"
wnt = "mmu_Wnt signaling pathway.gpml"
ins = "Mm_Insulin_Signaling_WP65_33395.gpml"
glu = "Mm_Glucuronidation_WP1241_34382.gpml|mmu_Starch and sucrose metabolism.gpml"
lep = "Mm_Leptin_Insulin_Overlap_WP578_39408.gpml|mmu_Insulin signaling pathway.gpml"
include = r$stats$permP
include[,] = 0
include[il6,c(glu,ins)] = 1
include[wnt,c(ins,lep)] = 1
dg = detailGraph(include = include > 0, file = "output/sp.det.ins.LFt48.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs, uniqueId = t)

srcPw = "mmu_Antigen processing and presentation.gpml"
include = r$stats$permP
include[,] = 0
include[srcPw, V(fg)[nei(V(fg)[name == srcPw], mode="out")]$name] = 1
include[V(fg)[nei(V(fg)[name == srcPw], mode="in")]$name, srcPw] = 1
tgtPw = "Mm_T_Cell_Receptor_Signaling_Pathway_WP480_34406.gpml"
include[V(fg)[nei(V(fg)[name %in% tgtPw], mode="in")]$name, tgtPw] = 1
dg = detailGraph(include = include > 0, file = "output/sp.det.agp.tc.LFt48.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

agp = "mmu_Antigen processing and presentation.gpml"
tc = "Mm_T_Cell_Receptor_Signaling_Pathway_WP480_34406.gpml"
tnfa = "Mm_TNF-alpha_NF-kB_Signaling_Pathway_WP246_34428.gpml"
cc = "mmu_Cell cycle.gpml|Mm_G1_to_S_cell_cycle_control_WP413_35821.gpml|Mm_Cell_cycle_WP190_35363.gpml"
il3 = "Mm_IL-3_Signaling_Pathway_WP373_35789.gpml"
include = r$stats$permP
include[,] = 0
include[cc, agp] = 1
include[tnfa, tc] = 1
include[
dg = detailGraph(include = include > 0, file = "output/sp.det.agp.tc.cc.tnfa.LFt48.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

tgtPw = "Mm_IL-3_Signaling_Pathway_WP373_35789.gpml"
srcPw = V(fg)[nei(V(fg)[name == srcPw], mode="in")]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.il3.LFt48.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

srcPw = "Mm_TNF-alpha_NF-kB_Signaling_Pathway_WP246_34428.gpml"
include = r$stats$permP
include[,] = 0
include[srcPw, V(fg)[nei(V(fg)[name == srcPw], mode="out")]$name] = 1
include[V(fg)[nei(V(fg)[name == srcPw], mode="in")]$name, srcPw] = 1
dg = detailGraph(include = include > 0, file = "output/sp.det.tnfa.LFt48.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

srcPw = "mmu_Cell cycle.gpml|Mm_G1_to_S_cell_cycle_control_WP413_35821.gpml|Mm_Cell_cycle_WP190_35363.gpml"
include = r$stats$permP
include[,] = 0
include[srcPw, V(fg)[nei(V(fg)[name == srcPw], mode="out")]$name] = 1
include[V(fg)[nei(V(fg)[name == srcPw], mode="in")]$name, srcPw] = 1
dg = detailGraph(include = include > 0, file = "output/sp.det.cc.LFt48.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

tgtPw = "Mm_GPCRs,_Other_WP41_34577.gpml"
srcPw = V(fg)[nei(V(fg)[name %in% tgtPw], mode="in")]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.gpcr.LFt48.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

srcPw = "Mm_TNF-alpha_NF-kB_Signaling_Pathway_WP246_34428.gpml"
tgtPw = "Mm_T_Cell_Receptor_Signaling_Pathway_WP480_34406.gpml"
dg = detailGraph(srcPw, tgtPw, "output/sp.det.tnfa.tc.LFt48.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs, uniqueId = t)

srcPw = "mmu_Proteasome.gpml|Mm_Proteasome_Degradation_WP519_33047.gpml"
tgtPw = "Mm_TGF-beta_Receptor_Signaling_Pathway_WP258_34374.gpml"
dg = detailGraph(srcPw, tgtPw, "output/sp.det.prot.tgf.LFt48.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs, uniqueId = t)

srcPw = "Mm_EGFR1_Signaling_Pathway_WP572_35717.gpml"
tgtPw = V(fg)[nei(V(fg)[name %in% srcPw], mode="out")]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.egfr.LFt48.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

srcPw = "mmu_Proteasome.gpml|Mm_Proteasome_Degradation_WP519_33047.gpml"
tgtPw = V(fg)[nei(V(fg)[name %in% srcPw], mode="out")]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.prot.LFt48.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

#HF response t=48
t = "HFt0vst48_T"
r = results.all[[t]]
fg = filterPathwayGraph(r$graph)
srcPw = "Mm_TNF-alpha_NF-kB_Signaling_Pathway_WP246_34428.gpml"
tgtPw = "Mm_T_Cell_Receptor_Signaling_Pathway_WP480_34406.gpml"
dg = detailGraph(srcPw, tgtPw, "output/sp.det.tnfa.tc.HFt48.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs, uniqueId = t)

srcPw = "mmu_Proteasome.gpml|Mm_Proteasome_Degradation_WP519_33047.gpml"
tgtPw = "Mm_TGF-beta_Receptor_Signaling_Pathway_WP258_34374.gpml"
dg = detailGraph(srcPw, tgtPw, "output/sp.det.prot.tgf.HFt48.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs, uniqueId = t)

tgtPw = "Mm_Insulin_Signaling_WP65_33395.gpml"
srcPw = V(fg)[nei(V(fg)[name %in% tgtPw], mode="in")]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.ins.HFt48.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

adi = "Mm_Adipogenesis_WP447_37353.gpml"
tgtPw = tgtPw = V(fg)[nei(V(fg)[name %in% adi], mode="out")]$name
prot = "mmu_Proteasome.gpml|Mm_Proteasome_Degradation_WP519_33047.gpml"
include = r$stats$permP
include[,] = 0
include[adi, tgtPw] = 1
include[prot, adi] = 1
dg = detailGraph(include = include > 0, file = "output/sp.det.prot.adi.HFt48.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

tgtPw = "Kennedy_pathway_adapted_Rubio.gpml|mmu_Glycerophospholipid metabolism.gpml"
srcPw = V(fg)[nei(V(fg)[name %in% tgtPw], mode="in")]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.ken.HFt48.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

srcPw = "mmu_Axon guidance.gpml"
tgtPw = V(fg)[nei(V(fg)[name %in% srcPw], mode="out")]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.ax.HFt48.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

srcPw = "mmu_Proteasome.gpml|Mm_Proteasome_Degradation_WP519_33047.gpml"
tgtPw = V(fg)[nei(V(fg)[name %in% srcPw], mode="out")]$name
dg = detailGraph(srcPw, tgtPw, "output/sp.det.prot.HFt48.gml", max.length = max.length, interactions = r$interactions, neighborhood = neighborhood, details=T, igraphs=igraphs)

kras = "EnMm:ENSMUSG00000030265"
mapk14 = "EnMm:ENSMUSG00000053436"
pik3r1 = "EnMm:ENSMUSG00000041417"
gg = igraphs[[1]]
E(gg)[V(gg)[name == kras] %--% V(gg)[name == mapk14]]$PathwayId
E(gg)[V(gg)[name == kras] %--% V(gg)[name == pik3r1]]$PathwayId
