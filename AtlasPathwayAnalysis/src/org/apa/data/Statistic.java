package org.apa.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.CollectionOfElements;

@Entity
public class Statistic {
	@Id
	private PrimaryKey id;
	
	@CollectionOfElements
	private Map<String, String> values = new HashMap<String, String>();
	
	public Statistic() { }
	
	public Statistic(Pathway pathway, ExperimentAnalysis analysis) {
		id = new PrimaryKey();
		id.pathway = pathway;
		id.analysis = analysis;
	}
	
	public String getValue(String name) {
		return values.get(name);
	}
	
	public void setValue(String name, String value) {
		values.put(name, value);
	}
	
	public Map<String, String> getValues() {
		return values;
	}
	
	public Pathway getPathway() {
		return id.pathway;
	}
	
	public ExperimentAnalysis getAnalysis() {
		return id.analysis;
	}
	
	@Override
	public String toString() {
		return "Statistic for analysis: " + id.analysis + " and pathway: " + id.pathway;
	}
	
	@Embeddable
	private static class PrimaryKey implements Serializable {
		private static final long serialVersionUID = -2126115369614019790L;

		@ManyToOne
		private ExperimentAnalysis analysis;

		@ManyToOne
		private Pathway pathway;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((analysis == null) ? 0 : analysis.hashCode());
			result = prime * result
					+ ((pathway == null) ? 0 : pathway.hashCode());
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
			if (analysis == null) {
				if (other.analysis != null)
					return false;
			} else if (!analysis.equals(other.analysis))
				return false;
			if (pathway == null) {
				if (other.pathway != null)
					return false;
			} else if (!pathway.equals(other.pathway))
				return false;
			return true;
		}
	}
}
