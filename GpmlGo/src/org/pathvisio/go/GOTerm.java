package org.pathvisio.go;
import java.util.HashSet;
import java.util.Set;


public class GOTerm implements Comparable<GOTerm> {
	String id;
	String name;
	Set<String> parents = new HashSet<String>();
	boolean isObsolete;
	
	public GOTerm(String id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public boolean isObsolete() {
		return isObsolete;
	}
	
	public void setObsolete(boolean isObsolete) {
		this.isObsolete = isObsolete;
	}
	
	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	protected void addParent(String id) {
		parents.add(id);
	}
	
	protected Set<String> getParents() {
		return parents;
	}

	protected void setParents(Set<String> p) {
		parents = p;
	}
	
	public String toString() {
		return name + " (" + id + ")";
	}
	
	public boolean equals(Object obj) {
		if(obj instanceof GOTerm) {
			return id.equals(((GOTerm)obj).getId());
		} else {
			return false;
		}
	}
	
	public int hashCode() {
		return id.hashCode();
	}
	
	public int compareTo(GOTerm o) {
		return id.compareTo(o.id);
	}
}