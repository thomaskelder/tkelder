#######################################################################
### Shared or utility functions for the pathway interaction scripts ###
#######################################################################
library(igraph)
library(hash)

src = "scripts/"
source(paste(src, "setbyname.R", sep=""))

## List edge attributes and index by edge name attribute. ##
edgeAttributesByName = function(g, att, nameAttr = 'name') {
	values = numeric()
	if(is.null(get.edge.attribute(g, nameAttr))) {
		nm = sapply(E(g), function(e) {
			ep = get.edge(g, e)
			p1 = V(g)[ep[1]]$name
			p2 = V(g)[ep[2]]$name
			paste(p1, "->", p2)
		})
		g = set.edge.attribute(g, nameAttr, value = nm)
	}
	values[as.character(get.edge.attribute(g, nameAttr))] = get.edge.attribute(g, att)
	values[sort(names(values))] #Sort by keys to make plotting easier
}

## Create a subgraph from the selected edges. ##
subgraphFromEdges = function(graph, edges) {
	remove = E(graph)[!(E(graph) %in% edges)]
	graph = delete.edges(graph, remove)
	## Also delete nodes that have no edges in filtered graph
	delete.vertices(graph, V(graph)[degree(graph) == 0])
}

## Filter graph by attribute threshold ##
subGraphByAttr = function(graph, low = NA, high = NA, att = "Pvalue", vertices = T) {
	if(!is.na(low)) graph = delete.edges(graph, E(graph)[get.edge.attribute(graph, att) < low])
	if(!is.na(high)) graph = delete.edges(graph, E(graph)[get.edge.attribute(graph, att) > high])
	## Also delete nodes that have no edges in filtered graph
	if(vertices) graph = delete.vertices(graph, V(graph)[degree(graph) == 0])
	graph
}

## Perform the limma geneSetTest function on a list of pathways ##
## using a mapped datafile as input                             ##
doGeneSetTests = function(file, pathways, datacol, xrefcol = 'mapped_xref', ...) {
	library(limma)
	data = read.delim(file, as.is=T)
	stats = data[,datacol]
	lapply(pathways, function(p) {
		sel = data[,xrefcol] %in% p
		if(sum(sel) < 1) 1
		else geneSetTest(sel, stats, ...)
	})
}

## Read mapped node weights from a tab delimited text file ##
## (e.g. exported with org.pct.util.XrefData) and derive   ##
## edge weights from this data.                            ##
## - file: The data file to import
## - interactions: The graph to add the weights to 
## - datacol: The column that contains the weights
## - xrefcol: The column that contains the xref (to match with nodes in graph)
## - abs: Whether to take absolute values of the data
## - ...: Additional arguments to the sigm function
readWeights = function(file, interactions, datacol, xrefcol = 'mapped_xref', abs = T, ...) {
	message("Reading weights")
	rawWeights = read.delim(file, as.is=T)
	if(abs) rawWeights[,datacol] = abs(rawWeights[,datacol])

	## Calculate weights for each node
	nodeWeights = sapply(V(interactions)$name, function(x) {
		w = rawWeights[rawWeights[,xrefcol] == x, datacol]
		w = mean(w, na.rm=T)
		if(is.nan(w)) w = NA
		w
	})

	## Set edge weights
	edgeWeights = sapply(E(interactions), function(e) {
		end = get.edge(interactions, e)
		nodeWeights[end[2] + 1]
	})

	## Apply soft threshold (sigmoid)
	sigm = function(x, a = 2, u = 3) {
		1/(1 + exp(-a*(x - u)))
	}
	sigmEdgeWeights = 1 - sigm(edgeWeights, ...)
	sigmEdgeWeights[is.na(sigmEdgeWeights)] = 1
	sigmNodeWeights = 1 - sigm(nodeWeights, ...)
	sigmNodeWeights[is.na(sigmNodeWeights)] = 1

	## Scale weights:
	# 0: highest differential expression (low cost in path)
	# 1: lowest differential expression (high cost in path)
	doScale = function(w) {
		sw = w
		min = min(w, na.rm=T)
		max = max(w, na.rm=T)
		if(min < 0 || max > 1) {
			sw = (w - min) / (max - min)
		}
		1 - sw #Higher T score is lower path cost
		sw[is.na(sw)] = 1 #No data is highest path cost
	}

	scaledEdgeWeights = doScale(edgeWeights)
	scaledNodeWeights = doScale(nodeWeights)
	
	list(
		nodeWeights = nodeWeights, edgeWeights = edgeWeights, 
		scaledNodeWeights = scaledNodeWeights, scaledEdgeWeights = scaledEdgeWeights,
		sigmNodeWeights = sigmNodeWeights, sigmEdgeWeights = sigmEdgeWeights
	)
}

