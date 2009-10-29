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

import org.hibernate.annotations.MapKey;

@Entity
@Table
public class Experiment {
	@Id
	private String accession;
	
	@Column
	private String desciption;

	@Column
	private String organism;
	
	@ManyToMany
	private Set<Factor> factors = new HashSet<Factor>();
	
	@OneToMany(cascade=CascadeType.ALL)
	@MapKey(columns = { @Column(name="name"), @Column(name="value") })
	private Map<Factor, ExperimentData> data = new HashMap<Factor, ExperimentData>();
	
	public Experiment() { }
	
	public Experiment(String accession) {
		this.accession = accession;
	}
	
	public Set<Factor> getFactors() {
		return factors;
	}
	
	public void setFactors(Set<Factor> factors) {
		this.factors = factors;
	}
	
	public void addFactor(Factor factor) {
		factors.add(factor);
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

	public String getDesciption() {
		return desciption;
	}

	public void setDesciption(String desciption) {
		this.desciption = desciption;
	}
	
	public void setData(Factor factor, ExperimentData data) {
		this.data.put(factor, data);
	}
	
	public ExperimentData getData(Factor f) {
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
