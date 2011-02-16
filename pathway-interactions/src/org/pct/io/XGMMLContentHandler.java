package org.pct.io;

import java.util.logging.Logger;

import org.pct.model.Network;
import org.pct.model.Network.FromString;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XGMMLContentHandler<N, E> extends DefaultHandler {
	private final static Logger log = Logger.getLogger(XGMMLContentHandler.class.getName());
	
	private boolean readAttributes;
	
	private String currentName = null;
	private String currentId = null;
	
	private FromString<N> nodeFactory;
	private FromString<E> edgeFactory;
	private Network<N, E> network;
	
	public XGMMLContentHandler(Network<N, E> network, FromString<N> nodeFactory, FromString<E> edgeFactory, boolean readAttributes) {
		this.network = network;
		this.nodeFactory = nodeFactory;
		this.edgeFactory = edgeFactory;
		this.readAttributes = readAttributes;
	}
	
	@Override
	public void startElement(String uri, String localName, String qName,
		Attributes attributes) throws SAXException {
		String id = attributes.getValue("id");
		if("node".equals(qName)) {
			log.finest("Processing " + currentName + ": " + currentId);
			currentName = qName;
			currentId = id;
			network.getGraph().addNode(nodeFactory.fromString(id));
		}
		else if("edge".equals(qName)) {
			log.finest("Processing " + currentName + ": " + currentId);
			currentName = qName;
			currentId = id;
			N a = nodeFactory.fromString(attributes.getValue("source"));
			N b = nodeFactory.fromString(attributes.getValue("target"));
			network.getGraph().addEdge(edgeFactory.fromString(id), a, b);
		}
		else if(readAttributes && "att".equals(qName)) {
			if(currentName != null) {
				log.finest("Processing attribute for " + currentName + ": " + currentId);
				log.finest("Name: " + attributes.getValue("name"));
				log.finest("Value: " + attributes.getValue("value"));
				
				if("node".equals(currentName)) {
					network.setNodeAttribute(
						nodeFactory.fromString(currentId),
						attributes.getValue("name"),
						attributes.getValue("value")
					);
				}
				else if("edge".equals(currentName)) {
					network.setEdgeAttribute(
							edgeFactory.fromString(currentId),
							attributes.getValue("name"),
							attributes.getValue("value")
					);
				}
				else if("graph".equals(currentName)) {
					network.setNetworkAttribute(attributes.getValue("name"), attributes.getValue("value"));
				}
			}
		}
		else if("graph".equals(qName)) {
			String name = attributes.getValue("name");
			if(name == null) name = attributes.getValue("id");
			if(name != null) network.setTitle(name);
		} else {
			currentName = null;
			currentId = null;
		}
	}
}
