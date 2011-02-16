############################################################################
## Plots T statistic vs weight to illustrate the transformation           ##
## (Supp. Figure in manuscript).                                          ##
############################################################################

#Use real data
#t = "HFvsLFt0_T"
#file = "input/weights/pps2.2log.stats.mapped.txt"
#rawWeights = read.delim(file, as.is=T)
#tstat = abs(rawWeights[,t]) #Take absolute T statistic

#Use generated data
tstat = seq(0, 10, 0.1)

## Apply soft threshold (sigmoid)
sigm = function(x, a = 2, u = 3) {
	1/(1 + exp(-a*(x - u)))
}

weight = 1 - sigm(tstat)

svg("output/transformation.svg")
plot(tstat, weight, type='l', lwd=3, xlab = 'T statistic', ylab = 'weight',
	main='Transformation of T statistic to weight')
dev.off()
