package walkietalkie;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bridgedb.AttributeMapper;
import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.pathvisio.data.XrefWithSymbol;
import org.pathvisio.debug.Logger;
import org.pathvisio.gex.ReporterData;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.visualization.colorset.Criterion;
import org.pathvisio.visualization.colorset.Criterion.CriterionException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class WalkieTalkie {
	public static final String ATTR_LABEL = "label";
	public static final String ATTR_TYPE = "type";
	public static final String TYPE_GENE = "gene";
	public static final String TYPE_PATHWAY = "pathway";
	
	private static final String EDGE = "in_pathway";

	private IDMapper gdb;
	private SimpleGex gex;
	private Criterion criterion;
	private Collection<PathwayInfo> pathways;
	private Parameters par;

	private Set<Xref> sigXrefs;
	private Map<Xref, String> symbols;
	SetMultimap<PathwayInfo, Xref> pathway2xref;
	SetMultimap<Xref, PathwayInfo> xref2pathway;

	public WalkieTalkie(Parameters par, Criterion crit, Collection<PathwayInfo> pathways, IDMapper gdb, SimpleGex gex) throws CriterionException, IDMapperException {
		this.gdb = gdb;
		this.gex = gex;
		this.criterion = crit;
		this.pathways = pathways;
		this.par = par;
		findSigXrefs();
		findPathwayMappings();
	}

	/**
	 * Get all pathways containing minimal one datanode that 
	 * passes the criterion.
	 */
	public Set<PathwayInfo> getIncludedPathways() {
		Set<PathwayInfo> pathways = new HashSet<PathwayInfo>();
		for(Xref x : xref2pathway.keySet()) {
			if(sigXrefs.contains(x)) {
				pathways.addAll(xref2pathway.get(x));
			}
		}
		return pathways;
	}
	
	private void findPathwayMappings() throws IDMapperException {
		//Create mappings
		pathway2xref = new HashMultimap<PathwayInfo, Xref>();
		xref2pathway = new HashMultimap<Xref, PathwayInfo>();
		symbols = new HashMap<Xref, String>();

		for(Xref x : sigXrefs) addSymbol(x, null);
		
		int i = 1;
		for(PathwayInfo pwi : pathways) {
			Logger.log.trace("Processing pathway " + i++ + " out of " + pathways.size());
			Set<Xref> srcRefs = pwi.getXrefs();
			for(Xref src : srcRefs) {
				if(src.getId() == null || src.getDataSource() == null) continue;
				String symbol = pwi.getSymbol(src);
				//Trick for KEGG converted pathways including all synonyms: only use first
				if(symbol.contains(",")) {
					String[] sym = symbol.split(",");
					//Find first id without colon
					for(String s : sym) if(!s.contains(":")) {
						symbol = s;
						break;
					}
				}
				
				if(par.getDataSource().equals(src.getDataSource())) {
					if(gex == null || sigXrefs.contains(src)) {
						pathway2xref.put(pwi, src);
						xref2pathway.put(src, pwi);
					}
				}
				
				for(Xref ref : gdb.mapID(src, par.getDataSource())) {
					addSymbol(ref, symbol);
					if(gex == null) {
						sigXrefs.add(ref);
						pathway2xref.put(pwi, ref);
						xref2pathway.put(ref, pwi);
					} else if(sigXrefs.contains(ref)) { //Only add significant
						pathway2xref.put(pwi, ref);
						xref2pathway.put(ref, pwi);
					}
				}
			}
		}
	}

	private void addSymbol(Xref x, String symbol) throws IDMapperException {
		if(symbol == null || "".equals(symbol)) symbol = symbolFromXref(x);
		
		if(symbol == null || "".equals(symbol)) { //Fallback on id if no symbol
			symbol = x.getId();
		}
		symbols.put(x, symbol);
	}
	
	private String symbolFromXref(Xref x) throws IDMapperException {
		String symbol = null;
		Set<String> res = ((AttributeMapper)gdb).getAttributes(x, "Symbol");
		if(res.size() > 0) symbol = res.iterator().next();
		return symbol;
	}
	
	private void findSigXrefs() throws CriterionException, IDMapperException {
		sigXrefs = new HashSet<Xref>();
		if(gex != null) {
			//Collect all significant genes in the dataset
			for(int i = 0; i < gex.getNrRow(); i++) {
				ReporterData row = gex.getRow(i);
				Xref reporter = row.getXref();
				if(criterion == null || criterion.evaluate(row.getByName())) {
					//Significant rpeporter, add gene to set
					if(reporter.getDataSource() != par.getDataSource()) {
						sigXrefs.addAll(gdb.mapID(reporter, par.getDataSource()));
					} else {
						sigXrefs.add(reporter);
					}
				}
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
	public void writeSif(Writer out, Collection<PathwayInfo> filterPathways) throws CriterionException, IOException {
		Logger.log.info(sigXrefs.size() + " significant reporters");

		//Use a copy of xref2pathway, because we are going to filter
		//out pathways (and we don't want them to be removed from the master map).
		SetMultimap<Xref, PathwayInfo> myXref2pathway = new HashMultimap<Xref, PathwayInfo>(xref2pathway);
		
		SetMultimap<Xref, PathwayInfo> filtered = new HashMultimap<Xref, PathwayInfo>();

		//If pathway filter is specified, only include this pathway
		//and first neighbors
		if(filterPathways != null && filterPathways.size() > 0) {
			Set<PathwayInfo> excludePathways = new HashSet<PathwayInfo>(pathways);
			for(PathwayInfo pwi : filterPathways) {
				excludePathways.remove(pwi);
				if(par.firstNeighbours) {
					for(Xref xref : pathway2xref.get(pwi)) {
						for(PathwayInfo linkPwi : xref2pathway.get(xref)) {
							excludePathways.remove(linkPwi);
						}
					}
				}
			}
			for(Xref x : sigXrefs) {
				myXref2pathway.get(x).removeAll(excludePathways);
			}
		}

		//Only include genes with enough connections
		for(Xref xref : myXref2pathway.keySet()) {
			Set<PathwayInfo> pwMapping = myXref2pathway.get(xref);
			if(pwMapping.size() >= par.getMinGeneConnections()) {
				//Add the mapping to the filtered list
				filtered.putAll(xref, pwMapping);
			}
		}

		//Write the mappings to sif
		for(Xref xref : filtered.keySet()) {
			Set<PathwayInfo> pws = myXref2pathway.get(xref);
			
			for(PathwayInfo pwi : pws) {
				out.append(xref.getId());
				out.append("\t");
				out.append(EDGE);
				out.append("\t");
				out.append(pwi.getFile().getName());
				out.append("\n");
			}
		}
		
		//In case minConnections == 0, also include xrefs without pathway annotation
		if(par.getMinGeneConnections() == 0) {
			for(Xref xref : sigXrefs) {
				if(!myXref2pathway.containsKey(xref)) out.append(xref.getId() + "\n");
			}
		}
	}

	public void writeLabelAttributes(Writer out) throws IOException {
		out.append(ATTR_LABEL + "\n");
		for(Xref x : sigXrefs) {
			String symbol = symbols.get(x);
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
		out.append(ATTR_TYPE + "\n");
		for(Xref x : sigXrefs) {
			out.append(x.getId());
			out.append(" = ");
			out.append(TYPE_GENE);
			out.append("\n");
		}

		for(PathwayInfo pwi : pathways) {
			out.append(pwi.getFile().getName());
			out.append(" = ");
			out.append(TYPE_PATHWAY);
			out.append("\n");
		}
	}

	public static class Parameters {
		/**
		 * Minimal number of connections (to pathway nodes) a
		 * gene needs to be included.
		 */
		private int minGeneConnections = 2;
		private DataSource dataSource = BioDataSource.ENTREZ_GENE;
		private boolean firstNeighbours = false;
		Set<DataSource> dsSet;
		
		private Parameters() {}
		static Parameters create() { return new Parameters(); }
		Parameters minGeneConnections(int m) { minGeneConnections = m; return this; }
		Parameters dataSource(DataSource ds) { 
			dataSource = ds;
			dsSet = new HashSet<DataSource>();
			dsSet.add(ds);
			return this; 
		}
		Parameters firstNeighbours(boolean n) { firstNeighbours = n; return this; }
		
		DataSource getDataSource() {
			return dataSource;
		}
		Set<DataSource> getDataSourceSet() {
			return dsSet;
		}
		int getMinGeneConnections() {
			return minGeneConnections;
		}
		boolean getFirstNeighbours() {
			return firstNeighbours;
		}
	}

	public static class PathwayInfo {
		private File file;
		private String name;
		private Map<Xref, String> symbols;
		private Set<Xref> xrefs;

		public PathwayInfo(File file, String name, Collection<XrefWithSymbol> xrefs) {
			this.file = file;
			this.name = name;
			this.xrefs = new HashSet<Xref>();
			this.symbols = new HashMap<Xref, String>();
			for(XrefWithSymbol xs : xrefs) {
				Xref xref = new Xref(xs.getId(), xs.getDataSource());
				this.xrefs.add(xref);
				this.symbols.put(xref, xs.getSymbol());
			}
		}

		public String getName() {
			return name;
		}

		public File getFile() {
			return file;
		}

		public Set<Xref> getXrefs() {
			return xrefs;
		}

		public String getSymbol(Xref xref) {
			return symbols.get(xref);
		}

		public int hashCode() {
			return file.hashCode();
		}

		public boolean equals(Object obj) {
			if(obj instanceof PathwayInfo) {
				return file.equals(((PathwayInfo)obj).file);
			}
			return false;
		}
	}
}
