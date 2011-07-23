data = read.delim("views_vs_edits.txt", sep="\t", stringsAsFactors = F)

library(ggplot2)

## Average by number of authors
authors = c(1, 2, 3, 4, 5, 6)
avgByAuthors = t(sapply(authors, function(nr) {
  rows = data$authors == nr
  if(nr == max(authors)) rows = data$authors >= nr
  avgEdits = mean(data$edits[rows])
  avgViews = mean(data$views[rows])
  nrPws = sum(rows)
  c(nr, avgEdits, avgViews, nrPws)
}))
colnames(avgByAuthors) = c("nrAuthors", "avgEdits", "avgViews", "nrPws")
avgByAuthors = as.data.frame(avgByAuthors)

lAuthors = as.character(authors)
lAuthors[length(lAuthors)] = paste(">=", lAuthors[length(lAuthors)])

p = ggplot(avgByAuthors, aes(avgViews, avgEdits)) + 
  geom_point(aes(colour = factor(nrAuthors, labels=lAuthors), size = nrPws)) +
  scale_area(to = c(5, 20), breaks = c(100, 300, 600), name = "Number of pathways") +
  scale_colour_hue(name = "Number of unique authors") +
  scale_x_continuous("Average number of views") +
  scale_y_continuous('Average number of edits')

print(p + opts(legend.position = "none"))
ggsave("views_vs_edits_avg.svg")
print(p)
ggsave("views_vs_edits_avg_legend.svg")
