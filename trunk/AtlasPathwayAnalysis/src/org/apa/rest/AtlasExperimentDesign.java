package org.apa.rest;

import java.util.HashSet;
import java.util.Set;


public class AtlasExperimentDesign {
	private String[] experimentalFactors;
	private String[] sampleCharacteristics;
	private AtlasSample[] samples;
	private AtlasAssay[] assays;
	private AtlasArrayDesign[] arrayDesigns;
	
	public String[] getExperimentalFactors() {
		return experimentalFactors;
	}
	public void setExperimentalFactors(String[] experimentalFactors) {
		this.experimentalFactors = experimentalFactors;
	}
	public String[] getSampleCharacteristics() {
		return sampleCharacteristics;
	}
	public void setSampleCharacteristics(String[] sampleCharacteristics) {
		this.sampleCharacteristics = sampleCharacteristics;
	}
	public AtlasSample[] getSamples() {
		return samples;
	}
	public void setSamples(AtlasSample[] samples) {
		this.samples = samples;
	}
	public AtlasAssay[] getAssays() {
		return assays;
	}
	public void setAssays(AtlasAssay[] assays) {
		this.assays = assays;
	}
	public AtlasArrayDesign[] getArrayDesigns() {
		return arrayDesigns;
	}
	public void setArrayDesigns(AtlasArrayDesign[] arrayDesigns) {
		this.arrayDesigns = arrayDesigns;
	}
	
	public Set<String> getFactorValues(String factor) {
		Set<String> values = new HashSet<String>();
		
		for(AtlasSample s : samples) {
			String v = s.getSampleCharacteristics().get(factor);
			if(v != null) {
				values.add(v);
			}
		}
		
		return values;
	}
}
