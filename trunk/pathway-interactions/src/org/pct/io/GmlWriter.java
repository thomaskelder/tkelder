package org.pct.io;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.freehep.util.io.IndentPrintWriter;
import org.pathvisio.util.FileUtils;
import org.pct.model.JungGraph;
import org.pct.model.Network;
import org.pct.util.ArgsParser;

import uk.co.flamingpenguin.jewel.cli.Option;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class GmlWriter {
	private final static Logger log = Logger.getLogger(GmlWriter.class.getName());
	
	public static <N, E> void writeGml(PrintWriter out, Network<N, E> network) {
		IndentPrintWriter iout = new IndentPrintWriter(out);
		iout.setIndentString("\t");
		int indent = 0;
		
		iout.println("graph [");
		iout.setIndent(++indent);
		iout.println("directed\t" + (network.getGraph().isDirected() ? 1 : 0));
		//Print nodes and attributes
		for(N n : network.getGraph().getNodes()) {
			iout.println("node [");
			iout.setIndent(++indent);
			iout.println("id\t" + n.hashCode());
			iout.println("identifier\t" + '"' + n + '"');
			printAttributes(iout, network.getNodeAttributes(n));
			iout.setIndent(--indent);
			iout.println("]");
		}
		//Print edges and attributes
		for(E e : network.getGraph().getEdges()) {
			iout.println("edge [");
			iout.setIndent(++indent);
			String srcS = network.getGraph().getFirst(e).toString();
			String tgtS = network.getGraph().getSecond(e).toString();
			int src = srcS.hashCode();
			int tgt = tgtS.hashCode();
			iout.println("source\t"  + src);
			iout.println("target\t"  + tgt);
			iout.println("id\t" + '"' + (src > tgt ? tgt + "," + src : src + "," + tgt) + '"');
			iout.println("identifier\t" + '"' + (src > tgt ? tgtS + "," + srcS : srcS + "," + tgtS) + '"');
			printAttributes(iout, network.getEdgeAttributes(e));
			iout.setIndent(--indent);
			iout.println("]");
		}
		//Print network attributes
		printAttributes(iout, network.getNetworkAttributes());
		iout.setIndent(--indent);
		iout.println("]");
	}
	
	private static void printAttributes(PrintWriter out, Map<String, String> attributes) {
		for(String k : attributes.keySet()) {
			//Find out if this is a number
			String v = attributes.get(k);
			if(v == null || "".equals(v)) continue; //Skip empty attributes
			boolean isNumber = true;
			try { 
				Double.parseDouble(v);
			} catch(NumberFormatException e) {
				isNumber = false;
			}
			if(!isNumber) v = '"' + v + '"';
			out.println(k + "\t" + v);
		}
	}
	public static void main(String[] args) {
		try {
			Args pargs = ArgsParser.parse(args, Args.class);
			
			Set<File> xgmmlFiles = new HashSet<File>();
			for(File f : pargs.getIn()) {
				xgmmlFiles.addAll(FileUtils.getFiles(f, "xgmml", true));
			}
			for(File f : xgmmlFiles) {
				String fname = f.getAbsolutePath();
				if(fname.endsWith("xgmml")) {
					fname = fname.substring(0, fname.length() - 5) + "gml";
				}
				File fout = new File(fname);
				log.info("Converting " + f + " to " + fout);
				Network<String, String> network = new Network<String, String>(
					new JungGraph<String, String>(
							pargs.isDirected() ? new DirectedSparseGraph<String, String>() : new UndirectedSparseGraph<String, String>()
					)
				);
				
				network.readFromXGMML(new FileReader(f), Network.defaultFactory, Network.defaultFactory);
				
				PrintWriter out = new PrintWriter(fout);
				writeGml(out, network);
				out.close();
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private static interface Args {
		@Option(description = "The xgmml file(s) of the network(s) to convert to GML. " +
				"If this is a directory, the script will recursively convert all .xgmml files in the directory.")
		List<File> getIn();
		@Option(description = "Should the network considered directed or not?")
		boolean isDirected();
	}
}
