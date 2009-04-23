dataFile = "/home/thomas/projects/pps/filtered_data/Combined_total.txt"
outPath = "/home/thomas/projects/pps/filtered_data/oriogen/profile_plots/"

data = read.delim(dataFile, row.names=1, na.strings = "NaN")

tissues = c("Muscle", "WAT", "Liver")
profiles = 1:8

#for(profile in profiles) {
#	for(tis in tissues) {
#		in_profile = which(data[,paste(tis, "_oriogen_profile", sep="")] == profile)

#		cols = c("_t0_avg", "_t1_avg", "_t6_avg", "_t9_avg", "_t12_avg")
#		for(i in 1:length(cols)) {
#			cols[i] = paste(tis, cols[i], sep="")
#		}

#		time = c(0, 1, 6, 9, 12)
#	
#		f = paste(outPath, tis, "_", profile, ".png", sep="")
#		message("Plotting to ", f)
#		png(file = f, width = 800, height = 800)
#	
#		plot(x=time, y = data[in_profile[1], cols], type="l", xlim = range(time), ylim = range(data[in_profile, cols], na.rm = TRUE))
#		for(i in 2:length(in_profile)) {
#			lines(x=time, y = data[in_profile[i], cols])
#		}
#	
#		dev.off()
#	}
#}

p.value = 0.005
for(tis in tissues) {
		in_anova = which(data[,paste("anova_pvalue_", tis, sep="")] < p.value)
		
		cols = c("_t0_avg", "_t1_avg", "_t6_avg", "_t9_avg", "_t12_avg")
		for(i in 1:length(cols)) {
			cols[i] = paste(tis, cols[i], sep="")
		}

		time = c(0, 1, 6, 9, 12)
	
		f = paste(outPath, tis, "_", p.value, ".png", sep="")
		message("Plotting to ", f)
		png(file = f, width = 800, height = 800)
	
		plot(x=time, y = data[in_anova[1], cols], type="l", xlim = range(time), ylim = range(data[in_anova, cols], na.rm = TRUE))
		for(i in 2:length(in_anova)) {
			lines(x=time, y = data[in_anova[i], cols])
		}
	
		dev.off()
}
