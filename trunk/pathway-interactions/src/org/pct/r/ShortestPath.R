## Find interactions between pathways by comparing the  ##
## shortest path distributions of their xrefs           ##
##                                                      ##
## Assumes that all xrefs are already translated to a   ##
## common datasource                                    ##
library(foreach)

src = "scripts/"
source(paste(src, "Functions.R", sep=""))

## Find shortest paths between pathway pairs based on an interaction network.  ##
## - interactions: The interaction network (interactions between entities within the pathways)
## - pathways: A list of pathways
## - neighborhood: The order of the neighborhood to consider (e.g. 3 will consider the nodes connected to the pathway, plus interactions within 2 steps)
## - pxgraph: Use a cached pathway-interaction graph (created with weightPathwayXrefGraph)
## - value.only: Only return the path lengths, not the paths themselves. If FALSE, the paths and the subgraph for each pathway pair will be returned.
## - sampledPathways: The resampled pathways to use (for a permutation step)
## - max.length: The maximum (weighted) length of a path to consider.
## - max.weight: The maximum edge weight value to consider
## - pathwayXref: All xrefs in the pathways
## - excludePairs: boolean matrix specifying pathway pairs to exclude from the analysis
## - deletePaths: Look for non-redundant paths only by (TRUE), or also consider redundant paths (FALSE). The latter option will be much slower.
## - scoreFun: Function that will be used to calculate a score based on the path lengths
shortestPaths = function(interactions, pathways, neighborhood = NA, pxgraph = NULL, value.only = T, sampledPathways = NULL, max.length = 2, max.weight = max.length, pathwayXrefs = unique(unlist(pathways)), excludePairs = NULL, deletePaths = T, scoreFun = NULL) {

	findPaths = function(start, end, g, adj, max.length, path = c()) {
		path = c(path, start)
		if(start == end) return(path)
		l = 0
		if(!is.na(neighborhood) && (length(path) >= neighborhood + 1)) return()

		l = sum(E(g, path=path)$weight)
		if(l > max.length) return()

		nodes = setdiff(adj[[start+1]], path)
		lapply(nodes, function(n) findPaths(n, end, g, adj, max.length, path))
	}

	directed = is.directed(interactions)
	
	if(is.null(pxgraph)) pxgraph = weightPathwayXrefGraph(interactions, pathways)
	if(is.null(excludePairs)) {
		excludePairs = matrix(F, nrow = length(pathways), ncol = length(pathways))
		dimnames(excludePairs) = list(names(pathways), names(pathways))
	}	

	message("Edges before removing weight > ", max.weight, ": ", ecount(pxgraph))
	message("Nodes before removing weight > ", max.weight, ": ", vcount(pxgraph))
	pxgraph = delete.edges(pxgraph, E(pxgraph)[weight > max.weight])
	pxgraph = delete.vertices(pxgraph, which(degree(pxgraph) == 0) - 1)
	message("Edges after removing weight > ", max.weight, ": ", ecount(pxgraph))
	message("Nodes after removing weight > ", max.weight, ": ", vcount(pxgraph))
	
	xrefs = V(interactions)$name
	pathwayInGraph = lapply(pathways, function(p) {
		p[p %in% xrefs]
	})

	pathwayNodes = V(pxgraph)[name %in% names(pathways)]
	pathwayNames = pathwayNodes$name

	npws = length(pathwayNodes)

	export = c("createPathwayPairGraph")

	paths = foreach(i = 1:length(pathwayNames), .combine=rbind, .inorder=T, .packages=c("igraph", "hash"), .export=export) %dopar% {
		message(date(), ":\t", "Processing ", i, " out of ", npws)
		p1n = pathwayNames[i]

		p1paths = as.list(rep(NA, length(pathwayNames)))
		names(p1paths) = pathwayNames
		
		if(sum(excludePairs[p1n,]) == npws) {
			message("Skipping ", p1n, ": all targets excluded")
			return(p1paths)
		}
		st = proc.time()

 		p1 = V(pxgraph)[name == p1n]

		pathwayXrefs.s = pathwayXrefs

		#If we use the sampled pathways, rewire p1 to the sampled xrefs
		if(!is.null(sampledPathways)) {
			pxgraph = rewireToSampled(pxgraph, p1, sampledPathways[[p1n]])
			
			#Set weight of edges from pathway to target node
			fromEdges = E(pxgraph)[from(p1)]
			w = sapply(fromEdges, function(e)	V(pxgraph)[get.edge(pxgraph, e)[2]]$weight)
			E(pxgraph)[fromEdges]$weight = w
	
			#Remove original pathways from list of xrefs in pathways
			pathwayXrefs.s = setdiff(pathwayXrefs.s, pathways[[p1n]])
			#Add sampled pathway to list of xrefs in pathways
			pathwayXrefs.s = union(pathwayXrefs.s, sampledPathways[[p1n]])
		}
		
		mode = ifelse(directed, "out", "all")
		if(is.na(neighborhood)) {
			nb = pxgraph		
		} else {
			nb = graph.neighborhood(pxgraph, neighborhood, p1, mode=mode)[[1]]
		}
		p1 = V(nb)[name == p1n] #New p1 index, based on neighborhood graph
		p1x = neighbors(nb, p1)

		notInPathway = V(nb)[!(name %in% pathwayXrefs.s)]
		notInPathway = setdiff(notInPathway, V(nb)[name %in% pathwayNames])
		
		nbPws = V(nb)[name %in% pathwayNames]
		message("Number of pathways in neighborhood: ", length(nbPws))
		
		for(p2 in nbPws) {
			if(p1 == p2) next
			
			p2n = V(nb)[p2]$name
			if(excludePairs[p1n, p2n]) next

			p2x = neighbors(nb, p2)
	
			#message("\t", p2n)
			g = createPathwayPairGraph(nb, p1, p2, p1x, p2x, notInPathway)
			
			p1g = V(g)[name == p1n]
			p2g = V(g)[name == p2n]
			
			## Find shortest path -> store
			## Remove edges of that path
			## Repeat until max length reached
			resultPaths = list()
			resultGraph = g
			resultValues = list()
			
			if(deletePaths) {
				l = 0
				i = 1
				while(T) {
					sp = get.shortest.paths(g, p1g, p2g, mode)
					spv = sp[[1]]
					if(length(spv) == 0) break
					spe = E(g, path=spv)

					l = sum(spe$weight)
					if(l > max.length) break
				
					spe = as.numeric(spe)
					resultPaths[[i]] = spv #Store the result
					resultValues[[i]] = l
				
					#Remove edges in path and find shortest path again
					g = delete.edges(g, spe[2:(length(spe)-1)])
				
					i = i + 1
				}
			} else {
				sp = findPaths(p1g, p2g, g, get.adjlist(g, mode), max.length)
				idx = rapply(sp, length)
				if(!is.null(idx)) {
					resultPaths = split(unlist(sp), rep(1:length(idx), idx))
					resultValues = lapply(resultPaths, function(p) sum(E(g, path=p)$weight))
				}
			}

			if(value.only) {
				v = as.numeric(resultValues)
				if(!is.null(scoreFun)) v = scoreFun(v)
				p1paths[[p2n]] = v
			} else {
				p1paths[[p2n]] = list(paths = resultPaths, graph = resultGraph)
			}
		}
		t = (proc.time() - st)[3]
		message("Elapsed: ", t, " (s)")
		p1paths
	}
	rownames(paths) = pathwayNames
	paths
}

