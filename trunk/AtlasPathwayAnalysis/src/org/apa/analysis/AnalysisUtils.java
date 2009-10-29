package org.apa.analysis;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apa.data.Experiment;
import org.apa.data.ExperimentAnalysis;
import org.apa.data.Statistic;

public class AnalysisUtils {
	public static void asText(ResultFormat format, ExperimentAnalysis analysis, Writer out) throws IOException {
		//Write general properties
		Experiment exp = analysis.getExperiment();
		out.append("# Experiment: " + exp.getAccession() + "\n");
		out.append("# Description: " + exp.getDesciption() + "\n");
		out.append("# Factor: " + analysis.getFactor().getValue() + "(" + analysis.getFactor().getName() + ")\n");
		out.append("# Organism: " + exp.getOrganism() + "\n");
		
		for(String name : analysis.getProperties().keySet()) {
			out.append("# ");
			out.append(name);
			out.append(":\t");
			out.append(analysis.getProperty(name));
			out.append("\n");
		}
		out.append("\n");
		
		List<Statistic> stats = getSortedStats(analysis, format);
		if(stats.size() > 0) {
			//Write headers
			List<String> headers = new ArrayList<String>(stats.get(0).getValues().keySet());
			Collections.sort(headers);
			
			out.append("Pathway\t");
			for(String h : headers) {
				out.append(h);
				out.append("\t");
			}
			out.append("\n");
			
			//Write statistics
			for(Statistic stat : stats) {
				out.append(stat.getPathway().toString());
				out.append("\t");
				for(String h : headers) {
					out.append(stat.getValue(h));
					out.append("\t");
				}
				out.append("\n");
			}
		}
		out.close();
	}
	
	private static List<Statistic> getSortedStats(ExperimentAnalysis analysis, final ResultFormat format) {
		List<Statistic> sorted = new ArrayList<Statistic>(analysis.getEntries().values());
		Collections.sort(sorted, new Comparator<Statistic>() {
			public int compare(Statistic o1, Statistic o2) {
				double d1 = Double.parseDouble(o1.getValue(format.getSortColumn()));
				double d2 = Double.parseDouble(o2.getValue(format.getSortColumn()));
				
				if (Double.isNaN(d2) && Double.isNaN(d1)) return 0;
				if (Double.isNaN(d2)) return -1;
				if (Double.isNaN(d1)) return 1;
				return format.sortAscending() ? Double.compare(d1, d2) : Double.compare(d2, d1);
			}
		});
		return sorted;
	}
	
	public static ResultFormat getFormat(ExperimentAnalysis ea) {
		String type = ea.getType();
		if(EnrichmentAnalysis.TYPE.equals(type)) {
			return new EnrichmentAnalysis();
		}
		if(ZScoreAnalysis.TYPE.equals(type)) {
			return new ZScoreAnalysis();
		}
		return null;
	}
}
