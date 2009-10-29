package org.apa.report;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apa.AtlasException;
import org.hibernate.Session;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYSeries;

public class ExpressionPlots extends Report {
	public ExpressionPlots(Session session) {
		super(session);
	}
	
	@Override
	public void saveReport(File outPath, String prefix) throws AtlasException {
		try {
			outPath.mkdirs();
			pvalueVsTstat(new File(outPath, prefix + "_pvalue_vs_tstat.png"));
			tstatHistogram(new File(outPath, prefix + "_tstat_hist.png"));
		} catch(Exception e) {
			throw new AtlasException(e);
		}
	}
	
	private void tstatHistogram(File plotFile) throws IOException {
		String query = "select e.tstat from ExperimentData ed join ed.entries e";
		
		List<String> strValues = getSession().createQuery(query).list();
		List<Double> dValues = new ArrayList<Double>(10000);
		
		for(String str : new ArrayList<String>(strValues)) {
			double d = Double.parseDouble(str);
			if(d == 0) continue;
			dValues.add(d);
		}
		
		double[] values = new double[dValues.size()];
		for(int i = 0; i < values.length; i++) {
			values[i] = dValues.get(i);
		}
		
		HistogramDataset data = new HistogramDataset();
		data.addSeries("T stats", values, 100);
		JFreeChart chart = ChartFactory.createHistogram(
				"Atlas T stats", "T", "frequency", 
				data, PlotOrientation.VERTICAL, false, false, false
		);
		ChartUtilities.saveChartAsPNG(
				plotFile, chart, 1024, 1024
		);
	}
	
	private void pvalueVsTstat(File plotFile) throws IOException {
		String query = "select e.tstat, e.pvalue from ExperimentData ed join ed.entries e";
		
		XYSeries series = new XYSeries("data");
		
		Iterator it = getSession().createQuery(query).iterate();
		while(it.hasNext()) {
			Object[] obj = (Object[])it.next();
			double t = Double.parseDouble((String)obj[0]);
			double p = Double.parseDouble((String)obj[1]);
			if(t == 0 && p == 1) continue;
			
			double lp = -Math.log10(p);
			if(Double.isInfinite(lp)) lp = 150;
			series.add(t, lp);
		}
		
		DefaultXYDataset data = new DefaultXYDataset();
		data.addSeries(series.getKey(), series.toArray());
		
		JFreeChart chart = ChartFactory.createScatterPlot(
				"Atlas experiments T vs p-value", 
				"T", "-log10(p-value)", 
				data, PlotOrientation.VERTICAL, false, false, false
		);
		ChartUtilities.saveChartAsPNG(
				plotFile, chart, 1024, 1024
		);
	}
}
