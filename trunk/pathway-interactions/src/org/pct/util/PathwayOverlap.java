package org.pct.util;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.bridgedb.Xref;
import org.pathvisio.model.ConverterException;
import org.pathvisio.model.MState;
import org.pathvisio.model.ObjectType;
import org.pathvisio.model.Pathway;
import org.pathvisio.model.PathwayElement;
import org.pathvisio.model.GraphLink.GraphIdContainer;
import org.pathvisio.model.GraphLink.GraphRefContainer;
import org.pathvisio.model.PathwayElement.MAnchor;
import org.pathvisio.model.PathwayElement.MPoint;
import org.pct.io.GmlWriter;
import org.pct.model.AttributeKey;
import org.pct.model.JungGraph;
import org.pct.model.Network;
import org.pct.util.ArgsData.DIDMapper;
import org.pct.util.ArgsData.DPathways;
import org.pct.util.ArgsParser.AHelp;
import org.pct.util.ArgsParser.AIDMapper;
import org.pct.util.ArgsParser.APathways;

import uk.co.flamingpenguin.jewel.cli.Option;
import edu.mit.broad.genome.utils.FileUtils;

/**
 * Calculate pathway overlap based on common xrefs and merge highly overlapping pathways.
 * @author thomas
 *
 */
public class PathwayOverlap {
	private final static Logger log = Logger.getLogger(PathwayOverlap.class.getName());

	public static final String MERGE_SEP = "|";
	
	public static Map<Set<String>, Set<Xref>> createMergedPathways(Map<String, Set<Xref>> pathways, double cutoff) {
		Map<Set<String>, Set<Xref>> merged = new HashMap<Set<String>, Set<Xref>>();

		for(String s : pathways.keySet()) {
			Set<String> ss = new HashSet<String>();
			ss.add(s);
			merged.put(ss, pathways.get(s));
		}
		
		//Merge cliques until no overlap above threshold is present anymore
		while(true) {
			List<Set<String>> names = new ArrayList<Set<String>>();
			List<Set<Xref>> xrefs = new ArrayList<Set<Xref>>();
			for(Set<String> s : merged.keySet()) {
				names.add(s);
				xrefs.add(merged.get(s));
			}
			double[][] overlap = calculateOverlap(xrefs);
			double max = findMax(overlap);
			log.info("Merge iteration, max overlap is now: " + max);
			if(max < cutoff) break;
			
			merged = merge2(merged, names, overlap, cutoff);
		}
		
		return merged;
	}
	
	private static double findMax(double[][] overlap) {
		double max = Double.MIN_VALUE;
		for(int x = 0; x < overlap.length; x++) {
			for(int y = x + 1; y < overlap.length; y++) {
				if(!Double.isNaN(overlap[x][y])) 
					max = Math.max(max, overlap[x][y]);
			}
		}
		return max;
	}
	
	private static Map<Set<String>, Set<Xref>> merge(Map<Set<String>, Set<Xref>> pathways, List<Set<String>> names, double[][] overlap, double cutoff) {
		Map<Set<String>, Set<Set<String>>> conn = new HashMap<Set<String>, Set<Set<String>>>();
		for(int x = 0; x < overlap.length; x++) {
			Set<String> nx = names.get(x);
			for(int y = x + 1; y < overlap.length; y++) {
				Set<String> ny = names.get(y);
				Set<Set<String>> xy = conn.get(nx);
				if(xy == null) conn.put(nx, xy = new HashSet<Set<String>>());
				Set<Set<String>> yx = conn.get(ny);
				if(yx == null) conn.put(ny, yx = new HashSet<Set<String>>());
				if(overlap[x][y] >= cutoff) {
					xy.add(ny);
					yx.add(nx);
				}
			}
		}
		
		//A maximal clique, sometimes called inclusion-maximal, is a clique that is not 
		//included in a larger clique. Finding a maximal clique is straightforward: 
		//Starting with a single vertex, grow the current clique one vertex at a time by 
		//iterating over the graph’s remaining vertices, adding a vertex if it is connected 
		//to each vertex in the current clique, and discarding it otherwise. 
		//This algorithm runs in O(m) time.
		//- Wikipedia
		Set<Set<Set<String>>> cliques = new HashSet<Set<Set<String>>>();
		for(Set<String> n : names) {
			Set<Set<String>> c = new HashSet<Set<String>>();
			c.add(n);
			for(Set<String> nn : names) {
				if(conn.get(nn).containsAll(c)) {
					c.add(nn);
				}
			}
			cliques.add(c);
		}
		
		Map<Set<String>, Set<Xref>> result = new HashMap<Set<String>, Set<Xref>>();
		for(Set<Set<String>> c : cliques) {
			Set<Xref> x = new HashSet<Xref>();
			Set<String> mergedNames = new HashSet<String>();
			for(Set<String> s : c) {
				x.addAll(pathways.get(s));
				mergedNames.addAll(s);
			}
			result.put(mergedNames, x);
		}
		return result;
	}
	
