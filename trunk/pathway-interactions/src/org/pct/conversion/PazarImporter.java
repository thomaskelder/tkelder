package org.pct.conversion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;

import org.bridgedb.DataSource;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.pathvisio.util.FileUtils;
import org.pct.io.GmlWriter;
import org.pct.model.AttributeKey;
import org.pct.model.JungGraph;
import org.pct.model.Network;
import org.pct.util.ArgsParser;
import org.pct.util.ArgsParser.AHelp;

import uk.co.flamingpenguin.jewel.cli.Option;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

/**
 * Import tabe delimited text files generated from Pazar XML with the script:
 * http://code.google.com/p/tkelder/source/browse/#svn/trunk/scripts/groovy/pazar_parser
 * 
 */
public class PazarImporter {
	private final static Logger log = Logger.getLogger(PazarImporter.class.getName());

	public static void main(String[] args) {
		try {
			Args pargs = ArgsParser.parse(args, Args.class);
			BioDataSource.init();

			DataSource ds = BioDataSource.ENSEMBL_MOUSE;

			Network<Xref, String> network = new Network<Xref, String>(
					new JungGraph<Xref, String>(new DirectedSparseGraph<Xref, String>())
			);

			for(File dir : pargs.getIn()) {
				for(File f : FileUtils.getFiles(dir, "txt", false)) {
					log.info("Reading file " + f);
					BufferedReader in = new BufferedReader(new FileReader(f));
					//Read header
					String line = in.readLine();
					//Make sure the file has the correct format
					String[] headers = line.split("\t");
					if(headers.length != 8) {
						log.info("Skipping file, incorrect number of columns: " + headers.length);
						 continue;
					}
					while((line = in.readLine()) != null) {
						String[] cols = line.split("\t", 8);
						String tfId = cols[1];
						String tgtId = cols[2];
						String type = cols[7];
						
						Xref tf = new Xref(tfId, ds);
						Xref tgt = new Xref(tgtId, ds);
						network.getGraph().addNode(tf);
						network.getGraph().addNode(tgt);
						
						String edge = tf + "(" + type + ")" + tgt + "(" + f.getName() + ")";
						network.getGraph().addEdge(edge, tf, tgt);
						
						network.setEdgeAttribute(edge, AttributeKey.Source.name(), f.getName());
						network.setEdgeAttribute(edge, AttributeKey.InteractionValue.name(), type);
						network.setEdgeAttribute(edge, AttributeKey.Interaction.name(), "TF");
						network.setNodeAttribute(tf, AttributeKey.Label.name(), cols[3]);
						network.setNodeAttribute(tgt, AttributeKey.Label.name(), cols[4]);
					}
					in.close();
				}
			}
			
			log.info("Writing network to " + pargs.getOut());
			PrintWriter out = new PrintWriter(pargs.getOut());
			if(pargs.getOut().getName().endsWith("gml")) {
				GmlWriter.writeGml(out, network);
			} else {
				network.writeToXGMML(out);
			}
			out.close();
		} catch(Exception e) {
			e.printStackTrace();
		}

	}

	private interface Args extends AHelp {
		@Option(shortName = "i", description = "The path(s) to the pazar .txt files.")
		List<File> getIn();
		@Option(shortName = "o", description = "The file to write the imported network to")
		File getOut();
	}
}
