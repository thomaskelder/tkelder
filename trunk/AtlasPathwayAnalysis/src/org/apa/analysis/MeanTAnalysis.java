package org.apa.analysis;

import java.util.logging.Logger;

import org.apa.data.ExperimentData.ExperimentDataEntry;
import org.pathwaystats.EnrichmentTest;
import org.pathwaystats.EnrichmentTest.TestOptions;

public class MeanTAnalysis extends EnrichmentAnalysis {
	static final Logger log = Logger.getLogger("org.apa.analysis");
	
	public static final String TYPE = "enrichment_absT_mean";
	
	static final TestOptions testOptions = new TestOptions()
		.numberPermutations(1000)
		.setScoreCalculator(EnrichmentTest.CALC_SET_MEAN)
		.setScoreComparator(EnrichmentTest.COMP_LARGEST);
	
	public String getType() {
		return TYPE;
	}
	
	@Override
	protected TestOptions getTestOptions() {
		return testOptions;
	}
	
	@Override
	protected double getValue(ExperimentDataEntry entry) {
		return Math.abs(entry.getTstat());
	}
	
	@Override
	protected boolean replaceValue(double oldValue, double newValue) {
		return newValue > oldValue;
	}
}