## Create a set of randomized pathways (see PathwySampler.R) ##
## - pathways: the list of pathways to shuffle
## - interactions: the interaction network to base the randomization on
## - hash created with the degreeSampler function
createShuffledPathways = function(pathways, interactions, sampler, ...) {
	message("Creating set of randomized pathways")
	ipathways = pathwaysToNodeIndex(pathways, interactions)
	lapply(1:nit, function(i) {
		message(date(), ":\t", i)
		samplePathways(ipathways, interactions, sampler, ...)
	})
}

## Get the packages these scripts depends on ##
getPackages = function() {
	c("igraph", "hash", "foreach")
}

## Hack to apply several modifications to the gml file as exported ##
## by igraph, since these are not compatible with Cytoscape:       ##
## - Convert doubles written as '1' to '1.0'                       ##
## - Convert inf to Infinity                                       ##
## - Convert nan to NaN                                            ##
## - Remove scientific notation                                    ##
##
## - f: the gml file
## - att: the attributes to apply the fix to
## - graph: if specified, all attribute names in the graph are used
fixGml = function(f, att = c(), graph = NULL) {
	f = paste("'", f, "'", sep="")
	if(!is.null(graph)) {
		gatt = union(list.edge.attributes(graph), list.vertex.attributes(graph))
		att = union(att, gatt)
	}
	#Attributes in gml have no underscores
	att = gsub("_", "", att)
	att = gsub("\\.", "", att)
	
	#Never touch 'id' attribute, as it should be integer
	att = att[att != "id"]
	system(paste("perl scripts/fixSci.pl ", f, " > ", 
			f, ".tmp && mv ", f, ".tmp ", f, sep=""))
	
	expr = as.character(sapply(att, function(a) {
		e = c()
		e = c(e, paste("-e 's/", a, " (-?[0-9]+)$/", a, " \\1.0/g' ", sep=''))

		e = c(e, paste("-e 's/", a, " (-?)inf$/", a, " \\1.0Infinity/g' ", sep=''))
		e = c(e, paste("-e 's/", a, " (-?)nan$/", a, " NaN/g' ", sep=''))
		e
	}))
	expr = paste(expr, collapse='')
	cmd = paste('sed -r ', expr, f, ' > ', f, '.tmp && mv ', f, '.tmp ', f, sep='')
	system(cmd, intern = T)
}

## Merge multiple graphs into a single graph, while maintaining the ##
## attributes when possible.                                        ##
## - graphs: The graphs to merge
## - prefix: The prefix to use in the merged graph for the attributes
## - overwrite: Do not use a prefix for these attributes, but overwrite them with the value in the latest graph
mergeGraphs = function(graphs, prefix = names(graphs), overwrite = c("name")) {
	merged = graph.empty()
	
	for(i in 1:length(graphs)) {
		g = graphs[[i]]
		#Add nodes
		newv = V(g)[!(name %in% V(merged)$name)]
		attr = list()
		for(n in list.vertex.attributes(g)) {
			attr[[n]] = get.vertex.attribute(g, n, newv)
		}
		newNames = names(attr)
		replace = which(!(names(attr) %in% overwrite))
		newNames[replace] = paste(prefix[[i]], names(attr)[replace], sep="_")
		names(attr) = newNames
		
		merged = add.vertices(
			merged, length(newv), attr = attr
		)

		#Add node attributes
		newIndex = sapply(V(g), function(x) {
			nm = V(g)[x]$name
			V(merged)[name == nm]
		})
		
		for(n in list.vertex.attributes(g)) {
			values = get.vertex.attribute(g, n)
			if(!(n %in% overwrite)) n = paste(prefix[[i]], n, sep="_")
			merged = set.vertex.attribute(merged, n, V(merged)[newIndex], values)
		}
		
		#Add edges
		edges = c()
		
		edges = foreach(e = E(g), .combine=c) %do% {
			ep = get.edge(g, e)
			n1 = ep[1]
			n2 = ep[2]
			ni1 = V(merged)[name == V(g)[n1]$name]
			ni2 = V(merged)[name == V(g)[n2]$name]
			c(ni1, ni2)
		}

		attr = list()
		for(n in list.edge.attributes(g)) {
			attr[[n]] = get.edge.attribute(g, n)
		}
		attr$sourceGraph = rep(prefix[[i]], ecount(g))
		#Cytoscape needs a label to recognize multiple edges between node pair
		attr$label = sapply(E(g), function(e) {
			ep = get.edge(g, e)
			p1 = V(g)[ep[1]]$name
			p2 = V(g)[ep[2]]$name
			paste(prefix[[i]], p1, "->", p2, sep="")
		})

		merged = add.edges(merged, edges, attr = attr)
	}

	merged
}

