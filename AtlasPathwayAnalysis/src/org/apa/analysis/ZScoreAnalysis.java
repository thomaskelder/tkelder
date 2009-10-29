package org.apa.analysis;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.apa.AtlasException;
import org.apa.data.Experiment;
import org.apa.data.ExperimentAnalysis;
import org.apa.data.ExperimentData;
import org.apa.data.Factor;
import org.apa.data.Pathway;
import org.apa.data.Statistic;
import org.apa.data.ExperimentData.ExperimentDataEntry;
import org.pathvisio.util.Stats;
import org.pathwaystats.EnrichmentTest;
import org.pathwaystats.EnrichmentTest.EnrichmentResult;
import org.pathwaystats.EnrichmentTest.ResultMap;
import org.pathwaystats.EnrichmentTest.SetScoreCalculator;
import org.pathwaystats.EnrichmentTest.TestOptions;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class ZScoreAnalysis implements AnalysisMethod, ResultFormat, SetScoreCalculator {
static final Logger log = Logger.getLogger("org.apa.analysis");
	static final double pvalue = 0.01;
	public static final String TYPE = "zscore_p_" + pvalue;
	
	public String getSortColumn() {
		return AnalysisMethod.VALUE_SCORE;
	}
	
	public boolean sortAscending() {
		return false;
	};
	
	public String getType() {
		return TYPE;
	}
	
	public void performAnalysis(ExperimentAnalysis analysis,
				Collection<Pathway> pathways) throws AtlasException {
		Factor factor = analysis.getFactor();
		Experiment exp = analysis.getExperiment();
		ExperimentData expData = exp.getData(factor);

		Multimap<String, Double> data = new HashMultimap<String, Double>();
		for(ExperimentDataEntry entry : expData.getEntries()) {
			data.put(entry.getGene(), entry.getPvalue());
		}
		
		Multimap<String, String> sets = new HashMultimap<String, String>();
		for(Pathway p : pathways) sets.putAll(p.getId(), p.getGenes());
		
		TestOptions testOptions = new TestOptions()
			.numberPermutations(1000)
			.setScoreCalculator(this)
			.setScoreComparator(EnrichmentTest.COMP_LARGEST);
		
		bigN = data.keySet().size();

		//Store general results
		analysis.setProperty(AnalysisMethod.PROP_TOTAL_MEASURED, "" + data.keySet().size());
		Set<String> inPathway = new HashSet<String>(sets.values());
		analysis.setProperty(AnalysisMethod.PROP_TOTAL_PATHWAY, "" + inPathway.size());
		inPathway.retainAll(data.keySet());
		analysis.setProperty(AnalysisMethod.PROP_TOTAL_MEASURED_PATHWAY, "" + inPathway.size());

		//Calculate R
		bigR = 0;
		for(String gene : data.keySet()) {
			Collection<Double> values = data.get(gene);
			if(isSignificant(values)) bigR++;
		}
		analysis.setProperty("Total genes significant (R)", "" + bigR);
		
		ResultMap<String> results = EnrichmentTest.calculateResults(sets, data, testOptions);
		
		//Store the results
		for(Pathway p : pathways) {
			Statistic stat = new Statistic(p, analysis);
			EnrichmentResult<String> r = results.getResult(p.getId());
			if(r == null) { //Empty pathway
				r = new EnrichmentResult<String>(p.getId(), 0, 0, Double.NaN, Double.NaN);
			}
			stat.setValue(AnalysisMethod.VALUE_PVALUE, "" + r.getPvalue());
			stat.setValue(AnalysisMethod.VALUE_SCORE, "" + r.getScore());
			stat.setValue(AnalysisMethod.VALUE_MEASURED, "" + r.getMeasured());
			stat.setValue(AnalysisMethod.VALUE_SIZE, "" + r.getSize());
			log.fine("Adding statistic: " + stat);
			analysis.setStatistic(stat);
		}
	}
	
	public void performAnalysisOld(ExperimentAnalysis analysis,
			Collection<Pathway> pathways) throws AtlasException {
		
		Factor factor = analysis.getFactor();
		Experiment exp = analysis.getExperiment();
		ExperimentData expData = exp.getData(factor);

		Multimap<String, Double> data = new HashMultimap<String, Double>();
		for(ExperimentDataEntry entry : expData.getEntries()) {
			data.put(entry.getGene(), entry.getPvalue());
		}
		
		Set<String> pathwayGenes = new HashSet<String>();
		for(Pathway p : pathways) pathwayGenes.addAll(p.getGenes());
		
		int bigN = data.keySet().size();

		analysis.setProperty("Total genes measured (N)", "" + bigN);
		analysis.setProperty("Total genes in pathways", "" + pathwayGenes.size());
		pathwayGenes.retainAll(data.keySet());
		analysis.setProperty("Total genes measured and in pathway", "" + pathwayGenes.size());

		//Calculate R
		int bigR = 0;
		for(String gene : data.keySet()) {
			Collection<Double> values = data.get(gene);
			if(isSignificant(values)) bigR++;
		}
		analysis.setProperty("Total genes significant (R)", "" + bigR);
		
		for(Pathway p : pathways) {
			Collection<String> genes = new HashSet<String>(p.getGenes());
			genes.retainAll(data.keySet()); //Remove all genes that were not measured
			int n = genes.size();
			int r = 0;
			for(String g : genes) {
				if(isSignificant(data.get(g))) r++;
			}
			double z = Stats.zscore(n, r, bigN, bigR);
			Statistic stat = new Statistic(p, analysis);
			stat.setValue("z-score", "" + z);
			stat.setValue("measured (n)", "" + n);
			stat.setValue("significant (r)", "" + r);
			stat.setValue("pathway size", "" + p.getGenes().size());
			analysis.setStatistic(stat);
		}
	}
	
	int bigN;
	int bigR;
	
	public <E> double score(Multimap<E, Double> values) {
		int n = values.keySet().size();
		int r = 0;
		for(E g : values.keySet()) {
			if(isSignificant(values.get(g))) r++;
		}
		return Stats.zscore(n, r, bigN, bigR);
	}
	
	private boolean isSignificant(Collection<Double> values) {
		for(double v : values) {
			//Call significant if one of the measurements for a gene has
			//a p-value below the threshold.
			if(!Double.isNaN(v) && v <= pvalue) return true;
		}
		return false;
	}
}
