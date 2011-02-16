package org.wikipathways.stats.taskimpl;

import static org.wikipathways.stats.TaskParameters.GRAPH_HEIGHT;
import static org.wikipathways.stats.TaskParameters.GRAPH_WIDTH;
import static org.wikipathways.stats.TaskParameters.OUT_PATH;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.bridgedb.Xref;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.time.Month;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYZDataset;
import org.pathvisio.model.Pathway;
import org.wikipathways.stats.Task;
import org.wikipathways.stats.TaskException;
import org.wikipathways.stats.TaskParameters;
import org.wikipathways.stats.TimeInterval;
import org.wikipathways.stats.WikiPathwaysStats;
import org.wikipathways.stats.db.CurationTag;
import org.wikipathways.stats.db.PathwayInfo;
import org.wikipathways.stats.db.User;
import org.wikipathways.stats.db.WPDatabase;

public class EditCounts implements Task {
	public void start(WPDatabase db, TaskParameters par) throws TaskException {
		try {
			PrintWriter txtout = new PrintWriter(new File(par.getFile(TaskParameters.OUT_PATH), "views_vs_edits.txt"));
			txtout.println("pathway\tviews\tedits\tauthors\tdatanodes");
			
			//Which pathways are test pathways?
			Collection<CurationTag> tags = CurationTag.getLatest(db);
			Set<Integer> isTest = new HashSet<Integer>();
			for(CurationTag t : tags) {
				if("Tutorial".equals(t.getType())) isTest.add(t.getPageId());
			}
			
			//Plot view counts against edit counts
			XYZDataset xyz = new DefaultXYZDataset();
			
			XYSeries[] data = new XYSeries[5];
			data[0] = new XYSeries("1");
			data[1] = new XYSeries("2");
			data[2] = new XYSeries("3");
			data[3] = new XYSeries("4");
			data[4] = new XYSeries(">=5");
			for(Entry<PathwayInfo, Integer> e : PathwayInfo.getViewCounts(db).entrySet()) {
				if(isTest.contains(e.getKey().getPageId())) continue;
				if(e.getKey().isPrivate()) continue;
				
				int revs = e.getKey().getNrRevisions(db, true);
				int views = e.getValue().intValue();
				int users = e.getKey().getAuthors().size();
				if(users > 4) users = 5;
				data[users - 1].add(views, revs);
				
				String gpml = e.getKey().getGpml();
				Pathway p = new Pathway();
				p.readFromXml(new StringReader(gpml), false);
				Set<Xref> xrefs = new HashSet<Xref>();
				xrefs.addAll(p.getDataNodeXrefs());
				txtout.println(e.getKey().getPathwayId() + "\t" + views + "\t" + revs + "\t" + e.getKey().getAuthors().size() + "\t" + xrefs.size());
			}
			
			txtout.close();
			
			DefaultXYDataset xydata = new DefaultXYDataset();
			xydata.setGroup(new DatasetGroup("Number of editors"));
			for(XYSeries s : data) xydata.addSeries(s.getKey(), s.toArray());
			JFreeChart chart = ChartFactory.createScatterPlot(
					"", "Views", "Edits", xydata, 
					PlotOrientation.VERTICAL, true, false, false);
	        chart.getXYPlot().setRangeAxis(new LogarithmicAxis("Edits"));
	        chart.getXYPlot().setDomainAxis(new LogarithmicAxis("Views"));
	        chart.getXYPlot().setBackgroundPaint(Color.WHITE);
	        chart.getXYPlot().setDomainGridlinePaint(new Color(220, 220, 220));
	        chart.getXYPlot().setRangeGridlinePaint(new Color(220, 220, 220));
	        chart.getXYPlot().setRangeMinorGridlinesVisible(false);
	        chart.getXYPlot().setDomainMinorGridlinesVisible(false);
	        chart.getXYPlot().setDomainGridlineStroke(new BasicStroke());
	        chart.getXYPlot().setRangeGridlineStroke(new BasicStroke());
	        chart.getXYPlot().getRangeAxis().setLabelFont(new Font("Arial", Font.PLAIN, 20));
	        chart.getXYPlot().getRangeAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 16));
	        chart.getXYPlot().getDomainAxis().setLabelFont(new Font("Arial", Font.PLAIN, 20));
	        chart.getXYPlot().getDomainAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 16));
	        
	        XYItemRenderer r = chart.getXYPlot().getRenderer();
	        r.setSeriesPaint(3, Color.GRAY);
	        r.setSeriesPaint(4, Color.MAGENTA);
	        
			ChartUtilities.saveChartAsPNG(
					new File(par.getFile(OUT_PATH), "view_vs_edit.png"), 
					chart, par.getInt(GRAPH_WIDTH), par.getInt(GRAPH_HEIGHT)
			);
			WikiPathwaysStats.exportChartAsSVG(
					chart, new Rectangle(par.getInt(GRAPH_WIDTH), par.getInt(GRAPH_HEIGHT)), 
					new File(par.getFile(OUT_PATH), "view_vs_edit.svg")
			);
			Date start = db.getWpStart();
			Calendar cal = Calendar.getInstance();
			cal.clear();
		    cal.set(Calendar.YEAR, 2011);
		    cal.set(Calendar.MONTH, 1);
		    cal.set(Calendar.DATE, 1);
		    cal.set(Calendar.HOUR_OF_DAY, 0);
			Date end = cal.getTime();
			
			TimeInterval timeInterval = new TimeInterval(start, end, Month.class);

			txtout = new PrintWriter(new File(par.getFile(TaskParameters.OUT_PATH), "editcounts_interval.txt"));
			txtout.println("date\ttype\tcount");
			SimpleDateFormat dformat = new SimpleDateFormat("yyyy/MM/dd");
			
			DefaultCategoryDataset editData = new DefaultCategoryDataset();
			
			RegularTimePeriod period = null;
			int i = 0;
			while((period = timeInterval.getNext()) != null) {
				System.out.println("Processing " + period);
				Date to = new Date(period.getLastMillisecond());
				Date from = new Date(period.getFirstMillisecond());
				Collection<User> users = User.getSnapshot(db, to);
				
				int editCounts = 0;
				int testCounts = 0;
				int botCounts = 0;
				for(User u : users) {
					List<Integer> userEdits = u.getEditPages(db, from, to);
					if("MaintBot".equals(u.getName())) {
						botCounts += userEdits.size();
						continue;
					}
					int total = userEdits.size();
					userEdits.removeAll(isTest);
					int notest = userEdits.size();
					
					editCounts += notest;
					testCounts += total - notest;
				}
				
				Date time = new Date(period.getLastMillisecond());
				txtout.println(
						dformat.format(time) + "\tUser edits\t" + editCounts
				);
				txtout.println(
						dformat.format(time) + "\tTest/tutorial edits\t" + testCounts
				);
				txtout.println(
						dformat.format(time) + "\tBot edits\t" + botCounts
				);
				
				editData.addValue(editCounts, "Number of edits", period);
				editData.addValue(testCounts, "Number of test/tutorial edits", period);
				editData.addValue(botCounts, "Number bot edits", period);
				if(i++ % 10 == 0) db.resetConnection();
			}
			
			txtout.close();
			chart = ChartFactory.createBarChart(
					"WikiPathways pathway edits", "Date", "Number of edits", editData, 
					PlotOrientation.VERTICAL, true, false, false);
			CategoryAxis xaxis = (CategoryAxis)chart.getCategoryPlot().getDomainAxis();
			xaxis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_90);
			ChartUtilities.saveChartAsPNG(
					new File(par.getFile(OUT_PATH), "editcounts_interval.png"), 
					chart, par.getInt(GRAPH_WIDTH), par.getInt(GRAPH_HEIGHT)
			);
			
		} catch(Exception e) {
			throw new TaskException(e);
		}
	}
}
