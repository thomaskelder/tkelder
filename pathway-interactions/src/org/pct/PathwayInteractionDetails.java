package org.pct;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pct.model.AttributeKey;
import org.pct.model.Graph;
import org.pct.model.JungGraph;
import org.pct.model.Network;
import org.pct.model.Graph.GraphFactory;
import org.pct.util.ArgsParser;
import org.pct.util.GpmlUtils;
import org.pct.util.ArgsData.DIDMapper;
import org.pct.util.ArgsData.DNetworks;
import org.pct.util.ArgsData.DPathways;
import org.pct.util.ArgsData.DWeights;
import org.pct.util.ArgsParser.AHelp;
import org.pct.util.ArgsParser.AIDMapper;
import org.pct.util.ArgsParser.ANetwork;
import org.pct.util.ArgsParser.APathways;
import org.pct.util.ArgsParser.AWeights;

import uk.co.flamingpenguin.jewel.cli.Option;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class PathwayInteractionDetails {
	public static final String PATHWAY_ID_SEP = "*";
	private final static Logger log = Logger.getLogger(PathwayInteractionDetails.class.getName());
	
	private GraphFactory<Xref, String> interactionGraphFactory;
	
	public PathwayInteractionDetails() {
		interactionGraphFactory = new GraphFactory<Xref, String>() {
			public Graph<Xref, String> createGraph() {
				return new JungGraph<Xref, String>(new UndirectedSparseGraph<Xref, String>());
			}
		};
	}
	
	public Network<Xref, String> xrefInteractions(Map<String, Set<Xref>> pathways, Network<Xref, String> interactions) {
		Network<Xref, String> network = new Network<Xref, String>(interactionGraphFactory.createGraph());
		
		Set<Xref> xrefs = new HashSet<Xref>();
		for(Set<Xref> px : pathways.values()) {
			xrefs.addAll(px);
		}
		
		interactions.subNetwork(xrefs, network);
		
		for(Entry<String, Set<Xref>> entry : pathways.entrySet()) {
			for(Xref x : entry.getValue()) {
				//Set attribute to indicate from which pathway(s) the xref came
				String patt = network.getNodeAttribute(x, AttributeKey.PathwayId.name());
				if(patt == null) {
					patt = entry.getKey();
				} else {
					patt += PATHWAY_ID_SEP + entry.getKey();
				}
				network.setNodeAttribute(x, AttributeKey.PathwayId.name(), patt);
			}
		}
		
		//Add attribute to indicate if edge is within a single pathway
		for(String e : network.getGraph().getEdges()) {
			Xref x1 = network.getGraph().getFirst(e);
			Xref x2 = network.getGraph().getSecond(e);
			String p1 = network.getNodeAttribute(x1, AttributeKey.PathwayId.name());
			String p2 = network.getNodeAttribute(x2, AttributeKey.PathwayId.name());
			network.setEdgeAttribute(e, AttributeKey.CrossPathway.name(), "" + (p1 == null || !p1.equals(p2)));
		}
		
		for(Xref x : xrefs) {
			network.setNodeAttribute(x, AttributeKey.XrefId.name(), x.getId());
			network.setNodeAttribute(x, AttributeKey.XrefDatasource.name(), x.getDataSource().getFullName());
		}
		
		network.setTitle("xref_interactions_" + pathways.keySet());
		return network;
	}
	
	public static void addMissingSymbols(Network<Xref, String> network, Map<Xref, String> symbols) throws IDMapperException {
		for(Xref x : network.getGraph().getNodes()) {
			String cs = network.getNodeAttribute(x, AttributeKey.Label.name());
			if(cs == null || "".equals(cs)) {
				String s = symbols.get(x);
				log.info("Finding symbol for " + x + ": " + s);
				if(s != null || "".equals(s)) network.setNodeAttribute(x, AttributeKey.Label.name(), s);
			}
		}
	}
	
	public static Network<Xref, String> createXrefInteractions(Map<String, Set<Xref>> pathways, DIDMapper didm, 
			DNetworks<Xref, String> dnw, DWeights dw) {
		PathwayInteractionDetails ct = new PathwayInteractionDetails();

		log.info("Creating network");
		Network<Xref, String> network = ct.xrefInteractions(pathways, dnw.getMergedNetwork());
		
		log.info("Writing weights attributes");
		for(Xref x : network.getGraph().getNodes()) {
			if(dw.getWeightValues().containsKey(x)) {
				double d = dw.getWeightValues().get(x);
				network.setNodeAttribute(x, "weight", "" + d);
			}
		}
		for(String e : network.getGraph().getEdges()) {
			Xref x1 = network.getGraph().getFirst(e);
			Xref x2 = network.getGraph().getSecond(e);
			network.setEdgeAttribute(e, "weight", "" + dw.getWeightProvider().getWeight(x1, x2));
		}
		
		return network;
	}
	
	public static void main(String[] args) {
		try {
			Args pargs = ArgsParser.parse(args, Args.class);
			DIDMapper didm = new DIDMapper(pargs);
			DPathways dpws = new DPathways(pargs, didm);
			DNetworks<Xref, String> dnw = new DNetworks<Xref, String>(
					pargs, PathwayCrossTalk.defaultInteractionFactory, Network.xrefFactory, Network.defaultFactory, true
			);
			DWeights dw = new DWeights(pargs, didm);

			Network<Xref, String> network = createXrefInteractions(dpws.getPathways(), didm, dnw, dw);
			
			addMissingSymbols(network, GpmlUtils.readSymbols(dpws.getPathwayFiles(), didm.getIDMapper(), didm.getDataSources()));
			FileWriter out = new FileWriter(pargs.getOut());
			network.writeToXGMML(out);
			out.close();
		} catch(Exception e) {
			log.log(Level.SEVERE, "Fatal error", e);
			e.printStackTrace();
		}
	}
	
	interface Args extends ANetwork, APathways, AIDMapper, AHelp, AWeights {
		@Option(shortName = "o", description = "The output file.")
		public File getOut();
	}
}
