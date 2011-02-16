package org.wikipathways.stats.taskimpl;

import static org.wikipathways.stats.TaskParameters.GRAPH_HEIGHT;
import static org.wikipathways.stats.TaskParameters.GRAPH_WIDTH;
import static org.wikipathways.stats.TaskParameters.OUT_PATH;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Month;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeTableXYDataset;
import org.wikipathways.stats.Task;
import org.wikipathways.stats.TaskException;
import org.wikipathways.stats.TaskParameters;
import org.wikipathways.stats.TimeInterval;
import org.wikipathways.stats.db.CurationTag;
import org.wikipathways.stats.db.User;
import org.wikipathways.stats.db.WPDatabase;

public class UserCounts implements Task {
	public void start(WPDatabase db, TaskParameters par) throws TaskException {
		try {
			Date start = db.getWpStart();
			//Date start = WPDatabase.timestampToDate("20100101000000");
			Calendar cal = Calendar.getInstance();
			cal.clear();
		    cal.set(Calendar.YEAR, 2011);
		    cal.set(Calendar.MONTH, 1);
		    cal.set(Calendar.DATE, 1);
		    cal.set(Calendar.HOUR_OF_DAY, 0);
			Date end = cal.getTime();
			
			TimeInterval timeInterval = new TimeInterval(start, end, Month.class);
			PrintWriter txtout = new PrintWriter(new File(par.getFile(TaskParameters.OUT_PATH), "usercounts_interval.txt"));
			txtout.println("date\ttype\tcount");
			SimpleDateFormat dformat = new SimpleDateFormat("yyyy/MM/dd");
			
			TimeTableXYDataset usersData = new TimeTableXYDataset();
			TimeTableXYDataset activeUsersData = new TimeTableXYDataset();
			DefaultCategoryDataset activeIntervalData = new DefaultCategoryDataset();
			
			Collection<CurationTag> tags = CurationTag.getLatest(db);
			Set<Integer> isTest = new HashSet<Integer>();
			for(CurationTag t : tags) {
				if("Tutorial".equals(t.getType())) isTest.add(t.getPageId());
			}

			RegularTimePeriod period = null;
			int i = 0;
			while((period = timeInterval.getNext()) != null) {
				System.out.println("Processing " + period);
				//Date from = new Date(period.getFirstMillisecond());
				Date to = new Date(period.getLastMillisecond());
				Date from = new Date(period.getFirstMillisecond());
				Collection<User> users = User.getSnapshot(db, to);
				
				int everActive = 0;
				int intervalActive = 0;
				for(User u : users) {
					List<Integer> pageEdits= u.getEditPages(db, to);
					pageEdits.removeAll(isTest);
					List<Integer> pageEditsInterval = u.getEditPages(db, from, to);
					pageEditsInterval.removeAll(isTest);
					
					if(pageEdits.size() > 0) everActive++;
					if(pageEditsInterval.size() > 0) intervalActive++;
				}
				
				usersData.add(period, everActive, "Active users");
				usersData.add(period, users.size() - everActive, "Registered users");
				
				activeUsersData.add(period, intervalActive, "1. Active users in month");
				//activeUsersData.add(period, intervalActive - intervalActiveNoTest, "1. Testing / tutorial users");
				activeUsersData.add(period, everActive - intervalActive, "2. Cumulative active users");
				
				activeIntervalData.addValue(intervalActive, "Active users", period);
				
				Date time = new Date(period.getLastMillisecond());
				txtout.println(
						dformat.format(time) + "\tCumulative unique active users\t" + everActive
				);
				txtout.println(
						dformat.format(time) + "\tUsers active in month\t" + intervalActive
				);
				
				if(i++ % 10 == 0) db.resetConnection();
			}
			
			txtout.close();
			
			JFreeChart chart = ChartFactory.createStackedXYAreaChart(
					"WikiPathways users", "Date", "Number of users", usersData, 
					PlotOrientation.VERTICAL, true, false, false);
			DateAxis axis = new DateAxis("Date");
			chart.getXYPlot().setDomainAxis(axis);
			ChartUtilities.saveChartAsPNG(
					new File(par.getFile(OUT_PATH), "usercounts.png"), 
					chart, par.getInt(GRAPH_WIDTH), par.getInt(GRAPH_HEIGHT)
			);
			
			chart = ChartFactory.createStackedXYAreaChart(
					"Active WikiPathways users", "Date", "Number of active users", activeUsersData, 
					PlotOrientation.VERTICAL, true, false, false);
			axis = new DateAxis("Date");
			chart.getXYPlot().setDomainAxis(axis);
			ChartUtilities.saveChartAsPNG(
					new File(par.getFile(OUT_PATH), "usercounts-active.png"), 
					chart, par.getInt(GRAPH_WIDTH), par.getInt(GRAPH_HEIGHT)
			);
			
			chart = ChartFactory.createStackedBarChart(
					"Active WikiPathways users", "Date", "Number of active users", activeIntervalData, 
					PlotOrientation.VERTICAL, false, false, false);
			CategoryAxis xaxis = (CategoryAxis)chart.getCategoryPlot().getDomainAxis();
			xaxis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_90);
			ChartUtilities.saveChartAsPNG(
					new File(par.getFile(OUT_PATH), "usercounts-interval.png"), 
					chart, par.getInt(GRAPH_WIDTH), par.getInt(GRAPH_HEIGHT)
			);
		} catch(Exception e) {
			throw new TaskException(e);
		}
	}
}
