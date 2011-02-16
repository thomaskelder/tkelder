library(timecourse)

source("tdata.R")

MB.2D = mb.long(data.zero, 
    method="2D", type="robust", times=4, 
    reps = matrix(8, nrow = nrow(data.no18), ncol = 2),
    condition.grp = as.character(diet.no18),
    time.grp = as.character(time.no18),
    ref = "LF"
)

rank = rank(MB.2D$HotellingT2, ties.method='random')

plotRankedProfiles(data.no18, rank)
