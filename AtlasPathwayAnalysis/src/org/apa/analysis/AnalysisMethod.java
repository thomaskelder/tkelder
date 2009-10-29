package org.apa.analysis;

import java.util.Collection;

import org.apa.AtlasException;
import org.apa.data.ExperimentAnalysis;
import org.apa.data.Pathway;

public interface AnalysisMethod {
	public static final String PROP_TOTAL_MEASURED = "Total genes measured";
	public static final String PROP_TOTAL_PATHWAY = "Total genes in pathways";
	public static final String PROP_TOTAL_MEASURED_PATHWAY = "Total genes in pathways and measured";
	public static final String VALUE_MEASURED = "measured";
	public static final String VALUE_SIZE = "size";
	public static final String VALUE_PVALUE = "p-value";
	public static final String VALUE_SCORE = "score";
	
	/**
	 * Get the analysis type identifier
	 */
	public String getType();
	
	/**
	 * Perform the analysis and store the results in the given analysis object.
	 */
	public void performAnalysis(ExperimentAnalysis analysis, Collection<Pathway> pathways) throws AtlasException;
}
