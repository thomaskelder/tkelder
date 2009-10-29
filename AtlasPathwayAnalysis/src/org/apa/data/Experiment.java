package org.apa.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

@Entity
@Table
public class Experiment {
	@Id
	private String accession;
	
	@Column()
	private String name;
	
	@Column(length = 2147483647)
	@Type(type = "text")
	private String desciption;

	@Column
	private String organism;

	@OneToMany(cascade=CascadeType.ALL)
	Map<FactorValue, ExperimentData> data = new HashMap<FactorValue, ExperimentData>();
	
	@ManyToMany
	Set<FactorValue> factorValues = new HashSet<FactorValue>();
	
	public Experiment() { }
	
	public Experiment(String accession) {
		this.accession = accession;
	}
	
	public Set<FactorValue> getFactorValues() {
		return factorValues;
	}
	
	public void setFactorValues(Set<FactorValue> factors) {
		this.factorValues = factors;
	}
	
	public void addFactorValue(FactorValue factorValue) {
		factorValues.add(factorValue);
	}
	
	public Set<ExperimentData> getData(Factor factor) {
		Set<ExperimentData> result = new HashSet<ExperimentData>();
		for(ExperimentData d : data.values()) {
			if(d.getFactorValue().getFactor().equals(factor)) {
				result.add(d);
			}
		}
		return result;
	}
	
	public Set<Factor> getFactors() {
		Set<Factor> factors = new HashSet<Factor>();
		for(FactorValue fv : factorValues) factors.add(fv.getFactor());
		return factors;
	}
	
	public String getOrganism() {
		return organism;
	}
	
	public void setOrganism(String organism) {
		this.organism = organism;
	}

	public String getAccession() {
		return accession;
	}

	public void setAccession(String accession) {
		this.accession = accession;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDesciption() {
		return desciption;
	}

	public void setDesciption(String desciption) {
		this.desciption = desciption;
	}
	
	public void setData(FactorValue factor, ExperimentData data) {
		this.data.put(factor, data);
	}
	
	public ExperimentData getData(FactorValue f) {
		return data.get(f);
	}

	@Override
	public String toString() {
		return accession;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((accession == null) ? 0 : accession.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Experiment other = (Experiment) obj;
		if (accession == null) {
			if (other.accession != null)
				return false;
		} else if (!accession.equals(other.accession))
			return false;
		return true;
	}
}
