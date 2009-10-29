package org.apa.rest;

import java.util.Map;

public class AtlasSample {
	Map<String, String> sampleCharacteristics;
	int[] relatedAssays;
	
	public Map<String, String> getSampleCharacteristics() {
		return sampleCharacteristics;
	}
	public void setSampleCharacteristics(Map<String, String> sampleCharacteristics) {
		this.sampleCharacteristics = sampleCharacteristics;
	}
	public int[] getRelatedAssays() {
		return relatedAssays;
	}
	public void setRelatedAssays(int[] relatedAssays) {
		this.relatedAssays = relatedAssays;
	}
}
