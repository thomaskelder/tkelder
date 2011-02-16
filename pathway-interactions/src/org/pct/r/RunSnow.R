##Run this on each node to install the required packages
#install.packages(c("foreach", "doSNOW", "igraph", "hash"))

message("Setting up the cluster")
library(doSNOW)

nbx = c(
	"nbx1", 
	"nbx8", 
	"nbx7"
#	"nbx12", #2.9.1
#	"nbx14", #2.9.1: /home/thomaskelder/R/i686-pc-linux-gnu-library/2.9
#	"nbx15"
#	"nbx17", #2.9.1
#	"nbx19"
)

l2.11 = "/home/thomaskelder/R/i686-pc-linux-gnu-library/2.11"
l2.9 = "/home/thomaskelder/R/i686-pc-linux-gnu-library/2.9"

libbase = c(
	l2.11,
	l2.11, 
	l2.11
#	l2.9,
#	l2.9, 
#	l2.11
#	l2.9,
#	l2.11
)

cores = 4 #Nr of cores per nbx
nbx = rep(nbx, cores) 
libbase = rep(libbase, cores)

nbxhosts = paste("thomaskelder@", nbx, ".nugo.org", sep="")

hosts = apply(cbind(nbxhosts, libbase), 1, function(x) { 
	list(host = x[1], rscript = "/usr/bin/Rscript", outfile="", 
		#port="35100",
		#master="84.27.101.8",
		master="137.120.228.45",
		scriptdir=paste(x[2], "snow", sep=""),
		snowlib=x[2]
	)
})

cl <- makeCluster(hosts, type = "SOCK")
registerDoSNOW(cl)

args <- commandArgs(TRUE)
script = args[1]
out = args[2]

message("Running script ", script)
source(script)

## Save the data
message("Saving the data")
save.image(file=out)

stopCluster(cl)
