#############################################################
### Some functions specific to the pps2 data and analysis ###
#############################################################

## Get labels for the different gene-level tests, as used in the ##
## exported pps2 data file.                                      ##
getTestNames = function(tzero.hf = T, tzero.lf = T, hflf = T) {
	t = c()
	if(hflf) t = c(t, "HFvsLFt0_T", "HFvsLFt0.6_T", "HFvsLFt2_T", "HFvsLFt48_T")
	if(tzero.lf) t = c(t, "LFt0vst0.6_T", "LFt0vst2_T", "LFt0vst48_T")
	if(tzero.hf) t = c(t, "HFt0vst0.6_T", "HFt0vst2_T", "HFt0vst48_T")
	
	t
}

## Run the gene set test (pathway enrichment) on the pps2 data ##
## - pathways: The list of pathways
## - mixed: If false, performs two signed tests (up and down) and combines the results into a single, signed p-value (negative if down test is more significant)
## - ...: additional arguments to the doGeneSetTests function
ppsGeneSetTests = function(pathways, mixed = F, ...) {
	file = "input/weights/pps2.2log.stats.mapped.txt"
	task = getTestNames()
	gst = foreach(t = task) %dopar% {
		message("Calculating enrichment for ", t)
		if(length(grep("_F$", t)) > 0 || mixed) {
			doGeneSetTests(file, pathways, t, alternative = "mixed", ...)
		} else {
			up = doGeneSetTests(file, pathways, t, alternative = "up", ...)
			down = doGeneSetTests(file, pathways, t, alternative = "down", ...)
			mapply(function(u, d) {
				if(u <= d) u
				else -d
			}, up, down)
		} 
	}
	names(gst) = task
	gst
}

## Add the gene set test (pathway enrichment) results to the pathway
## nodes in the given graph.
addGeneSetTests = function(graph, gst) {
	pws = V(graph)$name
	for(t in names(gst)) {
		sp = as.numeric(gst[[t]][pws])
		sp[sp == 0] = 1E-5
		if(min(sp) < 0) {
			#Add p-value and signed p-value
			graph = set.vertex.attribute(
				graph, paste("gst_signed_p_", t, sep=""),
				value = sp
			)
			#Add log10 of signed p-value
			graph = set.vertex.attribute(
				graph, paste("gst_signed_log10_p_", t, sep=""),
				value = -sign(sp) * log10(abs(sp))
			)
		} else {
			#Add log10 of p-value
			graph = set.vertex.attribute(
				graph, paste("gst_log10_p_", t, sep=""),
				value = -log10(abs(sp))
			)
		}
		graph = set.vertex.attribute(
			graph, paste("gst_p_", t, sep=""),
			value = abs(sp)
		)
	}
	graph
}

## Reads the exported pps2 data file and adds the data attributes
## to the gene/protein nodes in the graph.
addDataAttributes = function(graph) {
	ids = V(graph)$name
	
	statsData = read.delim("input/weights/pps2.2log.stats.mapped.txt", as.is=T)
	
	numCols = which(sapply(statsData[1,], is.numeric))
	numCols = numCols[names(numCols) != 'id']
	
	avgData = t(sapply(ids, function(id) {
		data = statsData[statsData[,'mapped_xref'] == id, numCols]
		colMeans(data)
	}))
	
	for(col in colnames(avgData)) {
		graph = set.vertex.attribute(graph, col, value = avgData[ids, col])
	}
	graph
}

## Generate a set of resampled pathways for the permutation analysis ##
## - shuf.file: file to use as cache (will load if exists, save if not)
## - pathways: the original pathways
## - interactions: the interaction network to base the resampling on
## - nit: number of permutations to generate
## - strength: Whether to use graph strenght or degree
generatePermutations = function(shuf.file, pathways, interactions, nit, strength) {
	message("Setting up permutations")

	if(file.exists(shuf.file)) {
		message("Loading randomized pathways from cache")
		load(shuf.file)
	} else {
		message("Creating set of randomized pathways")
		if(strength) {
			E(interactions)$weight = 1 - E(interactions)$weight #Invert weight for graph.strength
		}
		sampler = degreeSampler(interactions, useNames = T, mode="out", strength = strength)
		shuffled = createShuffledPathways(pathways, interactions, sampler, mode="out", strength = strength)
		save(shuffled, sampler, file=shuf.file)
	}
	list(shuffled = shuffled, sampler = sampler)
}

## Convert the shortest path matrix (as returned by ShortestPath.R) ##
## to a score matrix.                                               ##
toScore = function(sp) {
	each(sp, pathToScore)
}

