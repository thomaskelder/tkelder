package org.pct.model;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.bridgedb.DataSource;
import org.bridgedb.Xref;
import org.pct.io.XGMMLContentHandler;
import org.pct.io.XGMMLWriter;
import org.pct.model.Graph.GraphFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class Network<N, E> {
	private final static Logger log = Logger.getLogger(Network.class.getName());
	
	private final Map<Object, Map<String, String>> attributes = 
		new HashMap<Object, Map<String, String>>();
	
	private final Map<String, String> networkAttributes = new HashMap<String, String>();
	
	private String title = "untitiled";
	
	private Graph<N, E> graph;
	
	public Network(Graph<N, E> graph) {
		this.graph = graph;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getTitle() {
		return title;
	}
	
	public Graph<N, E> getGraph() {
		return graph;
	}
	
	public void setGraph(Graph<N, E> graph) {
		this.graph = graph;
	}
	
	public void setNetworkAttribute(String name, String value) {
		networkAttributes.put(name, value);
	}
	
	public String getNetworkAttribute(String name) {
		return networkAttributes.get(name);
	}
	
	public Set<String> listNodeAttributes() {
		Set<String> a = new HashSet<String>();
		for(N n : graph.getNodes()) {
			Set<String> atts = getNodeAttributes(n).keySet();
			if(atts != null) a.addAll(atts);
		}
		return a;
	}
	
	public Set<String> listEdgeAttributes() {
		Set<String> a = new HashSet<String>();
		for(E e : graph.getEdges()) {
			Set<String> atts = getEdgeAttributes(e).keySet();
			if(atts != null) a.addAll(atts);
		}
		return a;
	}
	
	public boolean isNumericAttribute(String name) {
		boolean numeric = true;
		for(Map<String, String> amap : attributes.values()) {
			try {
				String d = amap.get(name);
				if(d != null) Double.parseDouble(d);
			} catch(NumberFormatException e) {
				numeric = false;
				break;
			}
		}
		return numeric;
	}
	
	public Map<String, String> getNetworkAttributes() { return networkAttributes; }
	
	public Map<String, String> getNodeAttributes(N node) {
		return getAttributes(node);
	}
	
	public void setNodeAttribute(N node, String name, String value) {
		getAttributes(node).put(name, value);
	}
	
	public Map<Object, Map<String, String>> getRawAttributes() {
		return attributes;
	}
	
	public String getNodeAttribute(N node, String name) {
		return getAttributes(node).get(name);
	}
	
	public Map<String, String> getEdgeAttributes(E edge) {
		return getAttributes(edge);
	}
	
	public void setEdgeAttribute(E edge, String name, String value) {
		getAttributes(edge).put(name, value);
	}
	
	public String getEdgeAttribute(E edge, String name) {
		return getAttributes(edge).get(name);
	}
	
	private Map<String, String> getAttributes(Object id) {
		Map<String, String> a = attributes.get(id);
		if(a == null) attributes.put(id, a = new HashMap<String, String>());
		return a;
	}
	
	public void readFromXGMML(Reader in, FromString<N> nodeFactory, FromString<E> edgeFactory, boolean readAttributes) throws SAXException, ParserConfigurationException, IOException {
		XGMMLContentHandler<N, E> handler = new XGMMLContentHandler<N, E>(this, nodeFactory, edgeFactory, readAttributes);
		XMLReader xr = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
		xr.setContentHandler(handler);
		xr.parse(new InputSource(in));
	}
	
	public void readFromXGMML(Reader in, FromString<N> nodeFactory, FromString<E> edgeFactory) throws SAXException, ParserConfigurationException, IOException {
		readFromXGMML(in, nodeFactory, edgeFactory, true);
	}
	
	public void writeToXGMML(Writer out) {
		PrintWriter pout = new PrintWriter(out);
		XGMMLWriter.write(this, pout);
		pout.flush();
	}
	
	public interface FromString<K> {
		public K fromString(String s);
	}
	
	public static final FromString<String> defaultFactory = new FromString<String>() {
		public String fromString(String s) { return s; }
	};
	
	public static final GraphFactory<String, String> defaultGraphFactory = new GraphFactory<String, String>() {
		public org.pct.model.Graph<String,String> createGraph() {
			return new JungGraph<String, String>(new UndirectedSparseGraph<String, String>());
		}
	};
	
	public static final FromString<Xref> xrefFactory = new FromString<Xref>() {
		public Xref fromString(String s) {
			//Assume pattern code:id
			String[] ss = s.split(":", 2);
			return new Xref(ss[1], DataSource.getBySystemCode(ss[0]));
		};
	};
	
	public void copyAttributes(Network<N, E> source) {
		for(N n : source.graph.getNodes()) {
			attributes.put(n, source.getNodeAttributes(n));
		}
		for(E e : source.graph.getEdges()) {
			attributes.put(e, source.getEdgeAttributes(e));
		}
		networkAttributes.putAll(source.networkAttributes);
	}
	
	public void subNetwork(Set<N> nodes, Network<N, E> target) {
		for(N n1 : nodes) {
			if(!target.graph.containsNode(n1)) target.graph.addNode(n1);
			for(N n2 : nodes) {
				if(!target.graph.containsNode(n2)) target.graph.addNode(n2);
				Collection<E> edges = graph.getEdges(n1, n2);
				if(edges != null) for(E e : edges) {
					if(!target.graph.containsEdge(e)) {
						target.graph.addEdge(e, n1, n2);
					}
				}
			}
		}
		target.copyAttributes(this);
	}
	
	/**
	 * Merge n into this network. Any conflicting attributes will be overwritten.
	 */
	public void merge(Network<N, E> n) {
		merge(n, true);
	}
	
	/**
	 * Merge n into this network. Any conflicting attributes will be overwritten.
	 */
	public void merge(Network<N, E> n, boolean multi) {
		//Merge the graph
		for(N node : n.graph.getNodes()) {
			graph.addNode(node);
		}
		Collection<E> edges = n.graph.getEdges();
		for(E edge : edges) {
			if(multi) { //Just add the edge if multi-graph mode
				graph.addEdge(edge, n.graph.getFirst(edge), n.graph.getSecond(edge));
			} else { //Check if there is a parallel edge if single-graph mode
				E ee = graph.getEdge(n.graph.getFirst(edge), n.graph.getSecond(edge));
				if(ee == null) { //Only add if no existing edge yet
					graph.addEdge(edge, n.graph.getFirst(edge), n.graph.getSecond(edge));
				}
			}
		}
		//Merge the attributes
		for(Object o : n.attributes.keySet()) {
			Map<String, String> a = attributes.get(o);
			if(a == null) attributes.put(o, a = new HashMap<String, String>());
			a.putAll(n.attributes.get(o));
		}
	}
}
