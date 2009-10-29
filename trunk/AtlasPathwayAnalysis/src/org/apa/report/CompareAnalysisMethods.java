package org.apa.report;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apa.AtlasException;
import org.apa.analysis.AnalysisMethod;
import org.apa.data.Statistic;
import org.hibernate.Session;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.MatrixSeries;
import org.jfree.data.xy.XYSeries;

public class CompareAnalysisMethods extends Report {
	String type1;
	String type2;
	String value1;
	String value2;
	Transform trans1;
	Transform trans2;
	SeriesType type = SeriesType.EXPERIMENT;
	
	public CompareAnalysisMethods(Session session, String type1, String type2, String value1, String value2) {
		super(session);
		this.type1 = type1;
		this.type2 = type2;
		this.value1 = value1;
		this.value2 = value2;
	}

	public void setTrans1(Transform trans1) {
		this.trans1 = trans1;
	}
	
	public void setTrans2(Transform trans2) {
		this.trans2 = trans2;
	}

	private String getSeriesId(Statistic stat) {
		switch(type) {
		case PATHWAY_SIZE:
			int measured = Integer.parseInt(stat.getValue(AnalysisMethod.VALUE_MEASURED));
			if(measured < 10) return "< 10";
			if(measured >= 10 && measured < 20) return "10 - 20";
			if(measured >= 20 && measured < 30) return "20 - 30";
			if(measured >= 30 && measured < 40) return "30 - 40";
			if(measured >= 40 && measured < 50) return "40 - 50";
			if(measured >= 50) return ">50";
			break;
		case SPECIES:
			return stat.getPathway().getOrganism();
		case EXPERIMENT:
			return stat.getAnalysis().getExperiment().getAccession();
		}
		return null;
	}
	
	@Override
	public void saveReport(File outPath, String prefix) throws AtlasException {
		try {
			outPath.mkdirs();
			Session session = getSession();
			
			Map<String, List<double[]>> seriesById = new HashMap<String, List<double[]>>();
			
			Iterator results = session.createQuery(statPairQuery).setString(0, type1).setString(1, type2)
				.setMaxResults(5000).iterate();
			while(results.hasNext()) {
				Object[] obj = (Object[])results.next();
				Statistic stat1 = (Statistic)obj[0];
				Statistic stat2 = (Statistic)obj[1];
				
				double d1 = Double.parseDouble(stat1.getValue(value1));
				double d2 = Double.parseDouble(stat2.getValue(value2));
				if(trans1 != null) d1 = trans1.transform(d1);
				if(trans2 != null) d2 = trans2.transform(d2);
				
				if(Double.isNaN(d1) || Double.isNaN(d2)) {
					continue;
				}
				
				double m = Double.parseDouble(stat1.getValue(AnalysisMethod.VALUE_MEASURED)) / 200;
				
				String id = getSeriesId(stat1);
				List<double[]> values = seriesById.get(id);
				if(values == null) seriesById.put(id, values = new ArrayList<double[]>());
				values.add(new double[] { d1, d2, m });
			}

			DefaultXYZDataset data = new DefaultXYZDataset();
			for(String id : seriesById.keySet()) {
				List<double[]> vl = seriesById.get(id);
				double[][] va = new double[3][vl.size()];
				for(int i = 0; i < vl.size(); i++) {
					va[0][i] = vl.get(i)[0];
					va[1][i] = vl.get(i)[1];
					va[2][i] = vl.get(i)[2];
				}
				data.addSeries(id, va);
			}

			JFreeChart chart = ChartFactory.createBubbleChart(
					type1 + " vs " + type2, 
					(trans1 == null ? value1 : trans1.getLabel()) + " [" + type1 + "]", 
					(trans2 == null ? value2: trans2.getLabel()) + " [" + type2 + "]", 
					data, PlotOrientation.VERTICAL, true, false, false
			);
			File graphFile = new File(outPath, prefix + "_" + type + ".png");
			ChartUtilities.saveChartAsPNG(
					graphFile, chart, 1024, 1024
			);
		} catch(Exception e) {
			throw new AtlasException(e);
		}
	}

	String statPairQuery = 
		"from Statistic s1, Statistic s2 where " +
		"s1.id.pathway = s2.id.pathway and " +
		"s1.id.analysis.id.experiment = s2.id.analysis.id.experiment and " +
		"s1.id.analysis.id.factor = s2.id.analysis.id.factor and " +
		"s1.id.analysis.id.type = ? and " +
		"s2.id.analysis.id.type = ? ";
	
	public interface Transform {
		public double transform(double d);
		public String getLabel();
	}
	
	public enum SeriesType {
		SPECIES, PATHWAY_SIZE, EXPERIMENT
	}
}
