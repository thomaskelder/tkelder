package pps.interaction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class Network {
	Set<Edge> edges = new HashSet<Edge>();
	Set<Node> nodes = new HashSet<Node>();
	
	HashMap<Node, String> symbols = new HashMap<Node, String>();
	
	Multimap<Node, Node> neighbours = new HashMultimap<Node, Node>();
	
	public Network() {
	}
	
	public void addEdge(Edge edge) {
		//make sure the nodes are present
		addNode(edge.getNode1());
		addNode(edge.getNode2());
		
		edges.add(edge);
		neighbours.put(edge.getNode1(), edge.getNode2());
	}
	
	public void addNode(Node node) {
		nodes.add(node);
	}
	
	public String getSymbol(Node node) {
		return symbols.get(node);
	}
	
	public void setSymbol(Node node, String symbol) {
		symbols.put(node, symbol);
	}
	
	public Collection<Node> getFirstNeighbours(Node node) {
		Collection<Node> result = neighbours.get(node);
		return result == null ? new HashSet<Node>() : result;
	}
	
	public Collection<Node> getNodes() {
		return nodes;
	}
	
	public void readSymbols(BufferedReader in) throws IOException {
		String line = null;
		while((line = in.readLine()) != null) {
			if(line.startsWith("#")) continue; //Skip comments
			//Node \t Symbol
			String[] cols = line.split("\t",2);
			Node n = Node.getInstance(cols[0]);
			if(cols[1] != null) {
				setSymbol(n, cols[1]);
			}
		}
	}
	
	public void writeSymbols(Writer out) throws IOException {
		for(Node n : symbols.keySet()) {
			out.write(n.getId());
			out.write("\t");
			out.write(symbols.get(n));
			out.write("\n");
		}
	}
	
	/**
	 * Import from a SIF file.
	 */
	public static Network fromSif(BufferedReader in) throws IOException {
		Network network = new Network();
		
		String line = null;
		while((line = in.readLine()) != null) {
			if(line.startsWith("#")) continue; //Skip comments
			//Node1 \t type \t Node2
			String[] cols = line.split("\t",3);
			network.addEdge(
				new Edge(Node.getInstance(cols[0]), Node.getInstance(cols[2]), cols[1])	
			);
		}
		
		return network;
	}
	
	public void toSif(Writer out) throws IOException {
		for(Edge edge : edges) {
			out.write(edge.getNode1().getId());
			out.write("\t");
			out.write(edge.getType());
			out.write("\t");
			out.write(edge.getNode2().getId());
			out.write("\n");
		}
	}
}
