package org.pct.model;

import java.util.Collection;

public interface Graph<N, E> {
	public N getFirst(E e);
	public N getSecond(E e);
	public void addEdge(E e, N n1, N n2);
	public void removeEdge(E e);
	public void removeNode(N n);
	public void addNode(N n);
	public E getEdge(N n1, N n2);
	public Collection<E> getEdges(N n1, N n2);
	public int getDegree(N n);
	public Collection<N> getNeighbors(N n);
	public Collection<N> getNodes();
	public Collection<E> getEdges();
	public int getNodeCount();
	public int getEdgeCount();
	public boolean isDirected();
	public int getNeighborCount(N n);
	public boolean containsNode(N n);
	public boolean containsEdge(E e);
	
	public static interface GraphFactory<N, E> {
		Graph<N, E> createGraph();
	}
}