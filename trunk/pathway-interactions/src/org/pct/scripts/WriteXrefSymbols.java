package org.pct.scripts;

import java.io.File;
import java.io.PrintWriter;
import java.util.Map;

import org.bridgedb.Xref;
import org.pct.util.ArgsParser;
import org.pct.util.GpmlUtils;
import org.pct.util.ArgsData.DIDMapper;
import org.pct.util.ArgsData.DPathways;
import org.pct.util.ArgsParser.AIDMapper;
import org.pct.util.ArgsParser.APathways;

import uk.co.flamingpenguin.jewel.cli.Option;

public class WriteXrefSymbols {
	public static void main(String[] args) {
		try {
			Args pargs = ArgsParser.parse(args, Args.class);
			DIDMapper didm = new DIDMapper(pargs);
			DPathways dpws = new DPathways(pargs, didm);
			
			Map<Xref, String> sym = GpmlUtils.readSymbols(dpws.getPathwayFiles(), didm.getIDMapper(), didm.getDataSources());
			
			PrintWriter out = new PrintWriter(pargs.getOut());
			for(Xref x : sym.keySet()) {
				out.println(x + "\t" + sym.get(x));
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
