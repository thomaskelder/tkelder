package org.pct.scripts;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.jgrapht.Graph;
import org.jgrapht.alg.BronKerboschCliqueFinder;
import org.jgrapht.graph.SimpleGraph;
import org.pct.model.AttributeKey;
import org.pct.model.JgtGraph;
import org.pct.model.Network;


public class CliqueFinder {
	private final static Logger log = Logger.getLogger(CliqueFinder.class.getName());

	public static void main(String[] args) {
		try {
			File f = new File("/home/thomas/code/PathwayCrosstalk/output/crosstalk-wp.psmi-stitch-string-pazar-single.xgmml");
			double maxP = 0.001;
			
			log.info("Reading network " + f);
			Graph<String, String> graph = new SimpleGraph<String, String>(String.class);
			Network<String, String> network = new Network<String, String>(
					new JgtGraph<String, String>(graph));
			network.readFromXGMML(new FileReader(f), Network.defaultFactory, Network.defaultFactory);
			
			log.info("Removing edges with p > " + maxP);
			int count = 0;
			for(String e : new ArrayList<String>(network.getGraph().getEdges())) {
				double p = Double.parseDouble(network.getEdgeAttribute(e, AttributeKey.Pvalue.name()));
				if(p > maxP) {
					network.getGraph().removeEdge(e);
					count++;
				}
			}
			log.info("Removed " + count + " edges");
			
			log.info("Finding cliques");
			BronKerboschCliqueFinder<String, String> cfinder = new BronKerboschCliqueFinder<String, String>(graph);
			List<Set<String>> cliques = new ArrayList<Set<String>>(cfinder.getAllMaximalCliques());
			
			Collections.sort(cliques, new Comparator<Set<String>>() {
				public int compare(Set<String> s1, Set<String> s2) {
					return s2.size() - s1.size();
				}
			});
			
			log.info("Writing report file");
			PrintWriter out = new PrintWriter(
				new File("/home/thomas/code/PathwayCrosstalk/output/analysis/cliques-" + f.getName() + ".txt")
			);
			
			out.println(cliques.size() + " cliques found.");
			int i = 0;
			for(Set<String> c : cliques) {
				for(String s : c) out.println(s + "\t" + i);
				i++;
			}
			out.close();
		} catch(Exception e) {
			e.printStackTrace();
		}

	}
}
