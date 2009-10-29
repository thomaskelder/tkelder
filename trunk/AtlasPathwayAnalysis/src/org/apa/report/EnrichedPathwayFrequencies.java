package org.apa.report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apa.AtlasException;
import org.apa.data.ExperimentAnalysis;
import org.apa.data.Pathway;
import org.apa.data.Statistic;
import org.hibernate.Session;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

public class EnrichedPathwayFrequencies extends Report {
	Set<ExperimentAnalysis> analyses;
	String analysisType;
	String species;
	IsSignificant significant;
	
	List<PathwayCount> counts;
	
	public EnrichedPathwayFrequencies(Session session, String analysisType, String species, IsSignificant significant) {
		super(session);
		this.analysisType = analysisType;
		this.species = species;
		this.significant = significant;
	}
	
	@SuppressWarnings("unchecked") //HQL queries return untyped list, but we do know their type
	private void count() {
		Session session = getSession();
		if(analyses == null) {
			String query = 
				"from ExperimentAnalysis ae where ae.id.experiment.organism = :org and ae.id.type = :type";
			analyses = new HashSet<ExperimentAnalysis>(session.createQuery(query)
				.setString("org", species).setString("type", analysisType).list());
		}
		
		List<Pathway> pathways = (List<Pathway>)session.createQuery("from Pathway p where p.organism = :org")
			.setString("org", species).list();
		
		counts = new ArrayList<PathwayCount>();
		
		for(Pathway p : pathways) {
			PathwayCount pc = new PathwayCount(p);
			counts.add(pc);

			for(ExperimentAnalysis an : analyses) {
				Statistic stat = an.getStatistic(p);
				if(stat != null && significant.isSignificant(stat)) {
					pc.sigExperiments.add(an.getExperiment().getAccession());
				}
			}
		}
		
		Collections.sort(counts);
	}
	
	@Override
	public void saveReport(File outPath, String prefix) throws AtlasException {
		try {
			count();
			
			outPath.mkdirs();
			saveAsText(new File(outPath, prefix + ".txt"));
			saveAsGraph(new File(outPath, prefix + ".png"));
		} catch(Exception e) {
			if(e instanceof AtlasException) throw (AtlasException)e;
			else throw new AtlasException(e);
		}
	}
	
	private void saveAsGraph(File graphFile) throws IOException {
		DefaultCategoryDataset data = new DefaultCategoryDataset();
		
		for(PathwayCount pc : counts) {
			data.addValue(pc.getCount(), "significant count", "" + pc.pathway);
		}
		
		JFreeChart chart = ChartFactory.createBarChart(
				"Significant counts", "Pathway", "Times significant", data, 
				PlotOrientation.VERTICAL, true, false, false);
		((CategoryAxis)chart.getCategoryPlot().getDomainAxis()).setCategoryLabelPositions(CategoryLabelPositions.DOWN_90);
		ChartUtilities.saveChartAsPNG(
				graphFile, chart, Math.max(800, counts.size() * 20), 800
		);
	}
	
	private void saveAsText(File txtFile) throws IOException {
		//Write to text file
		BufferedWriter out = new BufferedWriter(new FileWriter(txtFile));
		out.append("Pathway\tSignificant count\tExperiments\n");
		for(PathwayCount pc : counts) {
			out.append(pc.pathway.getName() + " (" + pc.pathway.getId() + ")");
			out.append("\t");
			out.append("" + pc.getCount());
			out.append("\t");
			out.append(pc.sigExperiments.toString());
			out.append("\n");
		}
		out.close();
	}
	
	static class PathwayCount implements Comparable<PathwayCount>{
		Pathway pathway;
		Set<String> sigExperiments = new HashSet<String>();
		
		public PathwayCount(Pathway p) {
			pathway = p;
		}
		
		public int compareTo(PathwayCount o) {
			return o.sigExperiments.size() - sigExperiments.size();
		}
		
		public int getCount() {
			return sigExperiments.size();
		}
	}
	
	public interface IsSignificant {
		boolean isSignificant(Statistic stat);
	}
}
