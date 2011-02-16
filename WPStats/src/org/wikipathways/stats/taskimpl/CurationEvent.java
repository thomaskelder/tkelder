package org.wikipathways.stats.taskimpl;

import static org.wikipathways.stats.TaskParameters.GRAPH_HEIGHT;
import static org.wikipathways.stats.TaskParameters.GRAPH_WIDTH;
import static org.wikipathways.stats.TaskParameters.OUT_PATH;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
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
import org.jfree.data.time.Day;
import org.jfree.data.time.Hour;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeTableXYDataset;
import org.wikipathways.stats.Task;
import org.wikipathways.stats.TaskException;
import org.wikipathways.stats.TaskParameters;
import org.wikipathways.stats.TimeInterval;
import org.wikipathways.stats.db.User;
import org.wikipathways.stats.db.WPDatabase;
import org.wikipathways.stats.taskimpl.EditFrequencies.CountedUser;

public class CurationEvent implements Task {
	public void start(WPDatabase db, TaskParameters par) throws TaskException {
		try {
			Calendar cal = Calendar.getInstance();
			cal.clear();
		    cal.set(Calendar.YEAR, 2010);
		    cal.set(Calendar.MONTH, 1);
		    cal.set(Calendar.DATE, 11);
		    cal.set(Calendar.HOUR_OF_DAY, 6);
		    
			Date start = cal.getTime();
			
			cal.clear();
		    cal.set(Calendar.YEAR, 2010);
		    cal.set(Calendar.MONTH, 1);
		    cal.set(Calendar.DATE, 20);
		    cal.set(Calendar.HOUR_OF_DAY, 10);
		    
			Date end = cal.getTime();

			Collection<User> users = User.getSnapshot(db, end);
			
			System.out.println(start);
			System.out.println(end);
			
			//Histogram
			List<CountedUser> rankedUsers = new ArrayList<CountedUser>();
			
			//Rank users based on their edit count
			for(User u : users) {
				if(u.getName().equals("MaintBot")) continue; //Skip bot
				int c = u.getEditCount(db, start, end);
				rankedUsers.add(new CountedUser(u, c));
			}
			Collections.sort(rankedUsers);
			
			DefaultCategoryDataset catData = new DefaultCategoryDataset();
			
			for(int i = 0; i < rankedUsers.size(); i++) {
				CountedUser cu = rankedUsers.get(i);
				System.err.println(cu.user.getFullName() + ": " + cu.count);
				
				if(cu.count == 0) break; //End with inactive users
				catData.addValue(cu.count, "Ranked users edit count", cu.user.getFullName());
			}
			
			DateFormat f = new SimpleDateFormat("M/d/yyyy");
			JFreeChart chart = ChartFactory.createBarChart(
					"Number of edits during curation jamboree (" + f.format(start) + " - " + f.format(end) + ")",
					"User", "Number of edits", catData, 
					PlotOrientation.VERTICAL, false, false, false);
			CategoryAxis xaxis = (CategoryAxis)chart.getCategoryPlot().getDomainAxis();
			xaxis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_90);
			ChartUtilities.saveChartAsPNG(
					new File(par.getFile(OUT_PATH), "curation_jamboree_frequencies.png"), 
					chart, par.getInt(GRAPH_WIDTH), par.getInt(GRAPH_HEIGHT)
			);
			
			//Timeline
			TimeInterval timeInterval = new TimeInterval(start, Hour.class);
			TimeTableXYDataset editData = new TimeTableXYDataset();
			
			RegularTimePeriod period = null;
			while((period = timeInterval.getNext()) != null) {
				System.out.println("Processing " + period);
				Date to = new Date(period.getLastMillisecond());
				if(to.after(end)) break;
				Date from = new Date(period.getFirstMillisecond());
				
				for(int i = 0; i < rankedUsers.size(); i++) {
					CountedUser cu = rankedUsers.get(i);
					if(cu.count == 0) break; //End with inactive users
					
					int c = cu.user.getEditCount(db, from, to);
					
					editData.add(period, c, cu.user.getFullName());
				}
			}
			
			chart = ChartFactory.createStackedXYAreaChart(
					"Curation jamboree activity", "Time", "Number of edits", editData, 
					PlotOrientation.VERTICAL, true, false, false);
			DateAxis axis = new DateAxis("Date");
			chart.getXYPlot().setDomainAxis(axis);
			ChartUtilities.saveChartAsPNG(
					new File(par.getFile(OUT_PATH), "curation_jamboree_timeline.png"), 
					chart, par.getInt(GRAPH_WIDTH), par.getInt(GRAPH_HEIGHT)
			);
			
			db.resetConnection();
			
			//Curation tags changes
			Set<String> tags = new HashSet<String>();
			tags.add("NeedsReference");
			tags.add("MissingDescription");
			CurationTagCounts.run(db, par, "curationevent_refdesc_", start, end, Day.class, tags);
			
			tags.clear();
			tags.add("AnalysisCollection");
			tags.add("FeaturedPathway");
			CurationTagCounts.run(db, par, "curationevent_collections_", start, end, Day.class, tags);
		} catch(Exception e) {
			throw new TaskException(e);
		}
	}
}
