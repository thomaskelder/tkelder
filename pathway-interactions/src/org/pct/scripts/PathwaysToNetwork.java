package org.pct.scripts;

import java.io.File;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;

import org.bridgedb.Xref;
import org.pct.io.GmlWriter;
import org.pct.model.AttributeKey;
import org.pct.model.Network;
import org.pct.util.ArgsParser;
import org.pct.util.GpmlUtils;
import org.pct.util.ArgsData.DIDMapper;
import org.pct.util.ArgsData.DPathways;
import org.pct.util.ArgsParser.AIDMapper;
import org.pct.util.ArgsParser.APathways;

import uk.co.flamingpenguin.jewel.cli.Option;

public class PathwaysToNetwork {

	public static void main(String[] args) {
		try {
			Args pargs = ArgsParser.parse(args, Args.class);
			DIDMapper didm = new DIDMapper(pargs);
			DPathways dpws = new DPathways(pargs, didm);
			
			Network<Xref, String> network = GpmlUtils.pathwaysAsNetwork(dpws.getPathwayFiles(), didm.getIDMapper(), didm.getDataSources());
			
			//Add symbols
			Map<Xref, String> sym = GpmlUtils.readSymbols(dpws.getPathwayFiles(), didm.getIDMapper(), didm.getDataSources());
			for(Entry<Xref, String> e : sym.entrySet()) network.setNodeAttribute(e.getKey(), AttributeKey.Label.name(), e.getValue());
			
			PrintWriter out = new PrintWriter(pargs.getOut());
			if(pargs.getOut().getName().endsWith(".gml")) {
				GmlWriter.writeGml(out, network);
			} else {
				network.writeToXGMML(out);
			}
			out.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	interface Args extends APathways, AIDMapper {
		@Option(shortName = "o", description = "Output file")
		File getOut();
	}
}
