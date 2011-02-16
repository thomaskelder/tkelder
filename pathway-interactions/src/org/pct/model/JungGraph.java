package org.pct.model;

import java.util.Collection;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class JungGraph<N, E> implements Graph<N, E> {
	private edu.uci.ics.jung.graph.Graph<N, E> graph;
	
	public JungGraph(edu.uci.ics.jung.graph.Graph<N, E> graph) {
		this.graph = graph;
	}
	
	public JungGraph() {
		this.graph = new UndirectedSparseGraph<N, E>();
	}
	
	public edu.uci.ics.jung.graph.Graph<N, E> getJungGraph() {
		return graph;
	}
	
	public void addEdge(E e, N n1, N n2) {
		graph.addEdge(e, n1, n2);
	}
	
	public void addNode(N n) {
		graph.addVertex(n);
	}
	
	public void removeNode(N n) {
		graph.removeVertex(n);
	}
	
	public boolean containsNode(N n) {
		return graph.containsVertex(n);
	}
	
	public int getDegree(N n) {
		return graph.degree(n);
	}
	
	public E getEdge(N n1, N n2) {
		return graph.findEdge(n1, n2);
	}
	
	public int getEdgeCount() {
		return graph.getEdgeCount();
	}
	
	public Collection<E> getEdges() {
		return graph.getEdges();
	}
	
	public java.util.Collection<E> getEdges(N n1, N n2) {
		return graph.findEdgeSet(n1, n2);
	}
	
	public N getFirst(E e) {
		return graph.getEndpoints(e).getFirst();
	}
	
	public N getSecond(E e) {
		return graph.getEndpoints(e).getSecond();
	}
	
	public java.util.Collection<N> getIncidentNodes(E e) {
		return graph.getIncidentVertices(e);
	}
	
	public int getNeighborCount(N n) {
		return graph.getNeighborCount(n);
	}
	
	public java.util.Collection<N> getNeighbors(N n) {
		return graph.getNeighbors(n);
	}
	
	public int getNodeCount() {
		return graph.getVertexCount();
	}
	
	public Collection<N> getNodes() {
		return graph.getVertices();
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
