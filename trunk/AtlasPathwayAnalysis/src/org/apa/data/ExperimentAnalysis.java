package org.apa.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.MapKey;

@Entity
public class ExperimentAnalysis {
	@Id
	PrimaryKey id;
	
	@OneToMany(cascade=CascadeType.ALL)
	@Cascade(org.hibernate.annotations.CascadeType.MERGE)
	@MapKey(columns = @Column(name="id"))
	Map<Pathway, Statistic> entries = new HashMap<Pathway, Statistic>();
	
	@CollectionOfElements
	Map<String, String> properties = new HashMap<String, String>();
	
	@SuppressWarnings("unused") //Used by hibernate
	private ExperimentAnalysis() { }
	
	public ExperimentAnalysis(Experiment experiment, Factor factor, String type) {
		id = new PrimaryKey();
		id.experiment = experiment;
		id.factor = factor;
		id.type = type;
	}
	
	public void setStatistic(Statistic stat) {
		entries.put(stat.getPathway(), stat);
	}
	
	public String getType() {
		return id.type;
	}
	
	public Experiment getExperiment() {
		return id.experiment;
	}
	
	public Factor getFactor() {
		return id.factor;
	}
	
	public Statistic getStatistic(Pathway pathway) {
		return entries.get(pathway);
	}
	
	public String getProperty(String name) {
		return properties.get(name);
	}
	
	public void setProperty(String name, String value) {
		properties.put(name, value);
	}
	
	public Map<String, String> getProperties() {
		return properties;
	}
	
	public Map<Pathway, Statistic> getEntries() {
		return entries;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		ExperimentAnalysis other = (ExperimentAnalysis) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Experiment(" + id.experiment + "; " + id.factor + "; " + id.type + ")";
	}
	
	@Embeddable
	public static class PrimaryKey implements Serializable {
		private static final long serialVersionUID = 1869044391614542280L;
		@ManyToOne
		Experiment experiment;
		@ManyToOne
		Factor factor;
		@Column
		String type;
		
		private PrimaryKey() {
		}
		
		public PrimaryKey(Experiment experiment, Factor factor, String type) {
			this.experiment = experiment;
			this.factor = factor;
			this.type = type;
		}
		
		@Override
		public String toString() {
			return type + "." + experiment.getAccession() + "." + factor.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((experiment == null) ? 0 : experiment.hashCode());
			result = prime * result
					+ ((factor == null) ? 0 : factor.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
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
			PrimaryKey other = (PrimaryKey) obj;
			if (experiment == null) {
				if (other.experiment != null)
					return false;
			} else if (!experiment.equals(other.experiment))
				return false;
			if (factor == null) {
				if (other.factor != null)
					return false;
			} else if (!factor.equals(other.factor))
				return false;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}
	}	
}
