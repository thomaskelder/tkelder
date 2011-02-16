package org.pct.util;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pathvisio.data.XrefWithSymbol;
import org.pathvisio.model.ConverterException;
import org.pathvisio.model.ObjectType;
import org.pathvisio.model.Pathway;
import org.pathvisio.model.PathwayElement;
import org.pathvisio.model.PathwayElement.MPoint;
import org.pathvisio.util.PathwayParser;
import org.pathvisio.util.Relation;
import org.pathvisio.util.PathwayParser.ParseException;
import org.pct.model.AttributeKey;
import org.pct.model.JungGraph;
import org.pct.model.Network;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class GpmlUtils {
	public static Map<Xref, String> readSymbols(Collection<File> gpmlFiles, IDMapper idMapper, DataSource... targetDs) throws SAXException, ParseException, IDMapperException {
		XMLReader xmlReader = XMLReaderFactory.createXMLReader();

		Set<DataSource> tgts = new HashSet<DataSource>();
		for(DataSource ds : targetDs) tgts.add(ds);

		Map<Xref, String> symbols = new HashMap<Xref, String>();
		for(File f : gpmlFiles) {
			PathwayParser pp = new PathwayParser(f, xmlReader);
			for(XrefWithSymbol xs : pp.getGenes()) {
				Xref x = xs.asXref();
				if(tgts.contains(x.getDataSource())) {
					symbols.put(x, getPreferredSymbol(xs.getSymbol(), symbols.get(x)));
				} else {
					if(idMapper != null) {
						for(Xref xx : idMapper.mapID(x, targetDs)) {
							symbols.put(xx, getPreferredSymbol(xs.getSymbol(), symbols.get(xx)));
						}
					} else throw new IllegalArgumentException(
							"Found xref (" + x + ") that needs to be mapped to target datasources " + tgts + ", but no idMapper specified!"
					);
				}
			}
		}
		return symbols;
	}

	private static String getPreferredSymbol(String newS, String currS) {
		if(currS == null) return newS;
		else return currS.length() > newS.length() ? newS : currS;
	}

	public static Network<Xref, String> pathwaysAsNetwork(Collection<File> gpmlFiles, IDMapper idm, DataSource... targetDs) throws ConverterException, IDMapperException {
		Network<Xref, String> network = new Network<Xref, String>(new JungGraph<Xref, String>(new DirectedSparseMultigraph<Xref, String>()));
		for(File f : gpmlFiles) {
			Pathway p = new Pathway();
			p.readFromXml(f, true);

			for(PathwayElement pe : p.getDataObjects()) {
				if(isRelation(pe)) {
					Relation r = new Relation(pe);
					Set<Xref> xis = new HashSet<Xref>();
					Set<Xref> xos = new HashSet<Xref>();
					Set<Xref> xms = new HashSet<Xref>();
					for(PathwayElement i : r.getLefts()) if(i.getXref() != null) xis.addAll(idm.mapID(i.getXref(), targetDs));
					for(PathwayElement o : r.getRights()) if(o.getXref() != null) xos.addAll(idm.mapID(o.getXref(), targetDs));
					for(PathwayElement m : r.getMediators()) if(m.getXref() != null) xms.addAll(idm.mapID(m.getXref(), targetDs));

					if(xms.size() > 0) { //Add mediator -> input / output interactions
						for(Xref xm : xms) {
							network.getGraph().addNode(xm);
							network.setNodeAttribute(xm, "mediator", "true");
							Set<Xref> xios = new HashSet<Xref>(xis);
							xios.addAll(xos);
							for(Xref xio : xis) {
								network.getGraph().addNode(xio);
								String e = xm + "_m_" + xio;
								network.getGraph().addEdge(e, xm, xio);
								network.setEdgeAttribute(e, AttributeKey.Interaction.name(), "mediates_reaction");
								network.setEdgeAttribute(e, AttributeKey.Source.name(), "gpml");
								network.setEdgeAttribute(e, AttributeKey.PathwayId.name(), f.getName());
							}
						}
					}
					//Add input -> output interactions
					for(Xref xi : xis) {
						network.getGraph().addNode(xi);
						for(Xref xo : xos) {
							network.getGraph().addNode(xo);
							String e = xi + "_r_" + xo;
							network.getGraph().addEdge(e, xi, xo);
							network.setEdgeAttribute(e, AttributeKey.Interaction.name(), "reaction_interaction");
							network.setEdgeAttribute(e, AttributeKey.Source.name(), "gpml");
							network.setEdgeAttribute(e, AttributeKey.PathwayId.name(), f.getName());
						}
					}
				}
			}
		}
		return network;
	}

	private static boolean isRelation(PathwayElement pe) {
		if(pe.getObjectType() == ObjectType.LINE) {
			MPoint s = pe.getMStart();
			MPoint e = pe.getMEnd();
			if(s.isLinked() && e.isLinked()) {
				//Objects behind graphrefs should be PathwayElement
				//so not MAnchor
				if(pe.getParent().getElementById(s.getGraphRef()) != null &&
						pe.getParent().getElementById(e.getGraphRef()) != null)
				{
					return true;
				}
			}
		}
		return false;
	}

	public static Map<String, String> readPathwayTitles(Collection<File> gpmlFiles, boolean fullPath) throws SAXException, ParseException {
		XMLReader xmlReader = XMLReaderFactory.createXMLReader();

		Map<String, String> titles = new HashMap<String, String>();

		for(File f : gpmlFiles) {
			PathwayParser pp = new PathwayParser(f, xmlReader);
			titles.put(fullPath ? f.toString() : f.getName(), pp.getName());
		}
		return titles;
	}

	public static Map<String, Set<Xref>> readPathwaySets(Collection<File> gpmlFiles, boolean fullPath, IDMapper idMapper, DataSource... targetDs) throws SAXException, ParseException, IDMapperException {
		XMLReader xmlReader = XMLReaderFactory.createXMLReader();

		Set<DataSource> tgts = new HashSet<DataSource>();
		for(DataSource ds : targetDs) tgts.add(ds);

		Map<String, Set<Xref>> sets = new HashMap<String, Set<Xref>>();
		for(File f : gpmlFiles) {
			PathwayParser pp = new PathwayParser(f, xmlReader);
			Set<Xref> xrefs = new HashSet<Xref>();
			for(XrefWithSymbol xs : pp.getGenes()) {
				Xref x = xs.asXref();
				if(tgts.contains(x.getDataSource())) {
					xrefs.add(x);
				} else {
					if(idMapper != null) xrefs.addAll(idMapper.mapID(x, targetDs));
					else throw new IllegalArgumentException(
							"Found xref (" + x + ") that needs to be mapped to target datasources " + tgts + ", but no idMapper specified!"
					);
				}
			}
			sets.put(fullPath ? f.toString() : f.getName(), xrefs);
		}
		return sets;
	}
}
