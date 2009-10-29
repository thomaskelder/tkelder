package org.apa.analysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
import org.pathwaystats.EnrichmentTest;
import org.pathwaystats.EnrichmentTest.EnrichmentResult;
import org.pathwaystats.EnrichmentTest.ResultMap;
import org.pathwaystats.EnrichmentTest.TestOptions;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public abstract class EnrichmentAnalysis implements AnalysisMethod, ResultFormat {
	static final Logger log = Logger.getLogger("org.apa.analysis");
	
	public String getSortColumn() {
		return AnalysisMethod.VALUE_SCORE;
	}
	
	public boolean sortAscending() {
		return false;
	};
	
	protected abstract TestOptions getTestOptions();
	
	protected abstract double getValue(ExperimentDataEntry entry);
	protected abstract boolean replaceValue(double oldValue, double newValue);
	
	protected ResultMap<String> performAnalysis(Multimap<String, String> sets, Map<String, Double> data) {
		TestOptions testOptions = getTestOptions();
		Multimap<String, Double> mdata = new HashMultimap<String, Double>();
		for(String g : data.keySet()) mdata.put(g, data.get(g));
		return EnrichmentTest.calculateResults(sets, mdata, testOptions);
	}
	
	public void performAnalysis(ExperimentAnalysis analysis,
				Collection<Pathway> pathways) throws AtlasException {
		Factor factor = analysis.getFactor();
		Experiment exp = analysis.getExperiment();
		Set<ExperimentData> expData = exp.getData(factor);

		Map<String, Double> data = new HashMap<String, Double>();
		for(ExperimentData d : expData) {
			for(ExperimentDataEntry entry : d.getEntries()) {
				double v = getValue(entry);
				//Find minimum p-value
				if(data.containsKey(entry.getGene())) {
					if(replaceValue(data.get(entry.getGene()), v)) {
						data.put(entry.getGene(), v);
					}
				}
				data.put(entry.getGene(), v);
			}
		}
		
		Multimap<String, String> sets = new HashMultimap<String, String>();
		for(Pathway p : pathways) sets.putAll(p.getId(), p.getGenes());

		ResultMap<String> results = performAnalysis(sets, data);

		//Store general results
		setAnalysisProperties(analysis, data, sets);
		
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
	
	protected void setAnalysisProperties(ExperimentAnalysis analysis, Map<String, Double> data, Multimap<String, String> sets) {
		analysis.setProperty(AnalysisMethod.PROP_TOTAL_MEASURED, "" + data.keySet().size());
		Set<String> inPathway = new HashSet<String>(sets.values());
		analysis.setProperty(AnalysisMethod.PROP_TOTAL_PATHWAY, "" + inPathway.size());
		inPathway.retainAll(data.keySet());
		analysis.setProperty(AnalysisMethod.PROP_TOTAL_MEASURED_PATHWAY, "" + inPathway.size());
	}
}
