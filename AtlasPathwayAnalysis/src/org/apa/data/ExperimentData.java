package org.apa.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
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
	
	public ExperimentData(Experiment experiment, Factor factor) {
		id = new PrimaryKey();
		id.experiment = experiment;
		id.factor = factor;
	}
	
	public Factor getFactor() {
		return id.factor;
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
	
	@Embeddable
	static class PrimaryKey implements Serializable {
		private static final long serialVersionUID = 1869044391614542280L;
		@ManyToOne
		Experiment experiment;
		@ManyToOne
		@JoinColumns({
			@JoinColumn(referencedColumnName = "name"),
			@JoinColumn(referencedColumnName = "value")
		})
		Factor factor;
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
