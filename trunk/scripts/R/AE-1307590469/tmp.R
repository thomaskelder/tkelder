contrast.matrix_6vs24_WKYvsSHR = makeContrasts(
	SHR_6wks - WKY_6wks, 
	SHR_24wks - WKY_24wks,
	levels=design
)
fit2_6vs24_WKYvsSHR = contrasts.fit(fit, contrast.matrix_6vs24_WKYvsSHR)
fit2_6vs24_WKYvsSHR = eBayes(fit2_6vs24_WKYvsSHR)
results_6vs24_WKYvsSHR = decideTests(fit2_6vs24_WKYvsSHR)
png(filename="venn_6vs24_WKYvsSHR.png", width=1024, height=1024)
vennDiagram(results_6vs24_WKYvsSHR)
dev.off()

contrast.matrix_6vs24_SHRvsSconSa = makeContrasts(
	SconSa_6wks - SHR_6wks, 
	SconSa_24wks - SHR_24wks,
	levels=design
)
fit2_6vs24_SHRvsSconSa = contrasts.fit(fit, contrast.matrix_6vs24_SHRvsSconSa)
fit2_6vs24_SHRvsSconSa = eBayes(fit2_6vs24_SHRvsSconSa)
results_6vs24_SHRvsSconSa = decideTests(fit2_6vs24_SHRvsSconSa)
png(filename="venn_6vs24_SHRvsSconSa.png", width=1024, height=1024)
vennDiagram(results_6vs24_SHRvsSconSa)
dev.off()

contrast.matrix_6vs24_WKYvsWconSa = makeContrasts(
	WconSa_6wks - WKY_6wks, 
	WconSa_24wks - WKY_24wks,
	levels=design
)
fit2_6vs24_WKYvsWconSa = contrasts.fit(fit, contrast.matrix_6vs24_WKYvsWconSa)
fit2_6vs24_WKYvsWconSa = eBayes(fit2_6vs24_WKYvsWconSa)
results_6vs24_WKYvsWconSa = decideTests(fit2_6vs24_WKYvsWconSa)
png(filename="venn_6vs24_WKYvsWconSa.png", width=1024, height=1024)
vennDiagram(results_6vs24_WKYvsWconSa)
dev.off()

contrast.matrix_6wks_SHRvsSconSavsSISA = makeContrasts(
	SconSa_6wks - SHR_6wks, 
	SISA_6wks - SHR_6wks,
	SISA_6wks - SconSa_6wks,
	levels=design
)
fit2_6wks_SHRvsSconSavsSISA = contrasts.fit(fit, contrast.matrix_6wks_SHRvsSconSavsSISA)
fit2_6wks_SHRvsSconSavsSISA = eBayes(fit2_6wks_SHRvsSconSavsSISA)
results_6wks_SHRvsSconSavsSISA = decideTests(fit2_6wks_SHRvsSconSavsSISA)
png(filename="venn_6wks_SHRvsSconSavsSISA.png", width=1024, height=1024)
vennDiagram(results_6wks_SHRvsSconSavsSISA)
dev.off()

contrast.matrix_24wks_SHRvsSconSavsSISA = makeContrasts(
	SconSa_24wks - SHR_24wks, 
	SISA_24wks - SHR_24wks,
	SISA_24wks - SconSa_24wks,
	levels=design
)
fit2_24wks_SHRvsSconSavsSISA = contrasts.fit(fit, contrast.matrix_24wks_SHRvsSconSavsSISA)
fit2_24wks_SHRvsSconSavsSISA = eBayes(fit2_24wks_SHRvsSconSavsSISA)
results_24wks_SHRvsSconSavsSISA = decideTests(fit2_24wks_SHRvsSconSavsSISA)
png(filename="venn_24wks_SHRvsSconSavsSISA.png", width=1024, height=1024)
vennDiagram(results_24wks_SHRvsSconSavsSISA)
dev.off()


