package org.wikipathways.stats.taskimpl;

import static org.wikipathways.stats.TaskParameters.GRAPH_HEIGHT;
import static org.wikipathways.stats.TaskParameters.GRAPH_WIDTH;
import static org.wikipathways.stats.TaskParameters.OUT_PATH;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Month;
import org.jfree.data.time.Quarter;
import org.jfree.data.time.RegularTimePeriod;
import org.wikipathways.stats.Task;
import org.wikipathways.stats.TaskException;
import org.wikipathways.stats.TaskParameters;
import org.wikipathways.stats.TimeInterval;
import org.wikipathways.stats.db.WPDatabase;
import org.wikipathways.stats.db.Webservice;

public class WebserviceActivity implements Task {
	public void start(WPDatabase db, TaskParameters par) throws TaskException {
		try {
			Calendar cal = Calendar.getInstance();
			cal.clear();
			cal.set(Calendar.YEAR, 2009);
			cal.set(Calendar.MONTH, 3);
			cal.set(Calendar.DATE, 1);
			cal.set(Calendar.HOUR_OF_DAY, 1);

			Date start = cal.getTime();

			TimeInterval timeInterval = new TimeInterval(start, Month.class);
			RegularTimePeriod period = null;
			JFreeChart chart = null;
			
			//Create graph showing request counts over time
			DefaultCategoryDataset countData = new DefaultCategoryDataset();
			DefaultCategoryDataset operationData = new DefaultCategoryDataset();
			
			while((period = timeInterval.getNext()) != null) {
				System.out.println("Processing " + period);
				Date to = new Date(period.getLastMillisecond());
				Date from = new Date(period.getFirstMillisecond());
				
				int total = Webservice.getCounts(db, from, to);
				int own = 0;
				for(String ip : Webservice.getIps(db, from, to)) {
					if(
							ip.startsWith("137.120.14") ||
							ip.equals("137.120.89.38") ||
							ip.equals("137.120.89.24")) {
						own += Webservice.getCountsForIp(db, from, to, ip);
					}
				}
				countData.addValue(own, "Requests (own services)", period);
				countData.addValue(total - own, "Requests (external services)", period);
				
				int opTotal = 0;
				Map<String, Integer> operationCounts = Webservice.getCountsPerOperation(db, from, to);
				for(String op : operationCounts.keySet()) {
					int opCount = operationCounts.get(op);
					operationData.addValue(opCount, op, period);
					opTotal += opCount;
				}
				operationData.addValue(total - opTotal, "Other (e.g. wsdl or incorrect request)", period);
			}

			chart = ChartFactory.createStackedBarChart(
					"WikiPathways web service usage", "Date", "Number of requests", countData, 
					PlotOrientation.VERTICAL, true, false, false);
			CategoryAxis xaxis = (CategoryAxis)chart.getCategoryPlot().getDomainAxis();
			xaxis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_90);
			ChartUtilities.saveChartAsPNG(
					new File(par.getFile(OUT_PATH), "webservice.png"), 
					chart, par.getInt(GRAPH_WIDTH), par.getInt(GRAPH_HEIGHT)
			);
			
			chart = ChartFactory.createStackedBarChart(
					"WikiPathways web service usage", "Date", "Number of requests", operationData, 
					PlotOrientation.VERTICAL, true, false, false);
			xaxis = (CategoryAxis)chart.getCategoryPlot().getDomainAxis();
			xaxis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_90);
			ChartUtilities.saveChartAsPNG(
					new File(par.getFile(OUT_PATH), "webservice-operations.png"), 
					chart, par.getInt(GRAPH_WIDTH), par.getInt(GRAPH_HEIGHT)
			);
			
			//Create pie charts showing number of requests per ip per time interval
			
			timeInterval = new TimeInterval(start, Quarter.class);

			//Create graph showing request counts over time
			while((period = timeInterval.getNext()) != null) {
				DefaultPieDataset pieCounts = new DefaultPieDataset();
				Map<String, Integer> countsPerIp = sortByValue(
						Webservice.getCountsPerIp(db, period.getStart(), period.getEnd())
				);
				
				int i = 0;
				int other = 0;
				for(String ip : countsPerIp.keySet()) {
					System.out.println(ip + ": " + countsPerIp.get(ip));
					if(i++ < 10) {
						pieCounts.setValue(ip, countsPerIp.get(ip));
					} else {
						other += countsPerIp.get(ip);
					}
				}
				pieCounts.setValue("other", other);
				
				DateFormat f = new SimpleDateFormat("M/d/yyyy");
				
				chart = ChartFactory.createPieChart(
						"Requests per ip from " + f.format(period.getStart()) + " to " + f.format(period.getEnd()), 
						pieCounts, true, false, false);
				ChartUtilities.saveChartAsPNG(
						new File(par.getFile(OUT_PATH), "webservice-ipcounts-" + period.toString().replaceAll("/", "_") + ".png"), 
						chart, par.getInt(GRAPH_WIDTH), par.getInt(GRAPH_HEIGHT)
				);
			}
		} catch(Exception e) {
			throw new TaskException(e);
		}
	}

	static Map sortByValue(Map map) {
		List list = new LinkedList(map.entrySet());
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o2)).getValue())
				.compareTo(((Map.Entry) (o1)).getValue());
			}
		});
		Map result = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry)it.next();
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
}
