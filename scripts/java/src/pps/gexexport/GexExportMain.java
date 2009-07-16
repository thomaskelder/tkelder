package pps.gexexport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.List;

import org.bridgedb.DataSource;
import org.bridgedb.rdb.DataDerby;
import org.bridgedb.rdb.SimpleGdb;
import org.bridgedb.rdb.SimpleGdbFactory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pathvisio.debug.Logger;
import org.pathvisio.gex.SimpleGex;

/**
 * Export a gex file as a data matrix where the reporter ids
 * are converted to an identifier system of choice.
 * @author thomas
 */
public class GexExportMain {
	@Option(name = "-gdb", required = true, usage = "The synonym database to use.")
	private File gdbFile;
	
	@Option(name = "-gex", required = true, usage = "The dataset to export.")
	private File gexFile;
	
	@Option(name = "-out", required = true, usage = "The output file.")
	private File outFile;
	
	@Option(name = "-sys", required = true, usage = "The system code of the database system to export to.")
	private String sysCode;
	
	@Option(name = "-cols", required = false, usage = "The data columns to export.")
	private List<String> sampleNames;
	
	private GexExportMain() { }
	
	public static void main(String[] args) {
		Logger.log.setLogLevel(true, true, true, true, true, true);

		GexExportMain main = new GexExportMain();
		CmdLineParser parser = new CmdLineParser(main);
		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			e.printStackTrace();
			parser.printUsage(System.err);
			System.exit(-1);
		}

		try {
			SimpleGex gex = new SimpleGex("" + main.gexFile, false, new DataDerby());
			SimpleGdb gdb = SimpleGdbFactory.createInstance("" + main.gdbFile, new DataDerby(), 0);
			DataSource ds = DataSource.getBySystemCode(main.sysCode);
			if(ds == null) {
				Logger.log.error("Couldn't find database system for code '" + main.sysCode + "'");
				System.exit(-1);
			}
			
			Writer out = new BufferedWriter(new FileWriter(main.outFile));
			
			GexExport.exportDelimited(gex, out, "\t", gdb, ds, main.sampleNames);
			
			out.flush();
			out.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
