groups = c(
	"SconSa_24wks",	"SconSa_6wks",	"SconSa_24wks",
	"SconSa_6wks",	"SconSa_24wks",	"SconSa_6wks",
	"SconSa_24wks",	"SconSa_6wks",	"SconSa_24wks",
	"SconSa_6wks",	"SHR_24wks",	"SHR_6wks",
	"SHR_24wks",	"SHR_6wks",		"SHR_24wks",
	"SHR_6wks",		"SHR_24wks",	"SHR_6wks",
	"SHR_24wks",	"SHR_6wks",		"SISA_24wks",
	"SISA_6wks",	"SISA_24wks",	"SISA_6wks",
	"SISA_24wks",	"SISA_6wks",	"SISA_24wks",
	"SISA_6wks",	"SISA_24wks",	"SISA_6wks",
	"WconSa_24wks",	"WconSa_6wks",	"WconSa_24wks",
	"WconSa_6wks",	"WconSa_24wks",	"WconSa_6wks",
	"WconSa_24wks",	"WconSa_6wks",	"WconSa_24wks",
	"WconSa_6wks",	"WKY_24wks",	"WKY_6wks",
	"WKY_24wks",	"WKY_6wks",		"WKY_24wks",
	"WKY_6wks",		"WKY_24wks",	"WKY_6wks",
	"WKY_24wks",	"WKY_6wks"
)

design = model.matrix(~ -1+factor(c(
	1, 2, 1, 2, 1, 2, 1, 2, 1, 2,
	3, 4, 3, 4, 3, 4, 3, 4, 3, 4,
	5, 6, 5, 6, 5, 6, 5, 6, 5, 6,
	7, 8, 7, 8, 7, 8, 7, 8, 7, 8,
	9, 10, 9, 10, 9, 10, 9, 10, 9, 10
)))

colnames(design) = c(
	"SconSa_24wks",	"SconSa_6wks",	"SHR_24wks",
	"SHR_6wks", "SISA_24wks", "SISA_6wks", 
	"WconSa_24wks",	"WconSa_6wks", "WKY_24wks",	
	"WKY_6wks"
)

fit = lmFit(eset.gcrma.q, design)

contrast.matrix_6_WKYvsSHR = makeContrasts(
	SHR_6wks - WKY_6wks, 
	levels=design
)
fit2_6_WKYvsSHR = contrasts.fit(fit, contrast.matrix_6_WKYvsSHR)
fit2_6_WKYvsSHR = eBayes(fit2_6_WKYvsSHR)
results_6_WKYvsSHR = decideTests(fit2_6_WKYvsSHR)
png(filename="venn_6_WKYvsSHR.png", width=1024, height=1024)
vennDiagram(results_6_WKYvsSHR)
dev.off()
write.fit(fit2_6_WKYvsSHR, results=results_6_WKYvsSHR, file="fit_6_WKYvsSHR.txt")

contrast.matrix_24_WKYvsSHR = makeContrasts(
	SHR_24wks - WKY_24wks, 
	levels=design
)
fit2_24_WKYvsSHR = contrasts.fit(fit, contrast.matrix_24_WKYvsSHR)
fit2_24_WKYvsSHR = eBayes(fit2_24_WKYvsSHR)
results_24_WKYvsSHR = decideTests(fit2_24_WKYvsSHR)
png(filename="venn_24_WKYvsSHR.png", width=1024, height=1024)
vennDiagram(results_24_WKYvsSHR)
dev.off()
write.fit(fit2_24_WKYvsSHR, results=results_24_WKYvsSHR, file="fit_24_WKYvsSHR.txt")

