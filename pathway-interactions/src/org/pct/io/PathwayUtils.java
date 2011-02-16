package org.pct.io;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bridgedb.Xref;

public class PathwayUtils {
	private final static Logger log = Logger.getLogger(PathwayUtils.class.getName());
	
	public static Map<String, Set<Xref>> filterPathways(Map<String, Set<Xref>> pathways, PathwayFilter filter) {
		log.info("Filtering pathways based on criteria: " + filter);

		Map<String, Set<Xref>> filtered = new HashMap<String, Set<Xref>>();
		
		//First filter by size
		for(String pn : pathways.keySet()) {
			Set<Xref> p = pathways.get(pn);
			if(p.size() < filter.minXrefs) {
				log.info("Filtering out because it contains too few xrefs: " + pn + " (" + p.size() + ")");
				continue;
			}
			if(p.size() > filter.maxXrefs) {
				log.info("Filtering out because it contains too many xrefs: " + pn + " (" + p.size() + ")");
				continue;
			}
			filtered.put(pn, p);
		}
		return filtered;
	}
	
	public static class PathwayFilter {
		private int minXrefs = 1;
		private int maxXrefs = Integer.MAX_VALUE;
		
		public PathwayFilter setMaxXrefs(int maxXrefs) {
			this.maxXrefs = maxXrefs; return this;
		}
		public PathwayFilter setMinXrefs(int minXrefs) {
			this.minXrefs = minXrefs; return this;
		}
		public int getMaxXrefs() {
			return maxXrefs;
		}
		public int getMinXrefs() {
			return minXrefs;
		}
		
		public String toString() {
			return "PathwayFilter:\n" +
			"min nr xrefs: " + minXrefs + "\n" +
			"max nr xrefs" + maxXrefs + "\n";
		}
	}
}
