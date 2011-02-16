package org.pct.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import org.apache.commons.collections15.Predicate;
import org.pct.model.AttributeKey;
import org.pct.model.Graph;
import org.pct.model.JungGraph;
import org.pct.model.Network;
import org.pct.util.ArgsParser.AHelp;

import uk.co.flamingpenguin.jewel.cli.Option;
import edu.uci.ics.jung.algorithms.filters.EdgePredicateFilter;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

/**
 * Filter a network by attribute values.
 * @author thomas
 *
 */
public class FilterNetwork {
	public static <N, E> void filter(Graph<N, E> graph, Filter<N> nodeFilter, Filter<E> edgeFilter) {
		if(nodeFilter != null) {
			for(N n : new ArrayList<N>(graph.getNodes())) {
				if(!nodeFilter.evaluate(n)) graph.removeNode(n);
			}
		}
		if(edgeFilter != null) {
			for(E e : new ArrayList<E>(graph.getEdges())) {
				if(!edgeFilter.evaluate(e)) graph.removeEdge(e);
			}
		}
	}
	
	public static <N, E> void filterByPvalue(final Network<N, E> network, final double maxP) {
		filter(network.getGraph(), null, new Filter<E>() {
			public boolean evaluate(E e) {
				double p = Double.parseDouble(network.getEdgeAttribute(e, AttributeKey.Pvalue.name()));
				return p <= maxP;
			};
		});
	}
	
	public static <N, E> void filterByOverlap(final Network<N, E> network, final double maxOverlap) {
		filter(network.getGraph(), null, new Filter<E>() {
			public boolean evaluate(E e) {
				double o = Double.parseDouble(network.getEdgeAttribute(e, AttributeKey.Pvalue.name()));
				return o <= maxOverlap;
			};
		});
	}
	
	public interface Filter<O> {
		public boolean evaluate(O o);
	}
	
	public static void main(String[] args) {
		try {
			final Args pargs = ArgsParser.parse(args, Args.class);
			
			final JungGraph<String, String> jgraph = new JungGraph<String, String>(new UndirectedSparseGraph<String, String>());
			final Network<String, String> network = new Network<String, String>(jgraph);
			network.readFromXGMML(new FileReader(pargs.getIn()), Network.defaultFactory, Network.defaultFactory);
			
			EdgePredicateFilter<String, String> f = new EdgePredicateFilter<String, String>(new Predicate<String>() {
				public boolean evaluate(String e) {
					double pvalue = Double.parseDouble(network.getEdgeAttribute(e, AttributeKey.Pvalue.name()));
					double overlap = Double.parseDouble(network.getEdgeAttribute(e, AttributeKey.Overlap.name()));
					return pvalue <= pargs.getMaxP() && overlap <= pargs.getMaxOverlap();
				}
			});
			network.setGraph(new JungGraph<String, String>(f.transform(jgraph.getJungGraph())));
			network.setTitle(network.getTitle() + " - filtered p<=" + pargs.getMaxP() + ", overlap<=" + pargs.getMaxOverlap());
			FileWriter out = new FileWriter(pargs.getOut());
			network.writeToXGMML(out);
			out.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private interface Args extends AHelp {
		@Option(description = "The directory to write the filtered network to.")
		File getOut();
		
		@Option(description = "The xgmml file of the crosstalk network to filter.")
		File getIn();
		
		@Option(description = "The minimum p-value of the interaction between a pathway pair.")
		double getMaxP();
		
		@Option(description = "The maximum overlap (ratio) between the pathways.")
		double getMaxOverlap();
	}
}
