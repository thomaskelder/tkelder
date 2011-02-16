package org.wikipathways.stats.taskimpl;

import java.awt.Color;
import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.time.Quarter;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.ui.RectangleEdge;
import org.wikipathways.stats.Task;
import org.wikipathways.stats.TaskException;
import org.wikipathways.stats.TaskParameters;
import org.wikipathways.stats.TimeInterval;
import org.wikipathways.stats.db.PathwayInfo;
import org.wikipathways.stats.db.WPDatabase;

public class PathwayCountsBySpecies implements Task {
	public void start(WPDatabase db, TaskParameters par) throws TaskException {
		try {
			PrintWriter txtout = new PrintWriter(new File(par.getFile(TaskParameters.OUT_PATH), "pathwaycounts_species.txt"));
			txtout.println("date\tspecies\tcount");
			SimpleDateFormat dformat = new SimpleDateFormat("yyyy/MM/dd");
			
			Date start = db.getWpStart();
			TimeInterval timeInterval = new TimeInterval(start, Quarter.class);

			TimeTableXYDataset data = new TimeTableXYDataset();
			List<String> species = new ArrayList<String>(PathwayInfo.getSpecies(db));
			Collections.sort(species);
			
			RegularTimePeriod period = null;
			while((period = timeInterval.getNext()) != null) {
				System.out.println("Processing " + period);
				Date time = new Date(period.getLastMillisecond());
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
					txtout.println(
							dformat.format(time) + "\t" + s + "\t" + speciesCounts.get(s)
					);
				}
				
				db.resetConnection();
				
				System.err.println(Runtime.getRuntime().totalMemory() / 1000);
				Runtime.getRuntime().gc();
				System.err.println(Runtime.getRuntime().totalMemory() / 1000);
				System.err.println("---");
			}

			txtout.close();
			
			JFreeChart chart = ChartFactory.createStackedXYAreaChart(
					"WikiPathways growth", "Date", "Number of pathways", data, 
					PlotOrientation.VERTICAL, true, false, false);
			DateAxis axis = new DateAxis("Date");
			chart.getXYPlot().setDomainAxis(axis);
			chart.getLegend().setPosition(RectangleEdge.RIGHT);
			chart.getXYPlot().setBackgroundPaint(Color.WHITE);
			
			//Hack to set unique colors for final two species
			chart.getXYPlot().getRenderer().setSeriesPaint(species.size() - 1, new Color(0, 118, 255));
			chart.getXYPlot().getRenderer().setSeriesPaint(species.size() - 2, new Color(255, 156, 65));
			
			ChartUtilities.saveChartAsPNG(
					new File(par.getFile(TaskParameters.OUT_PATH), "pathwaycounts_species.png"), 
					chart, par.getInt(TaskParameters.GRAPH_WIDTH), par.getInt(TaskParameters.GRAPH_HEIGHT)
			);
			
		} catch(Exception e) {
			throw new TaskException(e);
		}
	}
}
