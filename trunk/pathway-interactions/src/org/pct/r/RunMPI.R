##Run this on each node to install the required packages
#install.packages(c("foreach", "doMPI", "igraph", "hash"))

args <- commandArgs(TRUE)
script = args[1]
out = args[2]

print(c(script, out))

message("Setting up the cluster")
library(doMPI)

cl = startMPIcluster(verbose=T)
registerDoMPI(cl)
print(mpi.comm.size(0))

message("Running script...")
source(script)

## Save the data
message("Saving the data")
save.image(file=out)

closeCluster(cl)
mpi.quit()
