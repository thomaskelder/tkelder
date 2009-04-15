package pps.walkietalkie;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.pathvisio.data.DataException;
import org.pathvisio.data.Gdb;
import org.pathvisio.debug.Logger;
import org.pathvisio.gex.ReporterData;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.model.DataSource;
import org.pathvisio.model.Xref;
import org.pathvisio.plugins.statistics.PathwayMap.PathwayInfo;
import org.pathvisio.visualization.colorset.Criterion;
import org.pathvisio.visualization.colorset.Criterion.CriterionException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class WalkieTalkie {
	private static final String EDGE = "in_pathway";
	
	private Gdb gdb;
	private SimpleGex gex;
	private Criterion criterion;
	private Collection<PathwayInfo> pathways;
	private Parameters par;
	
	private Set<Xref> sigXrefs;
	
	public WalkieTalkie(Parameters par, Criterion crit, Collection<PathwayInfo> pathways, Gdb gdb, SimpleGex gex) throws DataException, CriterionException {
		this.gdb = gdb;
		this.gex = gex;
		this.criterion = crit;
		this.pathways = pathways;
		this.par = par;
		findSigXrefs();
	}
	
	private void findSigXrefs() throws DataException, CriterionException {
		//Collect all significant genes in the dataset
		sigXrefs = new HashSet<Xref>();
		for(int i = 0; i < gex.getMaxRow(); i++) {
			ReporterData row = gex.getRow(i);
			Xref reporter = row.getXref();
			if(criterion == null || criterion.evaluate(row.getByName())) {
				//Significant rpeporter, add gene to set
				sigXrefs.addAll(gdb.getCrossRefs(reporter, par.getDataSource()));
			}
		}
	}
	
	/**
	 * Write a network where a node can be a gene or pathway, an edge
	 * is a relation (contains) between a gene and pathway.
	 * @throws DataException 
	 * @throws CriterionException 
	 * @throws IOException 
	 */
	public void writeSif(Writer out, Collection<PathwayInfo> filterPathways) throws DataException, CriterionException, IOException {
		Logger.log.info(sigXrefs.size() + " significant reporters");
		
		//Create mappings
		SetMultimap<PathwayInfo, Xref> pathway2xref = new HashMultimap<PathwayInfo, Xref>();
		SetMultimap<Xref, PathwayInfo> xref2pathway = new HashMultimap<Xref, PathwayInfo>();
		
		int i = 1;
		for(PathwayInfo pwi : pathways) {
			Logger.log.trace("Processing pathway " + i++ + " out of " + pathways.size());
			Set<Xref> srcRefs = pwi.getSrcRefs();
			for(Xref src : srcRefs) {
				for(Xref ref : gdb.getCrossRefs(src, par.getDataSource())) {
					if(sigXrefs.contains(ref)) { //Only add significant xrefs
						pathway2xref.put(pwi, ref);
						xref2pathway.put(ref, pwi);
					}
				}
			}
		}
		
		SetMultimap<Xref, PathwayInfo> filtered = new HashMultimap<Xref, PathwayInfo>();
		
		//If pathway filter is specified, only include this pathway
		//and first neighbors
		if(filterPathways != null && filterPathways.size() > 0) {
			Set<PathwayInfo> excludePathways = new HashSet<PathwayInfo>(pathways);
			for(PathwayInfo pwi : filterPathways) {
				excludePathways.remove(pwi);
				for(Xref xref : pathway2xref.get(pwi)) {
					for(PathwayInfo linkPwi : xref2pathway.get(xref)) {
						excludePathways.remove(linkPwi);
					}
				}
			}
			for(Xref x : sigXrefs) {
				xref2pathway.get(x).removeAll(excludePathways);
			}
		}
		
		//Only include genes with enough connections
		for(Xref xref : xref2pathway.keySet()) {
			Set<PathwayInfo> pwMapping = xref2pathway.get(xref);
			if(pwMapping.size() >= par.getMinGeneConnections()) {
				//Add the mapipng to the filtered list
				filtered.putAll(xref, pwMapping);
			}
		}
		
		//Write the mappings to sif
		for(Xref xref : filtered.keySet()) {
			for(PathwayInfo pwi : xref2pathway.get(xref)) {
				out.append(pwi.getFile().getName());
				out.append("\t");
				out.append(EDGE);
				out.append("\t");
				out.append(xref.getId());
				out.append("\n");
			}
		}
	}
	
	public void writeLabelAttributes(Writer out) throws IOException, DataException {
		out.append("label\n");
		for(Xref x : sigXrefs) {
			String symbol = gdb.getGeneSymbol(x);
			if(symbol != null) {
				out.append(x.getId());
				out.append(" = ");
				out.append(symbol);
				out.append("\n");
			}
		}
		
		for(PathwayInfo pwi : pathways) {
			out.append(pwi.getFile().getName());
			out.append(" = ");
			out.append(pwi.getName());
			out.append("\n");
		}
	}
	
	public void writeTypeAttributes(Writer out) throws IOException {
		out.append("type\n");
		for(Xref x : sigXrefs) {
			out.append(x.getId());
			out.append(" = ");
			out.append("gene");
			out.append("\n");
		}
		
		for(PathwayInfo pwi : pathways) {
			out.append(pwi.getFile().getName());
			out.append(" = ");
			out.append("pathway");
			out.append("\n");
		}
	}
	
	static class Parameters {
		/**
		 * Minimal number of connections (to pathway nodes) a
		 * gene needs to be included.
		 */
		private int minGeneConnections = 2;
		private DataSource dataSource = DataSource.ENTREZ_GENE;
		
		private Parameters() {}
		static Parameters create() { return new Parameters(); }
		Parameters minGeneConnections(int m) { minGeneConnections = m; return this; }
		Parameters dataSource(DataSource ds) { dataSource = ds; return this; }
		
		DataSource getDataSource() {
			return dataSource;
		}
		int getMinGeneConnections() {
			return minGeneConnections;
		}
	}
}