## Find pathway interactions (using shortestPaths) and create a graph containing ##
## also the protein interactions that form the paths between the pathways.       ##
## - include: a boolean matrix to specify which pathway pairs to include
## - pathways: the pathways
## - interactions: the protein interaction network
## - paths.only: only include the nodes and edges that contribute to a path (so exclude proteins that don't)
## - uniqueId: a unique identifier for generating the edge identifiers (to support loading multiple of these graphs into cytoscape without overwriting the edge attributes)
## - ...: additional arguments to shortestPaths
shortestPathsDetails = function(include, pathways, interactions, paths.only = T, uniqueId = "", ...) {
	sources = names(pathways)[rowSums(include) > 0]
	targets = names(pathways)[colSums(include) > 0]
	
	pwx = unique(unlist(pathways))
	pws = pathways[union(sources,targets)]
	sps = shortestPaths(interactions, pws, value.only=F, pathwayXrefs = pwx, exclude = !include, ...)
	
	message("Extracting paths")
	graph = NULL
	
	for(s in sources) {
		for(t in targets) {
			if(!include[[s, t]]) next;
			if(is.na(sps[[s,t]])) next
			if(length(sps[[s,t]]$paths) == 0) next
			
			## Extract the paths
			g = sps[[s, t]]$graph
			vpaths = sps[[s, t]]$paths
			epaths = lapply(vpaths, function(p) E(g, path=p))
			edges = unique(unlist(epaths))
			pcounts = sapply(edges, function(e) {
				sum(sapply(epaths, function(p) e %in% p))
			})
			E(g)[edges]$nrPaths = pcounts
		
			if(paths.only) {
				rm = setdiff(E(g), edges)
				rm = setdiff(rm, E(g)[from(s)])
				rm = setdiff(rm, E(g)[to(t)])
				g = delete.edges(g, rm)
				g = delete.vertices(g, which(degree(g) == 0) - 1)
			}
		
			if(is.null(graph)) graph = g
			else {
				#Merge into total graph
				newv = V(g)[!(name %in% V(graph)$name)]
				attr = lapply(list.vertex.attributes(g), function(a) {
					get.vertex.attribute(g, a, newv)
				})
				names(attr) = list.vertex.attributes(g)
				graph = add.vertices(graph, length(newv), attr = attr)
			
				newe = c()
				eids = c()
				for(n1 in V(g)) {
					from = E(g)[from(n1)]
					nn1 = V(graph)[name == V(g)[n1]$name]
					newe = c(newe, sapply(from, function(e) {
						n2 = get.edge(g, e)[2]
						nn2 = V(graph)[name == V(g)[n2]$name]
						c(nn1, nn2)
					}))
					eids = c(eids, from)
				}
				attr = lapply(list.edge.attributes(g), function(a) {
					get.edge.attribute(g, a, eids)
				})
				names(attr) = list.edge.attributes(g)
				graph = add.edges(graph, newe, attr = attr)
			}
		}
	}
	
	#Merge duplicate edges
	sgraph = simplify(graph, T, F)
	
	#Add edges that were removed due to overlapping xrefs
	E(sgraph)$pwIntersect = 0
	srcXrefs = intersect(unique(unlist(pathways[sources])), V(sgraph)$name)
	for(tgt in targets) {
		sgtgt = V(sgraph)[name == tgt]
		tgtXrefs = intersect(V(sgraph)$name, pathways[[tgt]])
		int = intersect(tgtXrefs, srcXrefs)
		for(ix in int) {
			sgix = V(sgraph)[name == ix]
			#Edge may not be present when overlap in all src/tgt combinations
			if(!are.connected(sgraph, sgix, sgtgt)) {
				sgraph = add.edges(sgraph, c(sgix, sgtgt))
			}
			e = E(sgraph)[sgix %->% sgtgt]
			E(sgraph)[e]$pwIntersect = 1
		}
	}
	
	#Sum path counts
	pcounts = sapply(E(sgraph), function(se) {
		sep = get.edge(sgraph, se)
		sum(E(graph)[sep[1] %->% sep[2]]$nrPaths, na.rm=T)
	})
	E(sgraph)$nrPaths = pcounts
	
	#Add unique id for edge (so attributes will not be overwritten
	#when loading multiple graphs in Cytoscape
	library(digest)
	code = paste(c(uniqueId, sources, targets), collapse="")
	code = digest(code, 'crc32')
	E(sgraph)$label = as.character(code)
	
	sgraph
}

