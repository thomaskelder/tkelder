package org.wikipathways.stats.taskimpl;

import static org.wikipathways.stats.TaskParameters.GRAPH_HEIGHT;
import static org.wikipathways.stats.TaskParameters.GRAPH_WIDTH;
import static org.wikipathways.stats.TaskParameters.OUT_PATH;

import java.io.File;
import java.util.Date;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.Week;
import org.wikipathways.stats.Task;
import org.wikipathways.stats.TaskException;
import org.wikipathways.stats.TaskParameters;
import org.wikipathways.stats.TimeInterval;
import org.wikipathways.stats.db.PathwayInfo;
import org.wikipathways.stats.db.WPDatabase;

public class PathwayCounts implements Task {
	public void start(WPDatabase db, TaskParameters par) throws TaskException {
		try {
			Date start = db.getWpStart();
			TimeInterval timeInterval = new TimeInterval(start, Week.class);

			TimeSeriesCollection data = new TimeSeriesCollection();
			TimeSeries tsPw = new TimeSeries("Number of pathways");

			RegularTimePeriod period = null;
			while((period = timeInterval.getNext()) != null) {
				System.out.println("Processing " + period);
				Date time = new Date(period.getMiddleMillisecond());
				tsPw.add(period, PathwayInfo.getSnapshot(db, time).size());
				
				System.err.println(Runtime.getRuntime().totalMemory() / 1000);
				Runtime.getRuntime().gc();
				System.err.println(Runtime.getRuntime().totalMemory() / 1000);
				System.err.println("---");
			}
			
			data.addSeries(tsPw);
			
			JFreeChart chart = ChartFactory.createTimeSeriesChart(
					"WikiPathways growth", "Date", "Number of pathways", data, false, true, false);
			
			ChartUtilities.saveChartAsPNG(
					new File(par.getFile(OUT_PATH), "pathwaycounts.png"), 
					chart, par.getInt(GRAPH_WIDTH), par.getInt(GRAPH_HEIGHT)
			);
			
		} catch(Exception e) {
			throw new TaskException(e);
		}
	}

}
