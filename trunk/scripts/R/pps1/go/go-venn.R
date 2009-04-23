library(limma)
library(plotrix)

createVenn = function(goData, terms, sig = TRUE, ...) {
	genes = genes(goData)
	sigGenes = sigGenes(goData)
	data = array(0, dim = c(length(genes), length(terms)), dimnames = list(genes, terms))
	for(t in terms) {
		tg = genesInTerm(goData, whichGO = t)
		if(length(tg) > 0) {
			tg = tg[[1]]
		} else {
			tg = c()
		}
		if(sig) {
			tg = intersect(tg, sigGenes)
		}
		data[tg, t] = 1;
	}
	termNames = sapply(terms, function(t) { Term(GOTERM[[t]]) })
	vennDiagram(vennCounts(data), names = termNames, ...)
	vennCounts(data)
}

createIntersect = function(goData, terms, sig = TRUE, maxname = 10, ...) {
	genes = genes(goData)
	sigGenes = sigGenes(goData)
	data = array(0, dim = c(length(genes), length(terms)), dimnames = list(genes, terms))
	for(t in terms) {
		tg = genesInTerm(goData, whichGO = t)
		if(length(tg) > 0) {
			tg = tg[[1]]
		} else {
			tg = list()
		}
		if(sig) {
			tg = intersect(tg, sigGenes)
		}
		data[tg, t] = 1;
	}
	data = data[unique(which(data == 1, arr.ind=TRUE)[,"row"]),]
	termNames = sapply(terms, function(t) { substr(Term(GOTERM[[t]]), 0, maxname) })
	colnames(data) = termNames
	altIntersectDiagram(data, ...)
}

altIntersectDiagram = function (x, pct = FALSE, show.nulls = FALSE, xnames = NULL, 
    namesep = "-", mar = c(0, 0, 3, 0), main = "Intersection Diagram", 
    col = NULL, minspacing = 0.1) 
{
    if (is.matrix(x) || is.data.frame(x)) 
        x <- makeIntersectList(x)
    if (!match(class(x), "intersectList", 0)) 
        stop("x must be a matrix, data frame or intersectList")
    
    #Filter out empty sets
    for(i in 1:length(x)) {
    	print(i)
    	print(i[i != 0])
    	x[[i]] = x[[i]][x[[i]] != 0]
    }
    oldmar <- par("mar")
    par(mar = mar)
    lenx <- length(x) - 1
    xtotal <- x[[length(x)]]
    if (is.null(xnames)) 
        xnames <- names(x[[1]])
    if (is.null(xnames)) 
        xnames <- LETTERS[1:lenx]
    if (is.null(col)) 
        col <- c(rainbow(length(x) - 1), NA)
    if (length(col) < length(x)) 
        col <- rep(col, length.out = length(x))
    listsums <- sapply(x, sum)
    horizmax <- max(listsums)
    xsum <- 0
    plot(0, xlim = c(0, horizmax * (1 + minspacing)), ylim = c(0, 
        lenx + show.nulls), main = main, xlab = "", ylab = "", 
        type = "n", axes = FALSE)
    for (comb in 1:lenx) {
        lenxcomb <- length(x[[comb]])
        if(lenxcomb > 0) {
		    xsum <- xsum + sum(x[[comb]])
		    rowmat <- combn(xnames, comb)
		    blocknames <- pasteCols(rowmat, namesep)
		    gap <- (horizmax * (1 + minspacing) - sum(x[[comb]]))/lenxcomb
		    startx <- gap/2
		    for (intersect in 1:lenxcomb) {
		        cellqnt <- ifelse(pct, paste(round(100 * x[[comb]][intersect]/xtotal, 
		            1), "%", sep = ""), x[[comb]][intersect])
		        if (x[[comb]][intersect] > 0) {
		            if (!is.na(col[1])) {
		              lencol <- length(col)
		              xinc <- x[[comb]][intersect]/comb
		              slice <- 1
		              leftx <- startx
		              for (bn in 1:lencol) {
		                if (length(grep(xnames[bn], blocknames[intersect], 
		                  fixed = TRUE))) {
		                  polygon(c(leftx, leftx, leftx + xinc, leftx + 
		                    xinc), c(lenx + show.nulls - comb + 0.1, 
		                    lenx + show.nulls - comb + 0.9, lenx + 
		                      show.nulls - comb + 0.9, lenx + show.nulls - 
		                      comb + 0.1), border = NA, col = col[bn])
		                  slice <- slice + 1
		                  leftx <- leftx + xinc
		                }
		              }
		            }
		            rect(startx, lenx + show.nulls - comb + 0.1, 
		              startx + x[[comb]][intersect], lenx + show.nulls - 
		                comb + 0.9)
		        boxed.labels(startx + x[[comb]][intersect]/2, lenx + 
		            show.nulls - comb + 0.5, paste(blocknames[intersect], 
		            cellqnt, sep = "\n"))
		        startx <- startx + x[[comb]][intersect] + gap
		        }
		    }
		}
    }
    if (show.nulls) {
        nonset <- as.numeric(xtotal - xsum)
        leftx <- sum(par("usr")[1:2])/2 - nonset/2
        polygon(c(leftx, leftx, leftx + nonset, leftx + nonset), 
            c(0.1, 0.9, 0.9, 0.1), col = NA)
        if (pct) 
            nonsetpc <- paste(round(100 * nonset/xtotal, 1), 
                "%", sep = "")
        boxed.labels(leftx + nonset/2, 0.5, paste("Non-members", 
            nonsetpc, sep = "\n"), col = col[length(col)])
    }
    par(mar = oldmar)
}

