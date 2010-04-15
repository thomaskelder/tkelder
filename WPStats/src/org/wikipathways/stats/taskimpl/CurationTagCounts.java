package org.wikipathways.stats.taskimpl;

import static org.wikipathways.stats.TaskParameters.GRAPH_HEIGHT;
import static org.wikipathways.stats.TaskParameters.GRAPH_WIDTH;
import static org.wikipathways.stats.TaskParameters.OUT_PATH;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
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
import org.jfree.chart.axis.DateAxis;
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
			run(db, par, "curation", start, new Date(), Month.class, null);
		} catch(Exception e) {
			throw new TaskException(e);
		}
	}
	
	static void run(WPDatabase db, TaskParameters par, String filePrefix, Date start, Date end, Class<? extends RegularTimePeriod> interval, Set<String> tagFilter) throws SQLException, ParseException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		TimeInterval timeInterval = new TimeInterval(start, end, interval);

		TimeTableXYDataset data = new TimeTableXYDataset();
		TimeTableXYDataset dataPerPage = new TimeTableXYDataset(); //Divide by nr pathways
		
		RegularTimePeriod period = null;
		while((period = timeInterval.getNext()) != null) {
			System.out.println("Processing " + period);
			Date time = new Date(period.getMiddleMillisecond());
			Set<String> tagTypes = new TreeSet<String>(CurationTag.getTagTypes(db, time));
			if(tagFilter != null) {
				tagTypes.retainAll(tagFilter);
			}
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
				if(!tagTypes.contains(type)) continue;
				tagCounts.put(type, tagCounts.get(type) + 1);
			}
			for(String t : tagTypes) {
				System.out.println("\t" + t + ": " + tagCounts.get(t));
				data.add(period, tagCounts.get(t), t);
				dataPerPage.add(period, (double)(tagCounts.get(t) / pathways.size()) * 100, t);
			}
			
			db.closePsts();
		}

		JFreeChart chart = ChartFactory.createTimeSeriesChart(
				"Number of tagged pathways", "Time", "Nr of pathways with tag", data, 
				true, false, false);
		DateAxis axis = new DateAxis("Time");
		chart.getXYPlot().setDomainAxis(axis);
		ChartUtilities.saveChartAsPNG(
				new File(par.getFile(OUT_PATH), filePrefix + "_tagcounts.png"), 
				chart, par.getInt(GRAPH_WIDTH), par.getInt(GRAPH_HEIGHT)
		);
		
		chart = ChartFactory.createTimeSeriesChart(
				"Percentage of pathways tagged", "Time", "% pathways tagged", dataPerPage, 
				true, false, false);
		axis = new DateAxis("Time");
		chart.getXYPlot().setDomainAxis(axis);
		ChartUtilities.saveChartAsPNG(
				new File(par.getFile(OUT_PATH), filePrefix + "tagcounts_perpage.png"), 
				chart, par.getInt(GRAPH_WIDTH), par.getInt(GRAPH_HEIGHT)
		);
	}
}
