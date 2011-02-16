package org.pct.model;

import java.util.SortedSet;
import java.util.TreeSet;

public class MergedPathwayInfo implements PathwayInfo {
	public final static String SEP = "|";
	final SortedSet<String> ids = new TreeSet<String>();

	public String getId() {
		return null;
	}
	
	public String getName() {
		return null;
	}
	
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ids == null) ? 0 : ids.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MergedPathwayInfo other = (MergedPathwayInfo) obj;
		if (ids == null) {
			if (other.ids != null)
				return false;
		} else if (!ids.equals(other.ids))
			return false;
		return true;
	}
}
