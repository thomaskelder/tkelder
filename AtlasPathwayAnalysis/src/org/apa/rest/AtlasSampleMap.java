package org.apa.rest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.bridgedb.IDMapperException;
import org.pathvisio.gex.Sample;
import org.pathvisio.gex.SimpleGex;

public class AtlasSampleMap {
	private Map<String, Sample> sampleByName = new HashMap<String, Sample>();
	
	public AtlasSampleMap() {
	}
	
	public AtlasSampleMap(SimpleGex gex) throws IDMapperException {
		for(org.pathvisio.gex.Sample s : gex.getSamples().values()) {
			sampleByName.put(s.getName(), s);
		}
	}
	
	public void addSample(Sample s) {
		sampleByName.put(s.getName(), s);
	}
	
	public Collection<Sample> getAllSamples() {
		return sampleByName.values();
	}
	
	public Sample getExprSample(String factor, String factorValue) {
		return sampleByName.get(getExprSampleName(factor, factorValue));
	}
	
	public Sample getPvalueSample(String factor, String factorValue) {
		return sampleByName.get(getPvalueSampleName(factor, factorValue));
	}
	
	public Sample getTstatSample(String factor, String factorValue) {
		return sampleByName.get(getTstatSampleName(factor, factorValue));
	}
	
	public static String getExprSampleName(String factor, String factorValue) {
		return "expr_" + factor + "." + factorValue;
	}
	
	public static String getPvalueSampleName(String factor, String factorValue) {
		return "pvalue_" + factor + "." + factorValue;
	}
	
	public static String getTstatSampleName(String factor, String factorValue) {
		return "tstat_" + factor + "." + factorValue;
	}
}
