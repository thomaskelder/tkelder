###########################################################
## Find interactions between pathways by calculating     ##
## the maximal flow between them.                        ##
##                                                       ##
## Assumes that all xrefs are already translated to a    ##
## common datasource.                                    ##
###########################################################

library(foreach)
src = "scripts/"
source(paste(src, "Functions.R", sep=""))

maxFlow = function(interactions, pathways, capCutoff = 0, neighborhood = 3, pxgraph = NULL, value.only = T, sampledPathways = NULL, pathwayXrefs = unique(unlist(pathways)), limitPwCap = c("from", "to")) {
	directed = is.directed(interactions)
	
	if(is.null(pxgraph)) pxgraph = capacityPathwayXrefGraph(interactions, pathways)
	message("Edges before removing capacity <= ", capCutoff, ": ", ecount(pxgraph))
	pxgraph = delete.edges(pxgraph, E(pxgraph)[capacity <= capCutoff])
	message("Edges after removing capacity <= ", capCutoff, ": ", ecount(pxgraph))

	xrefs = V(interactions)$name
	pathwayInGraph = lapply(pathways, function(p) {
		p[p %in% xrefs]
	})

	pathwayNodes = V(pxgraph)[name %in% names(pathways)]
	pathwayNames = pathwayNodes$name

	npws = length(pathwayNodes)

	export = c("createPathwayPairGraph", "limitPathwayCapacity")
	
	flows = foreach(i = 1:length(pathwayNames), .combine=rbind, .inorder=T, .packages=c("igraph", "hash"), .export=export) %dopar% {
		message(date(), ":\t", "Processing ", i, " out of ", npws)
		p1n = pathwayNames[i]
		
		st = proc.time()

 		p1 = V(pxgraph)[name == p1n]

		pathwayXrefs.s = pathwayXrefs

		#If we use the sampled pathways, rewire p1 to the sampled xrefs
		if(!is.null(sampledPathways)) {
			pxgraph = rewireToSampled(pxgraph, p1, sampledPathways[[p1n]])
			#Re-set capacity of outgoing edges fom pathway to target node weight
			if("from" %in% limitPwCap) {
				pxgraph = limitPathwayCapacity(pxgraph, p1n, "from")
			} else {
				E(pxgraph)[from(p1)]$capacity = 1E6
			}
			
			#Remove original pathways from list of xrefs in pathways
			pathwayXrefs.s = setdiff(pathwayXrefs.s, pathways[[p1n]])
			#Add sampled pathway to list of xrefs in pathways
			pathwayXrefs.s = union(pathwayXrefs.s, sampledPathways[[p1n]])
		}
		
		mode = ifelse(directed, "out", "all")
		nb = graph.neighborhood(pxgraph, neighborhood, p1, mode=mode)[[1]]
		p1 = V(nb)[name == p1n] #New p1 index, based on neighborhood graph
		p1x = neighbors(nb, p1)

		notInPathway = V(nb)[!(name %in% pathwayXrefs.s)]
		notInPathway = setdiff(notInPathway, V(nb)[name %in% pathwayNames])
		
		nbPws = V(nb)[name %in% pathwayNames]
		message("Number of pathways in neighborhood: ", length(nbPws))
		
		v = ifelse(value.only, NA, list())
		p1flows = rep(v, length(pathwayNames))
		names(p1flows) = pathwayNames
		
		for(p2 in nbPws) {
			if(p1 == p2) next
		
			p2n = V(nb)[p2]$name
			p2x = neighbors(nb, p2)
	
			g = createPathwayPairGraph(nb, p1, p2, p1x, p2x, notInPathway)
			
			p1g = V(g)[name == p1n]
			p2g = V(g)[name == p2n]
			
			f = graph.maxflow(g, p1g, p2g)
			if(value.only & is.list(f)) {
				f = f$value
			} else {
				f$graph = g
			}
			p1flows[[p2n]] = f
		}
		t = (proc.time() - st)[3]
		message("Elapsed: ", t, " (s)")
		
		p1flows
	}
	rownames(flows) = pathwayNames
	flows
}

limitPathwayCapacity = function(pxgraph, pathwayNames, direction = c("to", "from")) {
	doLimit = function(edges, w) {
		cap = sapply(edges, function(e)	V(pxgraph)[get.edge(pxgraph, e)[w]]$weight)
		E(pxgraph)[edges]$capacity = cap
		pxgraph
	}
	
	if("to" %in% direction) {
		pxgraph = doLimit(E(pxgraph)[to(V(pxgraph)[name %in% pathwayNames])], 1)
	}
	if("from" %in% direction) {
		pxgraph = doLimit(E(pxgraph)[from(V(pxgraph)[name %in% pathwayNames])], 2)
	}
	
	pxgraph
}

capacityPathwayXrefGraph = function(interactions, pathways, limitPwCap = c("from", "to")) {
	pxgraph = createPathwayXrefGraph(interactions, pathways)

	#Set capacity of outgoing edges fom pathway to target node weight
	if("from" %in% limitPwCap) {
		pxgraph = limitPathwayCapacity(pxgraph, names(pathways), "from")
	} else {
		E(pxgraph)[from(V(pxgraph)[name %in% names(pathways)])]$capacity = 1E6
	}
	
	#Set capacity of incoming edges fom pathway to target node weight
	if("to" %in% limitPwCap) {
		pxgraph = limitPathwayCapacity(pxgraph, names(pathways), "to")
	} else {
		E(pxgraph)[to(V(pxgraph)[name %in% names(pathways)])]$capacity = 1E6
	}
	
	pxgraph
}
