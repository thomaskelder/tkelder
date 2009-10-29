package org.apa.rest;

/**
 * http://www.ebi.ac.uk/gxa/api?experiment=E-AFMX-6
 */
public class AtlasExperiment {
	private AtlasExperimentInfo experimentInfo;
	private AtlasExperimentDesign experimentDesign;
	
	public AtlasExperimentInfo getExperimentInfo() {
		return experimentInfo;
	}

	public void setExperimentInfo(AtlasExperimentInfo experimentInfo) {
		this.experimentInfo = experimentInfo;
	}

	public AtlasExperimentDesign getExperimentDesign() {
		return experimentDesign;
	}

	public void setExperimentDesign(AtlasExperimentDesign experimentDesign) {
		this.experimentDesign = experimentDesign;
	}
}
