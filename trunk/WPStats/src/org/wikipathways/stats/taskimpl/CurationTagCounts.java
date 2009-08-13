package org.wikipathways.stats.taskimpl;

import static org.wikipathways.stats.TaskParameters.GRAPH_HEIGHT;
import static org.wikipathways.stats.TaskParameters.GRAPH_WIDTH;
import static org.wikipathways.stats.TaskParameters.OUT_PATH;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.time.Month;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeTableXYDataset;
import org.wikipathways.stats.Task;
import org.wikipathways.stats.TaskException;
import org.wikipathways.stats.TaskParameters;
import org.wikipathways.stats.TimeInterval;
import org.wikipathways.stats.db.CurationTag;
import org.wikipathways.stats.db.PathwayInfo;
import org.wikipathways.stats.db.WPDatabase;

public class CurationTagCounts implements Task {
	public void start(WPDatabase db, TaskParameters par) throws TaskException {
		try {
			Date start = db.getWpStart();
			TimeInterval timeInterval = new TimeInterval(start, Month.class);

			TimeTableXYDataset data = new TimeTableXYDataset();
			TimeTableXYDataset dataPerPage = new TimeTableXYDataset(); //Divide by nr pathways
			
			RegularTimePeriod period = null;
			while((period = timeInterval.getNext()) != null) {
				System.out.println("Processing " + period);
				Date time = new Date(period.getMiddleMillisecond());
				Set<String> tagTypes = new TreeSet<String>(CurationTag.getTagTypes(db, time));
				Collection<CurationTag> snapshot = CurationTag.getSnapshot(db, time);
				Collection<PathwayInfo> pathways = PathwayInfo.getSnapshot(db, time);
				
				//Remove any tags that are applied to a deleted pathway
				Set<Integer> pageIds = new HashSet<Integer>();
				for(PathwayInfo i : pathways) pageIds.add(i.getPageId());
				Set<CurationTag> remove = new HashSet<CurationTag>();
				for(CurationTag t : snapshot) if(!pageIds.contains(t.getPageId())) remove.add(t);
				snapshot.removeAll(remove);
				
				Map<String, Integer> tagCounts = new HashMap<String, Integer>();
				for(String s : tagTypes) {
					tagCounts.put(s, 0);
				}
				
				for(CurationTag tag : snapshot) {
					String type = tag.getType();
					tagCounts.put(type, tagCounts.get(type) + 1);
				}
				for(String t : tagTypes) {
					data.add(period, tagCounts.get(t), t);
					dataPerPage.add(period, (double)tagCounts.get(t) / pathways.size(), t);
				}
			}

			JFreeChart chart = ChartFactory.createStackedXYAreaChart(
					"Curation tag counts", "Date", "Number of curation tags", data, 
					PlotOrientation.VERTICAL, true, false, false);
			ChartUtilities.saveChartAsPNG(
					new File(par.getFile(OUT_PATH), "curationtagcounts.png"), 
					chart, par.getInt(GRAPH_WIDTH), par.getInt(GRAPH_HEIGHT)
			);
			
			chart = ChartFactory.createTimeSeriesChart(
					"Curation tags per page", "Date", "Number of curation tags per page", dataPerPage, 
					true, false, false);
			ChartUtilities.saveChartAsPNG(
					new File(par.getFile(OUT_PATH), "curationtagcounts_perpage.png"), 
					chart, par.getInt(GRAPH_WIDTH), par.getInt(GRAPH_HEIGHT)
			);
			
		} catch(Exception e) {
			throw new TaskException(e);
		}
	}
}
