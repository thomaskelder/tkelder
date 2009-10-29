package org.apa.rest;

import java.util.HashMap;
import java.util.Map;

/**
 * http://www.ebi.ac.uk/gxa/api?experiment=E-AFMX-6&gene=ENSG00000154734
 */
public class AtlasExperimentData extends AtlasExperiment {
	private Map<String, AtlasGeneExpression> geneExpressions = new HashMap<String, AtlasGeneExpression>();
	private Map<String, AtlasExpressionStatistics> geneExpressionStatistics = new HashMap<String, AtlasExpressionStatistics>();
	
	public Map<String, AtlasGeneExpression> getGeneExpressions() {
		return geneExpressions;
	}
	public void setGeneExpressions(Map<String, AtlasGeneExpression> geneExpressions) {
		this.geneExpressions = geneExpressions;
	}
	public Map<String, AtlasExpressionStatistics> getGeneExpressionStatistics() {
		return geneExpressionStatistics;
	}
	public void setGeneExpressionStatistics(
			Map<String, AtlasExpressionStatistics> geneExpressionStatistics) {
		this.geneExpressionStatistics = geneExpressionStatistics;
	}
}
