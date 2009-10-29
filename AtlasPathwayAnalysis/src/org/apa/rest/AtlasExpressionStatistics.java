package org.apa.rest;

import java.util.Map;

public class AtlasExpressionStatistics {
	private Map<String, Map<String, AtlasStatGene[]>> genes;

	public Map<String, Map<String, AtlasStatGene[]>> getGenes() {
		return genes;
	}

	public void setGenes(Map<String, Map<String, AtlasStatGene[]>> genes) {
		this.genes = genes;
	}
	
}
