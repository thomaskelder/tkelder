package pps.pathwayexport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import org.bridgedb.DataDerby;
import org.bridgedb.DataSource;
import org.bridgedb.Gdb;
import org.bridgedb.SimpleGdbFactory;
import org.bridgedb.Xref;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pathvisio.debug.Logger;
import org.pathvisio.model.Pathway;
import org.pathvisio.util.FileUtils;

/**
 * Export a set of GPML files as gene lists with a fixed identifier type.
 * @author thomas
 */
public class TranslateXrefExporter {
	@Option(name = "-in", required = true, usage = "The path containing the gpmls to export.")
	private File inPath;
	
	@Option(name = "-out", required = true, usage = "The output path.")
	private File outPath;
	
	@Option(name = "-code", required = true, usage = "The code of the DataSource to translate to")
	private String sysCode;
	
	@Option(name = "-gdb", required = true, usage = "The synonym database to use.")
	private File gdbFile;
	
	@Option(name = "-addName", required = false, usage = "First line of output will be comment with pathway name")
	private boolean addName;
	
	public static void main(String[] args) {
		Logger.log.setLogLevel(true, true, true, true, true, true);

		TranslateXrefExporter main = new TranslateXrefExporter();
		CmdLineParser parser = new CmdLineParser(main);
		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			e.printStackTrace();
			parser.printUsage(System.err);
			System.exit(-1);
		}

		try {
			Gdb gdb = SimpleGdbFactory.createInstance("" + main.gdbFile, new DataDerby(), 0);
			DataSource ds = DataSource.getBySystemCode(main.sysCode);
			if(ds == null) {
				Logger.log.error("Couldn't find database system for code '" + main.sysCode + "'");
				System.exit(-1);
			}
			
			for(File f : FileUtils.getFiles(main.inPath, "gpml", true)) {
				Logger.log.trace("Collecting xrefs for " + f.getName());

				Pathway pathway = new Pathway();
				pathway.readFromXml(f, true);
				
				Set<Xref> translated = new HashSet<Xref>();
				
				for(Xref x : pathway.getDataNodeXrefs()) {
					translated.addAll(gdb.getCrossRefs(x, ds));
				}
				
				if(translated.size() == 0) {
					Logger.log.trace("Skipping " + f.getName() + ": no xrefs");
					continue;
				}
				
				File outFile = new File(main.outPath, f.getName().replaceAll(".gpml", ".txt"));
				Writer out = new BufferedWriter(new FileWriter(outFile));
				
				if(main.addName) {
					out.write("#");
					out.write(pathway.getMappInfo().getMapInfoName());
					out.write("\n");
				}
				for(Xref x : translated) {
					out.write(x.getId());
					out.write("\n");
				}
				out.flush();
				out.close();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
