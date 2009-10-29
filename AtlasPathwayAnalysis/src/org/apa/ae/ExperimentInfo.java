package org.apa.ae;

/**
 * Experiment info for an ArrayExpress experiment.
 * @author thomas
 */
public class ExperimentInfo {
	String organism;
	String accession;
	
	public void setAccession(String accession) {
		this.accession = accession;
	}
	public void setOrganism(String organism) {
		this.organism = organism;
	}
	public String getAccession() {
		return accession;
	}
	public String getOrganism() {
		return organism;
	}
}