## Read directed interaction networks and merge them into a   ##
## single graph. Attributes will not be preserved (except for ##
## the vertex/edge name)                                      ##
## - gmlPaths: The path(s) to the .gml files to read
## - nameAttr: The attribute that will be used to merge different files on
readInteractionsDirected = function(gmlPaths, nameAttr = 'identifier') {
	files = listFiles(gmlPaths, "gml")
	el = NULL
	for(f in files) {
		message("Reading ", f)
		g = read.graph(f, "gml")
		V(g)$name = get.vertex.attribute(g, nameAttr)
		E(g)$name = get.edge.attribute(g, nameAttr)
		
		gel = get.edgelist(g)
		if(is.null(el)) {
			el = gel
		} else {
			#Combine the edge lists
			el = unique(rbind(gel, el))
		}
	}
	graph.edgelist(el, directed=T)
}

## Read undirected interaction networks and merge them into a ##
## single graph. Attributes will not be preserved (except for ##
## the vertex/edge name)                                      ##
## - gmlPaths: The path(s) to the .gml files to read
## - nameAttr: The attribute that will be used to merge different files on
readInteractions = function(gmlPaths, nameAttr = 'identifier') {
	files = listFiles(gmlPaths, "gml")
	interactions = NULL
	for(f in files) {
		message("Reading ", f)
		g = read.graph(f, "gml")
		V(g)$name = get.vertex.attribute(g, nameAttr)
		E(g)$name = get.edge.attribute(g, nameAttr)
		if(is.null(interactions)) {
			interactions = g
		} else {
			interactions = graph.union.by.name(g, interactions)
		}
	}
	interactions
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

## Read pathway titles (from files exported with ## 
## org.pct.util.PathwayOverlap).                 ##
readPathwayTitles = function(path) {
	files = listFiles(path, "txt")
	pathways = lapply(files, function(f) {
		read.delim(f, as.is=T, header=F)
	})
	titles = sapply(pathways, function(p) { p[2,1] })
	names(titles) = sapply(pathways, function(p) { p[1,1] })
	titles
}

## Recursively lits files with a given extension ##
## from multiple paths.                          ##
listFiles = function(paths, ext) {
	dirs = file.info(paths)$isdir
	files = dir(paths[dirs], pattern=paste("\\.", ext, "$", sep=""), 
		recursive=T, full.names=T, ignore.case=T)
	files = append(files, paths[!dirs])
	files
}

## Create a hash mapping node names to their index in the graph. ##
name2index = function(g) {
	hash(V(g)$name, V(g))
}

## Convert pathways that are node name vectors to ##
## node index vectors.                            ##
pathwaysToNodeIndex = function(pathways, g, xref2index = name2index(g)) {
	ipathways = lapply(pathways, function(p) {
		p = p[p %in% V(g)$name]
		as.numeric(values(xref2index[p]))
	})
}

## Run a function and report its execution time ##
timer = function(f, ...) {
	sd = date()
	st = proc.time()
	v= f(...)
	t = (proc.time() - st)[3]
	message("Started at: ", sd)
	message("Ended at: ", date())
	message("Elapsed: ", t, " (s)")
	v
}

## Run a function on each element in an array ##
each = function(a, f) {
	d = sapply(a, f)
	array(d, dim=dim(a), dimnames=dimnames(a))
}

## Convert a generic matrix to a numeric matrix ##
matrix.as.numeric = function(a) {
	d = as.numeric(a)
	array(d, dim=dim(a), dimnames=dimnames(a))
}

## Perform score corrections and tests on score matrix ##
## (output from ShortestPath.R )                       ##
## - scores: the score matrix
## - perm: list of score matrices for the first permutation round
## - perm.2nd: list of score matrices for the second permutation round
scoreTests = function(scores, perm, perm.2nd = NULL) {
	message("Calculating empirical p-values")
	
	calcPermP = function(scr, prm, prm.2nd) {
		prmp = each(scr, function(x) NA)
		for(i in 1:nrow(scr)) {
			for(j in 1:ncol(scr)) {
				f = scr[i,j]
			
				if(is.na(f)) { prmp[i,j] = NA; next }
				if(f == 0) { prmp[i,j] = 1; next }
				
				pfs = sapply(prm, function(x) x[i,j])
				if(!is.null(prm.2nd)) {
					pfs.2nd = sapply(prm.2nd, function(x) x[i,j])
					if(
						sum(pfs.2nd == 0, na.rm=T) < length(pfs.2nd) && 
						sum(is.na(pfs.2nd)) < length(pfs.2nd)) {
							pfs = c(pfs, pfs.2nd)
					}
				}
				pfs[is.na(pfs)] = 0
				prmp[i,j] = sum(pfs >= f) / length(pfs)
			}
		}
		prmp
	}
	
	pp = calcPermP(scores, perm, perm.2nd)
	list(permP = pp, scores = scores)
}

## Convert a score matrix (output of ShortestPath.R) to a graph object. ##
## - scores: The score matrix
## - directed: Whether the pathway interactions are directed
## - stats: List of statistics calculated using the scoreTests function
## - pathways: The list of pathways used in the analysis (to add additional attributes, e.g. pathway size)
## - pathwayTitles: Alternative names for the pathway nodes
## - pathCounts: Matrix that contains number of paths for each interaction
scoresAsGraph = function(scores, directed = T, stats = NULL, pathways = NULL, pathwayTitles = NULL, pathCounts = NULL) {
	scores.n = each(scores, function(x) if(is.na(x)) 0 else x)
	mode = ifelse(directed, "directed", "upper")
	graph = graph.adjacency(scores.n, diag=F, weighted=T, mode=mode)
	
	## Edge attributes
	# Add p/q-values
	toEdgeAtt = function(g, x) {
		sapply(E(graph), function(e) {
			ep = get.edge(graph, e)
			p1 = V(graph)[ep[1]]$name
			p2 = V(graph)[ep[2]]$name
			x[p1,p2]
		})
	}

	if(!is.null(stats)) {
		for(p in names(stats)) {
			if(p == "pathway.degree" || p == "pathway.strength") next
			graph = set.edge.attribute(graph, gsub('\\.', '', p), value=toEdgeAtt(graph, stats[[p]]))
		}
	}
	
	if(!is.null(pathCounts)) {
		graph = set.edge.attribute(graph, "nrPaths", value=toEdgeAtt(graph, pathCounts))
	}
	
	## Node attributes
	toNodeAtt = function(g, x) {
		sapply(V(graph)$name, function(vn) {
			x[vn]
		})
	}

	# Add pathway cumulative degree
	V(graph)$pathwayDegree = toNodeAtt(graph, stats$pathway.degree)
	# Add pathway size
	if(!is.null(pathways)) {
		V(graph)$nrXrefs = toNodeAtt(graph, lapply(pathways, length))
	}
	
	# Add pathway titles
	if(!is.null(pathwayTitles)) {
		V(graph)$title = toNodeAtt(graph, pathwayTitles)
	}
	# Set Cytoscape node id to pathway name
	V(graph)$label = V(graph)$name
	
	graph
}

## Adds edge annotations for edges between xrefs based on the ##
## original (unmerged) interaction graphs.                    ##
## - graph: The graph to add the edge annotations to
## - gmlPaths: The paths to the original gml files 
## - igraphs: The graph objects for the original interaction graphs (may be used instead of glmPaths when calling this function multiple times)
## - attr: The attributes to extract from the source graph.
addInterationDetails = function(graph, gmlPaths = NULL, igraphs = list(), attr = c("Source", "Interaction", "InteractionValue", "SourceFile", "PubmedId", "DetectionMethod"), sep='; ')  {
	if(!is.null(gmlPaths)) {
		files = listFiles(gmlPaths, "gml")
		for(f in files) {
			message("Reading ", f)
			g = read.graph(f, "gml")
			V(g)$name = V(g)$identifier
			E(g)$SourceFile = basename(f)
			igraphs[[f]] = g
		}
	}
	
	avalues = lapply(attr, function(a) c())
	names(avalues) = attr
	
	for(g in igraphs) {
		#First lookup
		etable = sapply(E(graph), function(e) {
			nodes = V(graph)[get.edge(graph, e)]$name
			isrc = V(g)[name == nodes[1]]
			itgt = V(g)[name == nodes[2]]
			if(length(isrc) > 0 && length(itgt) > 0) c(isrc, itgt)
			else c(-1, -1)
		})
		
		for(a in attr) {
			gvalues = apply(etable, 2, function(p) {
				isrc = p[1]
				itgt = p[2]
				if(isrc > 0 && itgt > 0)
					paste(get.edge.attribute(g, a, E(g)[isrc %->% itgt]), collapse=sep)
				else
					""
			})
			if(length(avalues[[a]]) == 0) {
				avalues[[a]] = gvalues
			} else {
				avalues[[a]] = paste(avalues[[a]], gvalues, sep=sep)
			}
		}
	}
	avalues = lapply(avalues, function(x) gsub(" ;", "", x))
	avalues = lapply(avalues, function(x) gsub("^; ", "", x))
	
	for(a in attr) graph = set.edge.attribute(graph, a, value = avalues[[a]])
	graph
}

################################################################
## Several support functions for MaxFlow.R and ShortestPath.R ##
################################################################

## Combine an interaction network with pathway annotations   ##
## This will add a pathway node for each pathway and connect ##
## it to the xrefs that are annotated to that pathway        ##
createPathwayXrefGraph = function(interactions, pathways) {
	directed = is.directed(interactions)
	
	#Add the pathways as nodes to the networks
	attr = list()
	attr$name = names(pathways)
	pxgraph = add.vertices(interactions, length(pathways), attr = attr)

	#Add edge between pathway and xref for association
	for(n in names(pathways)) {
		p = pathways[[n]]
		pn = V(pxgraph)[name == n]
		xn = V(pxgraph)[name %in% p]
		edges = rep(NA, length(xn)*2)
		edges[seq(1,length(edges), 2)] = pn
		edges[seq(2, length(edges), 2)] = xn
		pxgraph = add.edges(pxgraph, edges)
	
		if(directed) { #Also add reverse
			edges = rep(NA, length(xn)*2)
			edges[seq(2,length(edges), 2)] = pn
			edges[seq(1, length(edges), 2)] = xn
			pxgraph = add.edges(pxgraph, edges)
		}
	}
	
	pxgraph
}

## Rewire the edges that associate the xrefs of a pathway with the ##
## pathway nodes based on a sampled version of the pathway         ##
## - pxgraph: The graph
## - p: The pathway node
## - sampled: The names of the sampled xrefs
rewireToSampled = function(pxgraph, p, sampled) {
		edges = E(pxgraph)[from(p)]
		pxgraph = delete.edges(pxgraph, edges)
		xs = V(pxgraph)[name %in% sampled]
		edges.new = rep(NA, length(xs)*2)
		edges.new[seq(1,length(edges.new), 2)] = p
		edges.new[seq(2, length(edges.new), 2)] = xs
		add.edges(pxgraph, edges.new)
}

## Creates a subgraph that will be used to find interactions between ##
## two pathways (e.g. by ShortestPath.R                              ##
## - nb: the size of the neighborhood to include
## - p1, p2: the two pathways for which to build the subgraph
## - p1x, p2x: the xrefs in the two pathways
## - notInPathway: all other xrefs to include in the subgraph
createPathwayPairGraph = function(nb, p1, p2, p1x, p2x, notInPathway) {
		if(is.directed(nb)) {
			# For g which is in p1 and p2, remove edge p2->g
			# This will include flow through this gene via other paths, but not
			# via pathway directly (would result in infinite flow)
			p1u = p1x
			p2u = p2x
			int = intersect(p1x, p2x)
			e = E(nb)[p2 %--% int]
			if(length(e) > 0) g = delete.edges(nb, e)
			else g = nb
			
			#Remove edges p1<-p1x and p2->x
			g = delete.edges(g, E(g)[to(p1) | from(p2)])
		} else {
			# Exclude nodes that are associated with both pathways
			p1u = setdiff(p1x, p2x)
			p2u = setdiff(p2x, p1x)
			g = nb
		}
		
		subgraph(g, c(p1, p2, p1u, p2u, notInPathway))
}
