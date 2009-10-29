package org.apa.data;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table
public class FactorValue {
	@Id
	private PrimaryKey id;
	
//	@ManyToMany(mappedBy="factors")
//	@JoinColumns({
//		@JoinColumn(referencedColumnName = "factor"),
//		@JoinColumn(referencedColumnName = "value")
//	})
//	private Collection<Experiment> experiments;
	
	public Factor getFactor() {
		return id.factor;
	}
	
	public String getValue() {
		return id.value;
	}
	
	public PrimaryKey getId() {
		return id;
	}
	
	@SuppressWarnings("unused") //Used by hibernate
	private FactorValue() {}
	
	public FactorValue(Factor factor, String value) {
		id = new PrimaryKey();
		id.factor = factor;
		id.value = value;
	}
	
//	public Collection<Experiment> getExperiments() {
//		return experiments;
//	}

	@Override
	public String toString() {
		return id.factor + "." + id.value;
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
		FactorValue other = (FactorValue) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public static PrimaryKey createId(Factor factor, String value) {
		PrimaryKey pk = new PrimaryKey();
		pk.factor = factor;
		pk.value = value;
		return pk;
	}
	
	@Embeddable
	public static class PrimaryKey implements Serializable {
		private static final long serialVersionUID = -6350240606681658256L;

		@ManyToOne
		private Factor factor;
		
		@Column
		private String value;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((factor == null) ? 0 : factor.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
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
			if (factor == null) {
				if (other.factor != null)
					return false;
			} else if (!factor.equals(other.factor))
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
	}
}