	private static Map<Set<String>, Set<Xref>> merge2(Map<Set<String>, Set<Xref>> pathways, List<Set<String>> names, double[][] overlap, double cutoff) {
		Map<Set<String>, Set<Xref>> result = new HashMap<Set<String>, Set<Xref>>();

		Set<Set<String>> isMerged = new HashSet<Set<String>>();
		for(int x = 0; x < overlap.length; x++) {
			Set<String> nx = names.get(x);
			
			if(isMerged.contains(nx)) continue; //Already merged
			
			double maxOverlap = -1;
			Set<String> maxPartner = null;
			
			for(int y = x + 1; y < overlap.length; y++) {
				Set<String> ny = names.get(y);
				if(isMerged.contains(ny)) continue;
				if(overlap[x][y] >= cutoff & overlap[x][y] >= maxOverlap) {
					maxPartner = ny;
				}
			}
			
			Set<String> mergedNames = new HashSet<String>(nx);
			Set<Xref> mergedXrefs = new HashSet<Xref>(pathways.get(nx));
			if(maxPartner != null) {
				mergedNames.addAll(maxPartner);
				mergedXrefs.addAll(pathways.get(maxPartner));
				isMerged.add(maxPartner);
			}
			result.put(mergedNames, mergedXrefs);
		}
		return result;
	}
	
	/**
	 * Build a set of GPML pathway diagrams from merged pathway sets.
	 * @param merged A collection of sets containing GPML files. Each set represents
	 * a set of pathways to be merged into a single pathway diagram.
	 * @throws ConverterException 
	 */
	public static List<Pathway> buildMergedPathways(Collection<Set<File>> merged) throws ConverterException {
		List<Pathway> pathways = new ArrayList<Pathway>();
		
		for(Set<File> files : merged) {
			Pathway pathway = new Pathway();
			
			double ty = 0;
			for(File f : files) {
				Pathway p = new Pathway();
				p.readFromXml(f, true);
				PathwayElement info = p.getMappInfo();
				
				PathwayElement title = PathwayElement.createPathwayElement(ObjectType.LABEL);
				title.setMHeight(20);
				title.setMWidth(200);
				title.setMLeft(30);
				title.setMTop(ty);
				title.setTextLabel(info.getMapInfoName() + " (" + f.getName() + ")");
				pathway.add(title);
				
				String name = pathway.getMappInfo().getMapInfoName();
				name = "untitled".equals(name) ? info.getMapInfoName() : name + MERGE_SEP + info.getMapInfoName();
				if(name.length() > 50) name = name.substring(0, 46) + "...";
				pathway.getMappInfo().setMapInfoName(name);
				
				for(PathwayElement pe : p.getDataObjects()) {
					if(pathway.getGraphIds().contains(pe.getGraphId())) replaceGraphId(pe, pathway);
					if(pe.getObjectType() == ObjectType.LINE) {
						for(MAnchor ma : pe.getMAnchors()) {
							if(pathway.getGraphIds().contains(ma.getGraphId())) replaceGraphId(ma, pathway);
						}
					}
				}
				Set<PathwayElement> copied = new HashSet<PathwayElement>();
				for(PathwayElement pe : p.getDataObjects()) {
					if(pe.getObjectType() == ObjectType.MAPPINFO) continue;
					PathwayElement pec = pe.copy();
					pathway.add(pec);
					copied.add(pec);
				}
				for(PathwayElement c : copied) {
					if(c.getObjectType() != ObjectType.LINE &&
							c.getObjectType() != ObjectType.GROUP) c.setMTop(c.getMTop() + ty);
				}
				
				ty += info.getMBoardHeight() + 50;
			}
			pathways.add(pathway);
		}
		return pathways;
	}
	
	private static void replaceGraphId(GraphIdContainer pe, Pathway pathway) {
		//Replace this graphId and all references
		pe.setGraphId(pathway.getUniqueGraphId());
		for(GraphRefContainer ref : pe.getReferences()) {
			if(ref instanceof MPoint) ((MPoint)ref).setGraphRef(pe.getGraphId());
			if(ref instanceof MState) ((MState)ref).setGraphRef(pe.getGraphId());
		}
	}
	
