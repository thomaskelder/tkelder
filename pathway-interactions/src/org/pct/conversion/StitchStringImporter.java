package org.pct.conversion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.bio.Organism;
import org.pathvisio.util.Utils;
import org.pct.io.GmlWriter;
import org.pct.model.AttributeKey;
import org.pct.model.Graph;
import org.pct.model.JungGraph;
import org.pct.model.Network;
import org.pct.util.ArgsParser;
import org.pct.util.ArgsData.DIDMapper;
import org.pct.util.ArgsParser.AHelp;
import org.pct.util.ArgsParser.AIDMapper;

import uk.co.flamingpenguin.jewel.cli.Option;
import edu.uci.ics.jung.graph.DirectedSparseGraph;


/**
 * Import interactions from stitch / string
 *
 */
public class StitchStringImporter {
	private final static Logger log = Logger.getLogger(StitchStringImporter.class.getName());
	
	static final Pattern zeroPattern = Pattern.compile("^0*");
	static Map<Organism, String> species2taxid = new HashMap<Organism, String>();
	
	static {
		BioDataSource.init();
		species2taxid.put(Organism.MusMusculus, "10090");
		species2taxid.put(Organism.HomoSapiens, "9606");
	}
	
	int minScore;
	
	Set<String> excludeSources = new HashSet<String>();
	
	DataSource ensDs;
	DataSource proteinDs;
	DataSource metDs;
	
	Organism organism;
	String taxId;
	
	IDMapper idm;
	
	Map<String, String> protein2gene;
	
	public StitchStringImporter(Organism org, IDMapper idm, Map<String, String> protein2gene) {
		organism = org;
		taxId = species2taxid.get(organism);
		ensDs = BioDataSource.getSpeciesSpecificEnsembl(organism);
		proteinDs = ensDs;
		metDs = BioDataSource.CHEBI;
		
		this.idm = idm;
		this.protein2gene = protein2gene;
	}
	
	public void setMinScore(int minScore) {
		this.minScore = minScore;
	}
	
	public void setExcludeSources(Collection<String> exclude) {
		excludeSources.clear();
		excludeSources.addAll(exclude);
	}
	
	public Network<Xref, String> readInteractions(File inFile) throws IDMapperException, IOException {
		Network<Xref, String> network = new Network<Xref, String>(new JungGraph<Xref, String>(new DirectedSparseGraph<Xref, String>()));
		Graph<Xref, String> graph = network.getGraph();
		network.setTitle(inFile.toString());
		
		Set<Set<Xref>> directedEdges = new HashSet<Set<Xref>>();
		
		//Read the STITCH interactions
		BufferedReader in = new BufferedReader(new FileReader(inFile));
		String line = in.readLine(); //Skip header
		int srcNull = 0;
		while((line = in.readLine()) != null) {
			String[] cols = line.split("\t", 8);
			String a = cols[0];
			String b = cols[1];
			boolean directed = "1".equals(cols[4]);
			
			double score = Double.parseDouble(cols[5]);
			
			if(score < minScore) continue;
			
			Set<Xref> xas = getXref(a);
			if(xas == null) continue;
			Set<Xref> xbs = getXref(b);
			if(xbs == null) continue;
			
			//Add the interactions
			for(Xref xa : xas) {
				graph.addNode(xa);
				for(Xref xb : xbs) {
					graph.addNode(xb);
					String type = cols[2];
					//Source can be in col 6 or 7
					String source = cols[7];
					if(source == null || "".equals(source)) source = cols[6];
					if(source == null || "".equals(source) || excludeSources.contains(source)) continue;
					
					List<String> addedEdges = new ArrayList<String>();
					Set<Xref> endpoints = new HashSet<Xref>();
					endpoints.add(xa);
					endpoints.add(xb);
					
					if(directed || xa.equals(xb)) {
						String edge = xa + "(" + type + "|" + source + ")" + xb;
						graph.addEdge(edge, xa, xb);
						addedEdges.add(edge);
						network.setEdgeAttribute(edge, "Directed", "1");
						directedEdges.add(endpoints);
					} else {
						//Check if a directed edge with endpoints xa and xb already exists
						//Don't overwrite with less specific undirected edge.
						if(directedEdges.contains(endpoints)) continue;
						
						String edge1 = xa + "(" + type + "|" + source + ")" + xb;
						graph.addEdge(edge1, xa, xb);
						String edge2 = xb + "(" + type + "|" + source + ")" + xa;
						graph.addEdge(edge2, xb, xa);
						addedEdges.add(edge1);
						addedEdges.add(edge2);
						network.setEdgeAttribute(edge1, "Directed", "0");
						network.setEdgeAttribute(edge2, "Directed", "0");
					}
					for(String edge : addedEdges) {
						network.setEdgeAttribute(edge, AttributeKey.Interaction.name(), type);
						network.setEdgeAttribute(edge, AttributeKey.Source.name(), source);
						network.setEdgeAttribute(edge, "stitch_string_score", "" + score);
					}
				}
			}
		}
		log.info("Imported " + graph.getNodeCount() + " nodes and " + graph.getEdgeCount() + " edges.");
		in.close();
		return network;
	}
	
