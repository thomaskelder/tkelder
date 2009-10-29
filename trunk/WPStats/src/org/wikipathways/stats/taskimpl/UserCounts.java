package org.wikipathways.stats.taskimpl;

import static org.wikipathways.stats.TaskParameters.GRAPH_HEIGHT;
import static org.wikipathways.stats.TaskParameters.GRAPH_WIDTH;
import static org.wikipathways.stats.TaskParameters.OUT_PATH;

import java.io.File;
import java.util.Collection;
import java.util.Date;

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
import org.wikipathways.stats.db.User;
import org.wikipathways.stats.db.WPDatabase;

public class UserCounts implements Task {
	public void start(WPDatabase db, TaskParameters par) throws TaskException {
		try {
			Date start = db.getWpStart();
			TimeInterval timeInterval = new TimeInterval(start, Month.class);

			TimeTableXYDataset usersData = new TimeTableXYDataset();
			
			RegularTimePeriod period = null;
			while((period = timeInterval.getNext()) != null) {
				System.out.println("Processing " + period);
				//Date from = new Date(period.getFirstMillisecond());
				Date to = new Date(period.getMiddleMillisecond());
				
				Collection<User> users = User.getSnapshot(db, start, to);
				
				int active = 0;
				for(User u : users) {
					if(u.getEditCount(db) > 10) {
						active++;
					}
				}
				
				usersData.add(period, active, "Active users");
				usersData.add(period, users.size() - active, "Registered users");
			}
			
			JFreeChart chart = ChartFactory.createStackedXYAreaChart(
					"WikiPathways users", "Date", "Number of users", usersData, 
					PlotOrientation.VERTICAL, true, false, false);
			DateAxis axis = new DateAxis("Date");
			chart.getXYPlot().setDomainAxis(axis);
			ChartUtilities.saveChartAsPNG(
					new File(par.getFile(OUT_PATH), "usercounts.png"), 
					chart, par.getInt(GRAPH_WIDTH), par.getInt(GRAPH_HEIGHT)
			);
			
		} catch(Exception e) {
			throw new TaskException(e);
		}
	}
}
