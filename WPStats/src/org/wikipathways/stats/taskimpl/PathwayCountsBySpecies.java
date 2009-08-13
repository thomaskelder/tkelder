package org.wikipathways.stats.taskimpl;

import static org.wikipathways.stats.TaskParameters.GRAPH_HEIGHT;
import static org.wikipathways.stats.TaskParameters.GRAPH_WIDTH;
import static org.wikipathways.stats.TaskParameters.OUT_PATH;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
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
import org.wikipathways.stats.db.PathwayInfo;
import org.wikipathways.stats.db.WPDatabase;

public class PathwayCountsBySpecies implements Task {
	public void start(WPDatabase db, TaskParameters par) throws TaskException {
		try {
			Date start = db.getWpStart();
			TimeInterval timeInterval = new TimeInterval(start, Month.class);

			TimeTableXYDataset data = new TimeTableXYDataset();
			TreeSet<String> species = new TreeSet<String>(PathwayInfo.getSpecies(db));
			
			RegularTimePeriod period = null;
			while((period = timeInterval.getNext()) != null) {
				System.out.println("Processing " + period);
				Date time = new Date(period.getMiddleMillisecond());
				Set<PathwayInfo> snapshot = PathwayInfo.getSnapshot(db, time);
				Map<String, Integer> speciesCounts = new HashMap<String, Integer>();
				for(String s : species) speciesCounts.put(s, 0);
				speciesCounts.put("unspecified", 0);
				
				for(PathwayInfo i : snapshot) {
					String s = i.getSpecies();
					if(s == null) s = "unspecified";
					speciesCounts.put(s, speciesCounts.get(s) + 1);
				}
				for(String s : species) {
					data.add(period, speciesCounts.get(s), s);
				}
			}

			JFreeChart chart = ChartFactory.createStackedXYAreaChart(
					"WikiPathways growth", "Date", "Number of pathways", data, 
					PlotOrientation.VERTICAL, true, false, false);
			DateAxis axis = new DateAxis("Date");
			chart.getXYPlot().setDomainAxis(axis);
			
			ChartUtilities.saveChartAsPNG(
					new File(par.getFile(OUT_PATH), "pathwaycounts_species.png"), 
					chart, par.getInt(GRAPH_WIDTH), par.getInt(GRAPH_HEIGHT)
			);
			
		} catch(Exception e) {
			throw new TaskException(e);
		}
	}
}
