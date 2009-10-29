package org.apa.data;

import java.io.Serializable;
import java.util.Collection;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

@Entity
@Table
public class Factor {
	@Id
	private PrimaryKey id;
	
	@ManyToMany(mappedBy="factors")
	@JoinColumns({
		@JoinColumn(referencedColumnName = "name"),
		@JoinColumn(referencedColumnName = "value")
	})
	private Collection<Experiment> experiments;
	
	public String getName() {
		return id.name;
	}
	
	public String getValue() {
		return id.value;
	}
	
	public PrimaryKey getId() {
		return id;
	}
	
	@SuppressWarnings("unused") //Used by hibernate
	private Factor() {}
	
	public Factor(String name, String value) {
		id = new PrimaryKey();
		id.name = name;
		id.value = value;
	}
	
	public Collection<Experiment> getExperiments() {
		return experiments;
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
		Factor other = (Factor) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public static PrimaryKey createId(String name, String value) {
		PrimaryKey pk = new PrimaryKey();
		pk.name = name;
		pk.value = value;
		return pk;
	}
	
	@Embeddable
	public static class PrimaryKey implements Serializable {
		private static final long serialVersionUID = -6350240606681658256L;

		private String name;
		private String value;
	
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
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
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
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