	public static void main(String[] args) {
		try {
			Args pargs = ArgsParser.parse(args, Args.class);
			DIDMapper didm = new DIDMapper(pargs);
			
			Organism species = Organism.fromLatinName(pargs.getSpecies());

			//Create ensembl protein -> gene mappings
			Map<String, String> protein2gene = new HashMap<String, String>();
			for(File f : pargs.getEns()) protein2gene.putAll(readEnsemblMappings(f));
			
			StitchStringImporter importer = new StitchStringImporter(species, didm.getIDMapper(), protein2gene);
			
			if(pargs.getExcludeSources() != null) {
				importer.setExcludeSources(pargs.getExcludeSources());
				log.info("Excluding sources: " + pargs.getExcludeSources());
			}
			
			importer.setMinScore(pargs.getMinScore());
			Network<Xref, String> n = importer.readInteractions(pargs.getIn());
			PrintWriter out = new PrintWriter(pargs.getOut());
			if(pargs.getOut().getName().endsWith("gml")) {
				GmlWriter.writeGml(out, n);
			} else {
				n.writeToXGMML(out);
			}
			out.close();
		} catch(Exception e) {
			log.log(Level.SEVERE, "Fatal error", e);
			e.printStackTrace();
		}
	}
	
	Set<Xref> getXref(String id) throws IDMapperException {
		//Find out if id is protein or metabolite
		if(id.startsWith("CID")) {
			//Remove the CID part
			id = id.replace("CID", "");
			//Remove leading zeros
			id = removeLeadingZeros(id);
			Xref x = new Xref(id, BioDataSource.PUBCHEM);
			if(!x.getDataSource().equals(metDs)) {
				return idm.mapID(x, metDs);
			} else {
				return Utils.setOf(x);
			}
		} else {
			//Id is of the form taxcode.identifier
			if(!id.startsWith(taxId)) return null; //Skip other species
			int dot = id.indexOf('.');
			id = id.substring(dot + 1, id.length());
			//Assume it's an ensembl id for now and complain if not
			if(id.startsWith("ENS")) {
				//Find the gene id for the protein id
				String gid = protein2gene.get(id);
				if(gid != null) {
					Xref x = new Xref(gid, ensDs);
					if(ensDs.equals(proteinDs)) {
						return Utils.setOf(x);
					} else {
						return idm.mapID(x, proteinDs);
					}
				} else {
					log.warning("Couldn't find ensembl gene for protein " + id);
				}
 			} else {
 				log.warning("Non-ensembl identifier '" + id + "', not sure what to do with it...");
 			}
		}
		return null;
	}
	
	static Map<String, String> readEnsemblMappings(File f) throws IOException {
		Map<String, String> protein2gene = new HashMap<String, String>();
		BufferedReader in = new BufferedReader(new FileReader(f));
		String line = in.readLine(); //Skip header
		while((line = in.readLine()) != null) {
			String[] cols = line.split("\t", 2);
			if("".equals(cols[1]) || "".equals(cols[0])) continue;
			
			protein2gene.put(cols[1], cols[0]);
		}
		in.close();
		return protein2gene;
	}
	
	static String removeLeadingZeros(String s) {
	    Matcher m = zeroPattern.matcher(s);
	    return m.replaceAll("");
	}
	
	private interface Args extends AIDMapper, AHelp {
		@Option(description = "The path to the stitch 'actions.detailed' file.")
		File getIn();
		
		@Option(description = "The file to write the imported network to")
		File getOut();
		
		@Option(defaultValue = "400", description = "The minimum score an interaction should have to be included.")
		int getMinScore();
		
		@Option(description = "The path(s) to the file(s) that contains ensembl gene -> protein annotations (exported from BioMART).")
		List<File> getEns();
		
		@Option(description = "The species to import (latin name, e.g. Mus musculus).")
		String getSpecies();
		
		@Option(description = "Sources to exclude.")
		List<String> getExcludeSources();
	}
}