## Convert a vector of path lengths     ##
## to a single score                    ##
pathToScore = function(s) {
	if(is.na(s) || length(s) == 0) 0 else sum(1/s)
}

## Create urls for the google chart API based on the ##
## pathway enrichment results.                       ##
getGstChartUrls = function(gst, cols = c("HFvsLFt0_T", "HFvsLFt0.6_T", "HFvsLFt2_T", "HFvsLFt48_T"), sigp = 0.05, range = c(-4,4)) {
	p = list()
	p$chxt="y"
	p$chbh="a,0,0"
	p$chs="300x300"
	p$cht="bvg"
	p$chds=paste(range, collapse=",")
	p$chxr=paste(c(0,range), collapse=",")
	p$chf="bg,s,00000000"
	
	#chd, chco and chm depend on the data
	cr = colorRamp(c("green", "white", "red"))
	crmax = max(abs(range))
	
	data = foreach(tn = cols, .combine = cbind) %do% {
		as.numeric(gst[[tn]])
	}
	
	data = -sign(data) * log10(abs(data) + 1E-5)
	
	dimnames(data) = list(names(gst[[1]]), cols)
	urls = apply(data, 1, function(x) {
		xco = x
		xco[which(x > crmax)] = crmax
		xco[which(x < -crmax)] = -crmax
		xco = rgb(cr((xco + crmax) / (2*crmax)), max=255)
		xco = gsub("#", "", xco)
		p$chco = paste(xco, collapse="|")
		p$chd = paste("t:", paste(x, collapse=","), sep="")
		
		sig = which(abs(x) > -log10(sigp))
		if(length(sig) > 0) {
			sig = sig - 1
			chm = paste("t*,000000,0,", sig, ":", sig, ",100", sep="")
			p$chm = paste(chm, collapse="|")
		}
		
		pars = sapply(names(p), function(pn) paste(pn, p[[pn]], sep="="))
		url = "http://chart.apis.google.com/chart?"
		paste(url, paste(pars, collapse="&"), sep="")
	})
	names(urls) = rownames(data)
	urls
}

## Create urls for the google chart API based on the ##
## gene expression data.                             ##
getChartUrls = function(data, xrefCol = "mapped_xref") {
	diet = c("LF", "HF")
	time = c("t0", "t0.6", "t2", "t48")
	
	#maxval = max(data[,3:ncol(data)]) + 2

	#Constants
	p = list()
	p$chxl = paste(c("0:", time), collapse="|")
	p$chxt="x,y"
	p$chs="450x300"
	p$cht="lc"
	p$chco="3072F3,FF0000,C3D9FF,C3D9FF,FF9900,FF9900"
	p$chdl=paste(diet, collapse="|")
	p$chdlp="b"
	p$chls="10|10|0|0|0|0"
	p$cma="5,5,5,25"
	p$chm="b,E8F4F766,2,3,0,0|b,FFE6E666,4,5,0,1"
	
	#data paramters (chd)
	urls = apply(data[,3:ncol(data)], 1, function(row) {
		means = lapply(diet, function(dt) {
			cols = paste("mean_", dt, time, sep="")
			row[cols]
		})
		names(means) = diet
		sds = lapply(diet, function(dt) {
			cols = paste("sd_", dt, time, sep="")
			row[cols]
		})
		names(sds) = diet
		chd = c()
		for(dt in diet) chd = c(chd, paste(round(means[[dt]], 2), collapse=",")) #Data itself
		for(dt in diet) { #data +/- sd
				sd_plus = round(means[[dt]] + sds[[dt]], 2)
				sd_min = round(means[[dt]] - sds[[dt]], 2)
				chd = c(
					chd, paste(sd_plus, collapse=","), 
					 paste(sd_min, collapse=",")
				)
		}
		
		#Scaling
		rng = range(c(unlist(means) + unlist(sds), unlist(means) - unlist(sds)))
		rng[1] = rng[1] - 0.1
		rng[2] = rng[2] + 0.1
		p$chds = paste(rng[1], rng[2], sep=",")
		p$chxr = paste("1", rng[1], rng[2], sep=",")
		p$chd = paste("t:", paste(chd, collapse="|"), sep="")
		pars = sapply(names(p), function(pn) {
			paste(pn, p[[pn]], sep="=")
		})
		url = "http://chart.apis.google.com/chart?"
		paste(url, paste(pars, collapse="&"), sep="")
	})
	names(urls) = data[,xrefCol]
	urls
}

