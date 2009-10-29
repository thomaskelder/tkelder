package org.apa.report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.apa.AtlasException;
import org.apa.AtlasSessionUtils;
import org.apa.analysis.ZScoreAnalysis;
import org.apa.data.ExperimentAnalysis;
import org.apa.data.Pathway;
import org.apa.data.Statistic;
import org.hibernate.Session;

public class AnalysisMatrix extends Report {
	private String analysisType = ZScoreAnalysis.TYPE;
	private String species;
	private int minGenes = 10;

	public AnalysisMatrix(Session session, String species) {
		super(session);
		this.species = species;
	}

	@Override
	public void saveReport(File outPath, String prefix) throws AtlasException {
		try {
			outPath.mkdirs();
			Session session = getSession();
			
			BufferedWriter out = new BufferedWriter(new FileWriter(new File(outPath, prefix + "_" + analysisType + ".txt")));
			
			//Collect all pathways
			List<String> pathways = new ArrayList<String>(AtlasSessionUtils.getPathways(session, species));
			
			//Filter pathways for size
			for(String pid : new ArrayList<String>(pathways)) {
				Pathway p = (Pathway)session.get(Pathway.class, pid);
				if(p.getGenes().size() < minGenes) {
					pathways.remove(pid);
				}
			}
			
			//Write headers
			out.append(
				"Analysis\tExperiment name\tExperimental Factor"	
			);
			for(String pid : pathways) {
				Pathway p = (Pathway)session.get(Pathway.class, pid);
				out.append("\t" + p.toString());
			}
			out.append("\n");
			
			//Collect all analyses
			List<ExperimentAnalysis> analyses = (List<ExperimentAnalysis>)session.createQuery(
					"from ExperimentAnalysis e where e.id.type = ?").setString(0, analysisType).list();
			
			for(ExperimentAnalysis a : analyses) {
				if(!a.getExperiment().getOrganism().equals(species)) continue;
				
				out.append(a.toString() + "\t" + a.getExperiment().getName() + "\t" + a.getFactor());
				for(String pid : pathways) {
					out.append("\t");
					Pathway p = (Pathway)session.get(Pathway.class, pid);
					Statistic stat = a.getStatistic(p);
					if(stat != null) {
						out.append(stat.getValue(ZScoreAnalysis.VALUE_SCORE));
					} else {
						out.append("NaN");
					}
				}
				out.append("\n");
			}
			
			out.close();
		} catch(Exception e) {
			throw new AtlasException(e);
		}
	}
}
