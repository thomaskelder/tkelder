package org.apa.rest;

import java.util.Map;

public class AtlasGeneExpression {
	private int[] assays;
	private Map<String, Map<String, Double[]>> genes;
	
	public int[] getAssays() {
		return assays;
	}
	public void setAssays(int[] assays) {
		this.assays = assays;
	}
	public Map<String, Map<String, Double[]>> getGenes() {
		return genes;
	}
	public void setGenes(Map<String, Map<String, Double[]>> genes) {
		this.genes = genes;
	}
}