	public static void writeGML(PrintWriter out, double[][] pct, List<String> names, Map<String, Set<Xref>> pathways) {
		Network<String, String> network = new Network<String, String>(new JungGraph<String, String>());
		for(String s : names) {
			network.getGraph().addNode(s);
			network.setNodeAttribute(s, AttributeKey.Label.name(), s);
			network.setNodeAttribute(s, AttributeKey.NrXrefs.name(), "" + pathways.get(s).size());
			network.setNodeAttribute(s, "NrPathways", "" + s.split(MERGE_SEP).length);
		}
		for(int x = 0; x < names.size(); x++) {
			String nx = names.get(x);
			for(int y = x + 1; y < names.size(); y++) {
				String ny = names.get(y);
				String e = nx + ny;
				network.getGraph().addEdge(e, nx, ny);
				network.setEdgeAttribute(e, "pct", "" + pct[x][y]);
			}
		}
		GmlWriter.writeGml(out, network);
		out.close();
	}
	
	public static double[][] calculateOverlap(List<Set<Xref>> pathways) {
		double[][] overlap = new double[pathways.size()][pathways.size()];

		for(int x = 0; x < pathways.size(); x++) {
			Set<Xref> pa = pathways.get(x);
			for(int y = x; y < pathways.size(); y++) {
				Set<Xref> pb = pathways.get(y);
				Set<Xref> union = new HashSet<Xref>(pa);
				union.retainAll(pb);
				double o = (double)union.size() / (double)Math.min(pa.size(), pb.size());
				overlap[x][y] = o;
				overlap[y][x] = o;
			}
		}

		return overlap;
	}

	public static void main(String[] args) {
		try {
			Args pargs = ArgsParser.parse(args, Args.class);
			DIDMapper didm = new DIDMapper(pargs);
			DPathways dpws = new DPathways(pargs, didm);
			
			List<String> pathwayNames = new ArrayList<String>(dpws.getPathways().keySet());
			List<Set<Xref>> pathwayList = new ArrayList<Set<Xref>>();
			for(String pn : pathwayNames) pathwayList.add(dpws.getPathways().get(pn));
			
			Map<String, String> pathwayTitles = GpmlUtils.readPathwayTitles(dpws.getPathwayFiles(), false);
			
			//Write merged pathways info
			Map<Set<String>, Set<Xref>> merged = dpws.getPathwaysById();
			PrintWriter out = new PrintWriter(new File(pargs.getOut(), "mergereport"));
			
			//Write GML file containing resulting overlap between merged pathways
			List<String> gmlNames = new ArrayList<String>();
			List<Set<Xref>> gmlPathways = new ArrayList<Set<Xref>>();
			Map<String, Set<Xref>> gmlMap = new HashMap<String, Set<Xref>>();
			for(Set<String> nms : merged.keySet()) {
				gmlNames.add(StringUtils.join(nms, MERGE_SEP));
				gmlPathways.add(merged.get(nms));
				gmlMap.put(StringUtils.join(nms, MERGE_SEP), merged.get(nms));
			}
			double[][] pctOverlap = calculateOverlap(gmlPathways);
			PrintWriter nout = new PrintWriter(new File(pargs.getOut(), "mergednetwork.gml"));
			writeGML(nout, pctOverlap, gmlNames, gmlMap);
			nout.close();
			
			for(Set<String> c : merged.keySet()) {
				int xsize = merged.get(c).size();
				int csize = c.size();
				
				out.println(csize + "\t" + xsize + "\t" + c);
				
				//Write xref list to txt file
				String fn = "";
				String id = "";
				String title = "";
				for(String s : c) {
					fn += "".equals(fn) ? FileUtils.removeExtension(s) : "_" + FileUtils.removeExtension(s);
					id += "".equals(id) ? s : MERGE_SEP + s;
					title += "".equals(title) ? pathwayTitles.get(s) : "; " + pathwayTitles.get(s);
				}
				PrintWriter xout = new PrintWriter(new File(pargs.getOut(), fn + ".txt"));
				xout.println(id);
				xout.println(title);
				for(Xref x : merged.get(c)) xout.println(x);
				xout.close();
			}
			out.close();
			
			List<Set<File>> mergedFiles = new ArrayList<Set<File>>();
			for(Set<String> names : merged.keySet()) {
				Set<File> f = new HashSet<File>();
				mergedFiles.add(f);
				for(String n : names) {
					File pf = dpws.getFile(n);
					if(pf == null) {
						System.err.println(n);
					}
					f.add(pf);
				}
			}
					
//			//Save as GPML
//			List<Pathway> mergedPathways = buildMergedPathways(mergedFiles);
//			for(int i = 0; i < mergedFiles.size(); i++) {
//				String fn = "";
//				for(File f : mergedFiles.get(i)) fn += FileUtils.removeExtension(f.getName());
//				mergedPathways.get(i).writeToXml(
//					new File(pargs.getOut(), fn + ".gpml"), true	
//				);
//			}
		} catch(Exception e) {
			e.printStackTrace();
			log.log(Level.SEVERE, "Fatal error", e);
		}
	}
	
	private interface Args extends APathways, AIDMapper, AHelp {
		@Option(shortName = "o", description = "The path to write the merged pathways and report to.")
		File getOut();
	}
}
