package org.pct.scripts;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bridgedb.Xref;
import org.jgrapht.graph.Pseudograph;
import org.pct.model.Graph;
import org.pct.model.JgtGraph;
import org.pct.model.Network;
import org.pct.util.ArgsData;
import org.pct.util.ArgsParser;
import org.pct.util.ArgsData.DCrossTalk;
import org.pct.util.ArgsData.DIDMapper;
import org.pct.util.ArgsData.DNetworks;
import org.pct.util.ArgsData.DPathways;
import org.pct.util.ArgsParser.ACrossTalk;
import org.pct.util.ArgsParser.AHelp;
import org.pct.util.ArgsParser.AIDMapper;
import org.pct.util.ArgsParser.ANetwork;
import org.pct.util.ArgsParser.APathways;
import org.pct.util.ArgsParser.AWeights;

public class TestShortestPath {
	private final static Logger log = Logger.getLogger(TestShortestPath.class.getName());

	public static void main(String[] args) {
		try {
			Args pargs = ArgsParser.parse(args, Args.class);
			DIDMapper didm = new DIDMapper(pargs);
			DPathways dpws = new DPathways(pargs, didm);
			DCrossTalk dct = new DCrossTalk(pargs, didm);
			//DWeights dw = new DWeights(pargs, didm);
			DNetworks<Xref, String> dnw = ArgsData.loadCrossTalkInteractions(pargs);

			Map<String, Set<Xref>> pathways = dpws.getPathways();

			JgtGraph<Xref, String> jgt = new JgtGraph<Xref, String>(
					new Pseudograph<Xref, String>(String.class)
			);
			Network<Xref, String> jn = new Network<Xref, String>(jgt);
			jn.merge(dnw.getMergedNetwork());
			
			List<String> pwNames = new ArrayList<String>(pathways.keySet());
			for(int i = 0; i < pwNames.size() - 1; i++) {
				for(int j = i; j < pwNames.size(); j++) {
					
				}
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static <N, E> void breadthFirst(N start, N end, Graph<N, E> graph) {
		LinkedList<N> visited = new LinkedList<N>();
		visited.add(start);
		breadthFirst(end, graph, visited);
	}

	private static <N, E> void breadthFirst(N end, Graph<N, E> graph, LinkedList<N> visited) {
		LinkedList<N> nodes = new LinkedList<N>(graph.getNeighbors(visited.getLast()));
		// examine adjacent nodes
		for (N node : nodes) {
			if (visited.contains(node)) {
				continue;
			}
			if (node.equals(end)) {
				visited.add(node);
				//printPath(visited);
				visited.removeLast();
				break;
			}
		}
		if(visited.size() > 5) {
			//System.out.println("Stopping search, path > 10");
			return;
		}
		// in breadth-first, recursion needs to come after visiting adjacent nodes
		for (N node : nodes) {
			if (visited.contains(node) || node.equals(end)) {
				continue;
			}
			visited.addLast(node);
			breadthFirst(end, graph, visited);
			visited.removeLast();
		}
	}

	private static <N> void printPath(LinkedList<N> visited) {
		for (N node : visited) {
			System.out.print(node);
			System.out.print(" ");
		}
		System.out.println();
	}

	public interface Args extends AIDMapper, AWeights, APathways, ACrossTalk, ANetwork, AHelp {
	}
}
