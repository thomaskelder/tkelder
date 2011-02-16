package org.pct.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graphs;

public class JgtGraph<N, E> implements Graph<N, E> {
	private org.jgrapht.Graph<N, E> graph;
	
	public JgtGraph(org.jgrapht.Graph<N, E> graph) {
		this.graph = graph;
	}
	
	public org.jgrapht.Graph<N, E> getJgtGraph() {
		return graph;
	}
	
	public void addEdge(E e, N n1, N n2) {
		if(e != null) graph.addEdge(n1, n2, e);
		else graph.addEdge(n1, n2);
	}
	
	public void removeNode(N n) {
		graph.removeVertex(n);
	}
	
	public void addNode(N n) {
		graph.addVertex(n);
	}
	
	public int getDegree(N n) {
		return graph.edgesOf(n).size();
	}
	
	public E getEdge(N n1, N n2) {
		return graph.getEdge(n1, n2);
	}
	
	public boolean containsNode(N n) {
		return graph.containsVertex(n);
	}
	
	public int getEdgeCount() {
		return graph.edgeSet().size();
	}
	
	public Collection<E> getEdges() {
		return graph.edgeSet();
	}
	
	public java.util.Collection<E> getEdges(N n1, N n2) {
		return graph.getAllEdges(n1, n2);
	}
	
	public N getFirst(E e) {
		return graph.getEdgeSource(e);
	}
	
	public java.util.Collection<N> getIncidentNodes(E e) {
		Set<N> nodes = new HashSet<N>();
		nodes.add(graph.getEdgeSource(e));
		nodes.add(graph.getEdgeTarget(e));
		return nodes;
	}
	
	public int getNeighborCount(N n) {
		return Graphs.neighborListOf(graph, n).size();
	}
	
	public java.util.Collection<N> getNeighbors(N n) {
		return Graphs.neighborListOf(graph, n);
	}
	
	public int getNodeCount() {
		return graph.vertexSet().size();
	}
	
	public Collection<N> getNodes() {
		return graph.vertexSet();
	}
	
	public N getSecond(E e) {
		return graph.getEdgeTarget(e);
	}
	
	public boolean isDirected() {
		return graph instanceof DirectedGraph;
	}
	
	public void removeEdge(E e) {
		graph.removeEdge(e);
	}
	
	public boolean containsEdge(E e) {
		return graph.containsEdge(e);
	}
}
