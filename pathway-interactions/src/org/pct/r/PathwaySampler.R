##############################################################
## Several functions to sample nodes from a network         ##
## by taking into account their degree (or graph.strength).	##
##############################################################
library(igraph)
library(hash)

## Create a set of sampled pathways based on the node
## indices of the original pathways (ipathways) in the
## graph (g) and the degreeSampler that contains samples
## of nodes by their degree.
samplePathways = function(ipathways, g, degreeSampler, strength = F, ...) {
	if(strength) degrees = graph.strength(g, ...)
	else degrees = degree(g, ...) 
	
	message("sampling pathways with graph.strength = ", strength)
	lapply(ipathways, function(p) {
		sapply(p, function(x) {
			d = degrees[x + 1]
			s = degreeSampler[[as.character(d)]]
			if(is.null(s) || length(s) < 2) {
				x #Unable to pick random, return same
			} else {
				r = x
				while(r == x) {
					r = s[sample(1:length(s), 1)]
				}
			}
			r
		})
	})
}

## Create a hash that contains a sample
## of nodes from the graph with similar degree.
## The key will be the degree (or graph.strength),
## the value will be the vector of sampled nodes.
degreeSampler = function(g, fuzzy = 0.1, minSize = 50, useNames = F, strength = F, ...) {
	message("sampling pathways with graph.strength = ", strength)
	
	vnames = V(g)$name
	
	if(strength) degrees = graph.strength(g, ...)
	else degrees = degree(g, ...)

	nd = sort(unique(degrees))
	byDegree = lapply(nd, function(d) {
		which(degrees == d) - 1
	})
	names(byDegree) = as.character(nd)
	
	sampleNodes = hash()
	for(d in nd) {
		low = d * (1 - fuzzy);
		high = d * (1 + fuzzy);
		sub = as.character(nd[which(nd >= low & nd <= high)])
	
		similar = unlist(byDegree[sub])
	
		if(length(similar) < minSize) {
			head = as.character(sort(nd[which(nd < d)], T)) 
			tail = as.character(nd[which(nd > d)])
			if(length(head) > length(tail)) {
				large = head
				small = tail
			} else {
				large = tail
				small = head
			}				
			both = cbind(large, c(small, rep(NA, length(large) - length(small))))
			mix = rep(NA, nrow(both) * 2)
			mix[seq(1, length(mix), 2)] = both[,1]
			mix[seq(2, length(mix), 2)] = both[,2]
			mix = mix[!is.na(mix)]
			for(md in mix) {
				ns = byDegree[[md]]
				take = min(length(ns), minSize - length(similar))
				if(take > 0) {
					similar = c(similar, ns[1:take])
				}
				if(length(similar) >= minSize) break
			}
		}
		if(useNames) similar = vnames[similar + 1]
		else similar = as.numeric(similar)
		sampleNodes[[as.character(d)]] = similar
	}
	sampleNodes
}

