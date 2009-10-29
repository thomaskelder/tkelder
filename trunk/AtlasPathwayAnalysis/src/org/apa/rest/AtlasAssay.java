package org.apa.rest;

import java.util.Map;

public class AtlasAssay {
	private Map<String, String> factorValues;
	private String arrayDesign;
	private int[] relatedSamples;
	
	public Map<String, String> getFactorValues() {
		return factorValues;
	}
	public void setFactorValues(Map<String, String> factorValues) {
		this.factorValues = factorValues;
	}
	public String getArrayDesign() {
		return arrayDesign;
	}
	public void setArrayDesign(String arrayDesign) {
		this.arrayDesign = arrayDesign;
	}
	public int[] getRelatedSamples() {
		return relatedSamples;
	}
	public void setRelatedSamples(int[] relatedSamples) {
		this.relatedSamples = relatedSamples;
	}
}