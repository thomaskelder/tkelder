package org.apa.analysis;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

import org.apa.data.ExperimentAnalysis;
import org.apa.data.ExperimentData.ExperimentDataEntry;
import org.pathvisio.util.Stats;
import org.pathwaystats.EnrichmentTest;
import org.pathwaystats.EnrichmentTest.ResultMap;
import org.pathwaystats.EnrichmentTest.SetScoreCalculator;
import org.pathwaystats.EnrichmentTest.TestOptions;

import com.google.common.collect.Multimap;

public class ZScoreAnalysis extends EnrichmentAnalysis implements SetScoreCalculator {
	static final Logger log = Logger.getLogger("org.apa.analysis");
	static final double pvalue = 0.01;
	public static final String TYPE = "zscore_p_" + pvalue;
	
	int bigN;
	int bigR;
	
	public String getType() {
		return TYPE;
	}
	
	@Override
	protected ResultMap<String> performAnalysis(Multimap<String, String> sets,
			Map<String, Double> data) {
		//First calculate bigR and bigN
		bigN = data.keySet().size();
		bigR = 0;
		for(String gene : data.keySet()) {
			if(isSignificant(data.get(gene))) bigR++;
		}
		return super.performAnalysis(sets, data);
	}
	
	@Override
	protected void setAnalysisProperties(ExperimentAnalysis analysis,
			Map<String, Double> data, Multimap<String, String> sets) {
		analysis.setProperty("Total genes significant (R)", "" + bigR);
		super.setAnalysisProperties(analysis, data, sets);
	}
	
	@Override
	protected TestOptions getTestOptions() {
		return new TestOptions()
			.numberPermutations(1000)
			.setScoreCalculator(this)
			.setScoreComparator(EnrichmentTest.COMP_LARGEST);
	}
	
	@Override
	protected double getValue(ExperimentDataEntry entry) {
		return entry.getPvalue();
	}
	
	@Override
	protected boolean replaceValue(double oldValue, double newValue) {
		return newValue < oldValue;
	}
	
	public <E> double score(Multimap<E, Double> values) {
		int n = values.keySet().size();
		int r = 0;
		for(E g : values.keySet()) {
			if(isSignificant(values.get(g))) r++;
		}
		return Stats.zscore(n, r, bigN, bigR);
	}
	
	private boolean isSignificant(double value) {
		return !Double.isNaN(value) && value <= pvalue;
	}
	
	private boolean isSignificant(Collection<Double> values) {
		for(double v : values) {
			//Call significant if one of the measurements for a gene has
			//a p-value below the threshold.
			if(isSignificant(v)) return true;
		}
		return false;
	}
}