## Write wget commands to download charts from the Google Chart API ##
## generated with getChartUrls                                      ##
writeWgetCharts = function(symbols = NULL) {
	meanData = read.delim("input/weights/PPS2_filteredlist_2log.mean_sd_mapped.txt", as.is=T)
	urls = getChartUrls(meanData)
	fn = paste(names(urls), ".png", sep="")
	if(!is.null(symbols)) {
		symbols = symbols[names(urls)]
		fn = paste(symbols, "_", names(urls), ".png", sep="")
	}
	# wget -o NAME.png URL
	wget = paste("wget -O ", fn, " \"", urls, "\"", sep="")
	write.table(wget, "output/wget.data.graphs.txt", row.names=F, col.names=F, sep="\t", quote=F)
}

## Load the graphs for the interaction network used ##
## in the pps2 analysis                             ##
loadInteractionGraphs = function(gmlPaths) {
	igraphs = list()
	files = listFiles(gmlPaths, "gml")
	for(f in files) {
		message("Reading ", f)
		g = read.graph(f, "gml")
		V(g)$name = V(g)$identifier
		E(g)$SourceFile = basename(f)
		igraphs[[f]] = g
	}
	igraphs
}

## Create a detailed graph (including protein interactions) from the interaction ##
## between the given source and target pathways.                                 ##
## - srcPw: The pathway(s) that form the source of the interaction
## - tgtPw: The pathway(s) that form the target of the interaction
## - file: The gml file to write the graph to
## - interactions: The interaction network containing the protein interactions
## - details: Include detailed edge attributes from the individual interaction networks
## - include: Optional replacement for srcPw and tgtPw, a boolean matrix defining the interactions to include.
## - igraphs: The individual protein interaction networks to use if details is set to TRUE
## - ...: additional arguments to shortestPathsDetails
detailGraph = function(srcPw = NULL, tgtPw = NULL, file = "output/test.gml", interactions, details = T, include = NULL, igraphs = NULL, ...) {
	if(is.null(srcPw) | is.null(tgtPw)) {
		dg = shortestPathsDetails(include, pathways, interactions, ...)
	} else {
		dg = shortestPathsDetailsSrcTgt(srcPw, tgtPw, pathways, interactions, ...)
	}

	#Set cytoscape ids (label attribute)
	V(dg)$label = V(dg)$name
	
	#Set pathway and xref labels
	V(dg)$title = V(dg)$name
	V(dg)[name %in% names(pathways)]$title = as.character(sapply(V(dg)[name %in% names(pathways)]$name, function(n) pathwayTitles[n]))
	V(dg)[!name %in% names(pathways)]$title = as.character(sapply(V(dg)[!name %in% names(pathways)]$name, function(n) symbols[n]))
	
	V(dg)$title = as.character(V(dg)$title)
	
	V(dg)[title == 'NULL']$title = V(dg)[title == 'NULL']$name

	#Store igraph ids (cytoscape uses label as id)
	V(dg)$igraphId = as.character(V(dg))
	
	#Add interaction details
	if(details) {
		if(is.null(igraphs)) igraphs = loadInteractionGraphs("input/interactions/mouse-directed")
		dg = addInterationDetails(dg, igraphs = igraphs)
	}
	dg = addDataAttributes(dg)
	
	write.graph(dg, file, format="gml")
	fixGml(file, graph = dg)
	
	dg
}

## Run detailGraph for a given source pathway, including all outgoing neighbors target ##
detailGraphBySrc = function(pid, graph, label = "", pCutoff = 0.001, nrPathCutoff = 1, ...) {
	srcPw = V(graph)[pid]$name
	tgtPw = V(graph)[get.edges(graph, E(graph)[from(srcPw) & permP < pCutoff & nrPaths >= nrPathCutoff])[,2]]$name
	detailGraph(srcPw, tgtPw, paste("output/sp.det.", label, ".", srcPw, ".out.gml", sep=""), ...)
}

## Run detailGraph for a given source pathway, including all incoming neighbors as source ##
detailGraphByTgt = function(pid, graph, label = "", pCutoff = 0.001, nrPathCutoff = 1, ...) {
	tgtPw = V(graph)[pid]$name
	srcPw = V(graph)[get.edges(graph, E(graph)[to(tgtPw) & permP < pCutoff & nrPaths >= nrPathCutoff])[,1]]$name
	detailGraph(srcPw, tgtPw, paste("output/sp.det.", label, ".", tgtPw, ".in.gml", sep=""), ...)
}
