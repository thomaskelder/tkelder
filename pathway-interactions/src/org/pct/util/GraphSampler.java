package org.pct.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.pct.model.Graph;
import org.pct.util.XrefData.WeightProvider;


/**
 * Randomly sample nodes from a graph with a similar degree.
 */
public class GraphSampler<N> {
	private final static Logger log = Logger.getLogger(GraphSampler.class.getName());

	private int minSetSize = 50; //Minimum size of set of similar nodes
	private double fuzzyness = 0.1; //defines range of 'similar' degree. E.g. if 0.1, nodes with degree between 0.9*d and 1.1*d are considered similar.

	private Random random = new Random();
	private Graph<N, ?> graph;
	private Map<Double, List<N>> sampleNodes = new HashMap<Double, List<N>>();
	private WeightProvider<N> weights;

	public GraphSampler(Graph<N, ?> graph, WeightProvider<N> weights) {
		this.graph = graph;
		this.weights = weights;
		rebuild();
	}

	public GraphSampler(Graph<N, ?> graph) {
		this(graph, null);
	}

	public N sampleNode(N n) {
		List<N> s = sampleNodes.get(getDegree(n));
		N sn = s.get(random.nextInt(s.size()));
		while(sn == n) { //Never return the input node
			sn = s.get(random.nextInt(s.size()));
		}
		return sn;
	}

	public void rebuild() {
		log.info("Building node map for sampling by degree...");
		sampleNodes.clear();

		double maxDegree = 0;

		//Build a map that sorts the nodes by their degree
		SortedMap<Double, Set<N>> nodesByDegree = new TreeMap<Double, Set<N>>();
		SortedSet<Double> degrees = new TreeSet<Double>();
		for(N n : graph.getNodes()) {
			double degree = getDegree(n);
			degrees.add(degree);
			maxDegree = Math.max(degree, maxDegree);
			Set<N> nodes = nodesByDegree.get(degree);
			if(nodes == null) nodesByDegree.put(degree, nodes = new HashSet<N>());
			nodes.add(n);
		}

		//For each degree, create a set of similar nodes to sample from
		for(N n : graph.getNodes()) {
			double degree = getDegree(n);

			if(sampleNodes.containsKey(degree)) {
				continue; //A set was already created for this degree
			}

			double low = degree * (1 - fuzzyness);
			double high = degree * (1 + fuzzyness);

			Set<N> similarNodes = new HashSet<N>();

			log.fine("Finding nodes with similar degree for " + n + " (" + degree + 
					"), looking from " + low + " to " + high);
			SortedMap<Double, Set<N>> sub = nodesByDegree.subMap(low, high);
			for(Set<N> ns : sub.values()) similarNodes.addAll(ns);

			if(similarNodes.size() < minSetSize) {
				SortedSet<Double> head = new TreeSet<Double>(degrees.headSet(degree));
				SortedSet<Double> tail = new TreeSet<Double>(degrees.tailSet(degree));
				log.fine("Head: " + head.size());
				log.fine("Tail: " + tail.size());
				while(true) {
					if(similarNodes.size() >= minSetSize) break;
					if(head.size() == 0 && tail.size() == 0) {
						log.warning("Couldn't find enough similar nodes for " + n);
						break;
					}

					if(head.size() > 0) {
						for(N nn : nodesByDegree.get(head.last())) {
							if(similarNodes.size() >= minSetSize) break;
							similarNodes.add(nn);
						}
						head.remove(Double.valueOf(head.last()));
					}
					if(tail.size() > 0) {
						for(N nn : nodesByDegree.get(tail.first())) {
							if(similarNodes.size() >= minSetSize) break;
							similarNodes.add(nn);
						}
						tail.remove(Double.valueOf(tail.first()));
					}
				}
			}

			sampleNodes.put(degree, new ArrayList<N>(similarNodes));
		}
		log.fine("Nodes ordered by (weighted) degree in map of size " + sampleNodes.size() + ":");
		for(double d : sampleNodes.keySet()) {
			log.fine(d + ": " + sampleNodes.get(d).size());
		}
		log.info("Done!");
	}

	private double getDegree(N n) {
		double degree = graph.getDegree(n);
		if(weights != null) { //Use weighted degree intsead
			degree = 0;
			for(N nb : graph.getNeighbors(n)) {
				double w = weights.getWeight(n, nb);
				degree += w;
			}

		}
		return degree;
	}
}