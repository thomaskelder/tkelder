package org.apa.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.Parent;

@Entity
public class ExperimentData {
	@Id
	PrimaryKey id;
	
	@CollectionOfElements
	List<ExperimentDataEntry> entries = new ArrayList<ExperimentDataEntry>();
	
	@SuppressWarnings("unused") //Used by hibernate
	private ExperimentData() {}
	
	public ExperimentData(Experiment experiment, FactorValue factorValue) {
		id = new PrimaryKey();
		id.experiment = experiment;
		id.factorValue = factorValue;
	}
	
	public FactorValue getFactorValue() {
		return id.factorValue;
	}
	
	public Experiment getExperiment() {
		return id.experiment;
	}
	
	public List<ExperimentDataEntry> getEntries() {
		return entries;
	}
	
	public void addEntry(String gene, double pvalue, double tstat, double expression) {
		ExperimentDataEntry entry = new ExperimentDataEntry(
				gene, this, pvalue, tstat, expression
		);
		entries.add(entry);
	}
	
	public static PrimaryKey createId(FactorValue factorValue, Experiment experiment) {
		PrimaryKey pk = new PrimaryKey();
		pk.experiment = experiment;
		pk.factorValue = factorValue;
		return pk;
	}
	
	@Embeddable
	static class PrimaryKey implements Serializable {
		private static final long serialVersionUID = 1869044391614542280L;
		@ManyToOne
		Experiment experiment;
		@ManyToOne
		FactorValue factorValue;
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((experiment == null) ? 0 : experiment.hashCode());
			result = prime * result
					+ ((factorValue == null) ? 0 : factorValue.hashCode());
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
			if (factorValue == null) {
				if (other.factorValue != null)
					return false;
			} else if (!factorValue.equals(other.factorValue))
				return false;
			return true;
		}
	}
	
	@Embeddable
	public static class ExperimentDataEntry implements Serializable {
		private static final long serialVersionUID = 3838794383083233782L;
		@Parent
		ExperimentData parent;
		@Column
		String gene;
		@Column
		private String pvalue;
		@Column
		private String tstat;
		@Column
		private String expression;
		
		@SuppressWarnings("unused") //Used by hibernate
		private ExperimentDataEntry() { }
		
		public ExperimentDataEntry(String gene, ExperimentData parent, double pvalue, double tstat, double expression) {
			this.parent = parent;
			this.gene = gene;
			this.pvalue = "" + pvalue;
			this.tstat = "" + tstat;
			this.expression = "" + expression;
		}
		
		public String getGene() {
			return gene;
		}
		
		public double getPvalue() {
			return Double.parseDouble(pvalue);
		}
		
		public double getTstat() {
			return Double.parseDouble(tstat);
		}
		
		public double getExpression() {
			return Double.parseDouble(expression);
		}
		
		public ExperimentData getParent() {
			return parent;
		}
		
		public void setParent(ExperimentData parent) {
			this.parent = parent;
		}
	}
}
