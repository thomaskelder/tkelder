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
import org.pathwaystats.EnrichmentTest;
import org.pathwaystats.EnrichmentTest.EnrichmentResult;
import org.pathwaystats.EnrichmentTest.ResultMap;
import org.pathwaystats.EnrichmentTest.TestOptions;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class EnrichmentAnalysis implements AnalysisMethod, ResultFormat {
	static final Logger log = Logger.getLogger("org.apa.analysis");
	
	public static final String TYPE = "enrichment_absT_mean";
	
	static final TestOptions testOptions = new TestOptions()
		.numberPermutations(1000)
		.setScoreCalculator(EnrichmentTest.CALC_SET_MEAN)
		.setScoreComparator(EnrichmentTest.COMP_LARGEST);
	
	public String getSortColumn() {
		return AnalysisMethod.VALUE_PVALUE;
	}
	
	public boolean sortAscending() {
		return true;
	}
	
	public String getType() {
		return TYPE;
	}
	
	public void performAnalysis(ExperimentAnalysis analysis, Collection<Pathway> pathways) throws AtlasException {

		Factor factor = analysis.getFactor();
		Experiment exp = analysis.getExperiment();
		ExperimentData expData = exp.getData(factor);

		Multimap<String, Double> data = new HashMultimap<String, Double>();
		for(ExperimentDataEntry entry : expData.getEntries()) {
			data.put(entry.getGene(), Math.abs(entry.getTstat()));
		}
		
		Multimap<String, String> sets = new HashMultimap<String, String>();
		for(Pathway p : pathways) sets.putAll(p.getId(), p.getGenes());
		
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
		
		//Store general results
		analysis.setProperty(AnalysisMethod.PROP_TOTAL_MEASURED, "" + data.keySet().size());
		Set<String> inPathway = new HashSet<String>(sets.values());
		analysis.setProperty(AnalysisMethod.PROP_TOTAL_PATHWAY, "" + inPathway.size());
		inPathway.retainAll(data.keySet());
		analysis.setProperty(AnalysisMethod.PROP_TOTAL_MEASURED_PATHWAY, "" + inPathway.size());
	}
}