## Run shortestPathsDetails based on interactions between the given ##
## source and target pathways.                                      ##
shortestPathsDetailsSrcTgt = function(sources, targets, ...) {
	include = matrix(F, ncol = length(pathways), nrow = length(pathways))
	dimnames(include) = list(names(pathways), names(pathways))
	for(s in sources) {
		include[s, targets] = T
	}
	shortestPathsDetails(include, ...)
}

inverseLengthFun = function() {
	function(s) {
		if(is.null(s)) {
			NA
		} else {
			sum(as.numeric(lapply(names(s), function(d) s[d][[1]] / as.numeric(d))))
		}
	}
}

inverseLengthScore = function(sps) {
	each(sps, inverseLengthFun())
}

weightPathwayXrefGraph = function(interactions, pathways) {
	pxgraph = createPathwayXrefGraph(interactions, pathways)

	#Set weight of edges to pathway to target node to 0 (no cost)
	E(pxgraph)[to(V(pxgraph)[name %in% names(pathways)])]$weight = 0
	
	#Set weight of edges from pathway to target node
	fromEdges = E(pxgraph)[from(V(pxgraph)[name %in% names(pathways)])]
	w = sapply(fromEdges, function(e)	V(pxgraph)[get.edge(pxgraph, e)[2]]$weight)
	E(pxgraph)[fromEdges]$weight = w
	
	pxgraph
}
