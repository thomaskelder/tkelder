package org.pct.scripts;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bridgedb.Xref;
import org.pct.model.JungGraph;
import org.pct.model.Network;
import org.pct.util.ArgsParser;
import org.pct.util.ArgsData.DIDMapper;
import org.pct.util.ArgsData.DPathways;
import org.pct.util.ArgsParser.AHelp;
import org.pct.util.ArgsParser.AIDMapper;
import org.pct.util.ArgsParser.APathways;

import uk.co.flamingpenguin.jewel.cli.Option;

/**
 * For each pathway, find out how many genes are present in the interaction network.
 */
public class PathwayInteractionCoverage {
	private final static Logger log = Logger.getLogger(PathwayInteractionCoverage.class.getName());

	public static void main(String[] args) {
		try {
			Args pargs = ArgsParser.parse(args, Args.class);
			DIDMapper didm = new DIDMapper(pargs);
			DPathways dpws = new DPathways(pargs, didm);

			//Read the interactions networks and merge
			log.info("Reading interaction networks");
			Network<Xref, String> interactions = new Network<Xref, String>(new JungGraph<Xref, String>());
			for(File f : pargs.getNetworkFiles()) {
				log.fine("Reading interaction network: " + f);
				Network<Xref, String> i = new Network<Xref, String>(new JungGraph<Xref, String>());
				i.readFromXGMML(
						new FileReader(f), Network.xrefFactory, Network.defaultFactory, false
				);
				interactions.merge(i);
			}
			
			//For each pathway, classify it into metabolic or not
			Map<String, String> classes = new HashMap<String, String>();
			
			for(String name : dpws.getPathways().keySet()) {
				String lname = name.toLowerCase();
				boolean m = false;
				if(lname.contains("synthesis")) m = true;
				if(lname.contains("degratation")) m = true;
				if(lname.contains("metabolism")) m = true;
				if(lname.contains("tca")) m = true;
				if(lname.contains("pentose")) m = true;
				if(m) classes.put(name, "metabolic");
			}
			
			//For each pathway, count how many datanodes are in the network
			final List<String> pathwayNames = new ArrayList<String>(dpws.getPathways().keySet());
			final List<Integer> counts = new ArrayList<Integer>();
			Set<Xref> allPathwayXrefs = new HashSet<Xref>();
			for(String name : pathwayNames) {
				Set<Xref> pathway = dpws.getPathways().get(name);
				int count = 0;
				for(Xref x : pathway) if(interactions.getGraph().containsNode(x)) count++;
				counts.add(count);
				
				allPathwayXrefs.addAll(pathway);
			}
			
			//Use the counts to sort (highest count on top)
			Collections.sort(pathwayNames, new Comparator<String>() {
				public int compare(String p1, String p2) {
					return counts.get(pathwayNames.indexOf(p2)) - counts.get(pathwayNames.indexOf(p1));
				}
			});
			
			Set<Xref> inPathwayAndInteractions = new HashSet<Xref>(allPathwayXrefs);
			inPathwayAndInteractions.retainAll(interactions.getGraph().getNodes());
			
			PrintWriter out = new PrintWriter(pargs.getOut());
			String nfn = ""; for(File f : pargs.getNetworkFiles()) nfn += f.getName() + "; ";
			out.println("Interaction network: " + nfn);
			out.println("\tNr edges: " + interactions.getGraph().getEdgeCount());
			out.println("\tNr nodes: " + interactions.getGraph().getNodeCount());
			out.println("Nr nodes in all pathways: " + allPathwayXrefs.size());
			out.println("Nr nodes both in pathway and interaction network: " + inPathwayAndInteractions.size());
			out.println();
			out.println("Pathway\tNr xrefs (mapped to common datasource)\tNr xrefs in interaction network\tPathway class");
			for(String name : pathwayNames) {
				Set<Xref> pathway = dpws.getPathways().get(name);
				int count = 0;
				for(Xref x : pathway) if(interactions.getGraph().containsNode(x)) count++;
				String c = classes.get(name);
				if(c == null) c = "";
				out.println(name + "\t" + pathway.size() + "\t" + count + "\t" + c);
			}
			
			out.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private interface Args extends APathways, AIDMapper, AHelp {
		@Option(description = "The output file.")
		File getOut();

		@Option(shortName = "n", description = "The xgmml file(s) of interaction network(s).")
		List<File> getNetworkFiles();
	}
}
