package org.apa.data;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.CollectionOfElements;

@Entity
public class Pathway {
	@Id
	private String id;
	@Column
	private String name;
	@Column
	private String organism;
	@Column
	private String url;
	@Column
	private String localFile;
	@CollectionOfElements
	private Set<String> genes;
	
	Pathway() { }
    
	public Pathway(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public String getOrganism() {
		return organism;
	}

	public void setOrganism(String organism) {
		this.organism = organism;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getLocalFile() {
		return localFile;
	}

	public void setLocalFile(String localFile) {
		this.localFile = localFile;
	}

	public Set<String> getGenes() {
		return genes;
	}

	public void setGenes(Set<String> genes) {
		this.genes = genes;
	}
	
	@Override
	public String toString() {
		return name + " (" + id + ")";
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
		Pathway other = (Pathway) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
