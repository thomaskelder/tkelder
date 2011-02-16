############################################################################
## Produces statistics on the interaction networks from different sources ##
############################################################################

library(igraph)

src = "scripts/"
source(paste(src, "ShortestPath.R", sep=""))

mergeGraphs = function(gmlPaths) {
	interactions = graph.empty()
	
	files = listFiles(gmlPaths, "gml")
	for(f in files) {
		message("Reading ", f)
		g = read.graph(f, "gml")
		V(g)$name = V(g)$identifier

		#Add nodes
		newv = V(g)[!(name %in% V(interactions)$name)]
		attr = list()
		newnames = V(g)[newv]$name
		attr$name = newnames
		newlabels = V(g)[newv]$Label
		if(!is.null(newlabels)) attr$label = newlabels
		interactions = add.vertices(
			interactions, length(newv), attr = attr
		)

		#Add edges
		edges = c()
		oldEdgeIds = c()
		
		for(n1 in V(g)) {
			if(n1 %% 100 == 0) message(n1, " out of ", vcount(g))

			from = E(g)[from(n1)]
			ni1 = V(interactions)[name == V(g)[n1]$name]

			edges = c(edges, sapply(from, function(e) {
				n2 = get.edge(g, e)[2]
				ni2 = V(interactions)[name == V(g)[n2]$name]
				c(ni1, ni2)
			}))
			oldEdgeIds = c(oldEdgeIds, from)
		}
		attr = list()
		for(n in list.edge.attributes(g)) {
			attr[[n]] = get.edge.attribute(g, n, oldEdgeIds)
		}
		attr$SourceFile = rep(basename(f), length(oldEdgeIds))
		interactions = add.edges(interactions, edges, attr = attr)
	}
	interactions
}

interactions = mergeGraphs("input/interactions/mouse-directed")
save.image("interactions.RData")

edges = get.edgelist(interactions)

etxt = paste(edges[,1], edges[,2], sep='.')
etxt.inv = paste(edges[,2], edges[,1], sep='.')
etxt.ud = intersect(etxt, etxt.inv)
directed = which(!(etxt %in% etxt.ud)) - 1
undirected = which(etxt %in% etxt.ud) - 1
freq = table(etxt)

## Which multiple edges are both in psimi and string
s = simplify(interactions, edge.attr.comb = list(SourceFile = function(a) paste(sort(a), collapse='|'), Interaction = function(a) paste(sort(a), collapse='|')))
table(E(s)$SourceFile)
table(E(s)$Interaction)
