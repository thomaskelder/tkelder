package org.wikipathways.stats.taskimpl;

import static org.wikipathways.stats.TaskParameters.GRAPH_HEIGHT;
import static org.wikipathways.stats.TaskParameters.GRAPH_WIDTH;
import static org.wikipathways.stats.TaskParameters.OUT_PATH;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.wikipathways.stats.Task;
import org.wikipathways.stats.TaskException;
import org.wikipathways.stats.TaskParameters;
import org.wikipathways.stats.db.User;
import org.wikipathways.stats.db.WPDatabase;

public class EditFrequencies implements Task {
	public void start(WPDatabase db, TaskParameters par) throws TaskException {
		try {
			Date now = new Date();
			Date start = db.getWpStart();

			DefaultCategoryDataset catData = new DefaultCategoryDataset();
			
			Collection<User> users = User.getSnapshot(db, start, now);
			List<CountedUser> rankedUsers = new ArrayList<CountedUser>();
			
			//Rank users based on their edit count
			for(User u : users) {
				if(u.getName().equals("MaintBot")) continue; //Skip bot
				
				rankedUsers.add(new CountedUser(u, u.getEditCount(db)));
			}
			Collections.sort(rankedUsers);
			
			for(int i = 0; i < rankedUsers.size(); i++) {
				CountedUser cu = rankedUsers.get(i);
				if(cu.count == 0) break; //End with inactive users
				catData.addValue(cu.count, "Ranked users edit count", cu.user.getName());
			}
			
			JFreeChart chart = ChartFactory.createBarChart(
					"WikiPathways users edit frequencies", "User rank", "Number of edits", catData, 
					PlotOrientation.VERTICAL, true, false, false);
			((CategoryAxis)chart.getCategoryPlot().getDomainAxis()).setCategoryLabelPositions(CategoryLabelPositions.DOWN_90);
			ChartUtilities.saveChartAsPNG(
					new File(par.getFile(OUT_PATH), "editfrequencies.png"), 
					chart, par.getInt(GRAPH_WIDTH), par.getInt(GRAPH_HEIGHT)
			);

		} catch(Exception e) {
			throw new TaskException(e);
		}

	}
	
	static class CountedUser implements Comparable<CountedUser> {
		User user;
		int count;
		
		public CountedUser(User u, int c) {
			user = u;
			count = c;
		}
		
		public int compareTo(CountedUser o) {
			return o.count - count;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((user == null) ? 0 : user.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CountedUser other = (CountedUser) obj;
			if (user == null) {
				if (other.user != null)
					return false;
			} else if (!user.equals(other.user))
				return false;
			return true;
		}
	}
}
