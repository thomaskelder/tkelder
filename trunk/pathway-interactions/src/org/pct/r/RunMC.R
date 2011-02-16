library(doMC)
registerDoMC()

message("Running script...")
src = "scripts/"
source(paste(src, "RunShortestPath.R", sep=""))

message("Running script...")
source(script)

## Save the data
message("Saving the data")
save.image(file=out)

closeCluster(cl)
mpi.quit()
