package pps.pathwayexport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

import org.bridgedb.IDMapperException;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.DataDerby;
import org.bridgedb.rdb.IDMapperRdb;
import org.bridgedb.rdb.SimpleGdbFactory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pathvisio.debug.Logger;
import org.pathvisio.gex.ReporterData;
import org.pathvisio.gex.Sample;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.plugins.statistics.StatisticsPathwayResult;
import org.pathvisio.plugins.statistics.StatisticsResult;
import org.pathvisio.plugins.statistics.ZScoreCalculator;
import org.pathvisio.preferences.PreferenceManager;
import org.pathvisio.visualization.colorset.Criterion;

/**
 * Export a gex dataset as cytoscape attribute files.
 * @author thomas
 */
public class AttributeExporter {
	@Option(name = "-data", required = true, usage = "The data file (pgex) to export.")
	private File dataFile;

	@Option(name = "-out", required = true, usage = "The directory to write the attribute files to.")
	private File outFile;
	
	@Option(name = "-prefix", required = true, usage = "A prefix to the attribute file names.")
	private String prefix;
	
	@Option(name = "-zscore", required = false, usage = "Export z-scores instead of data.")
	private boolean zscore;
	
	@Option(name = "-crit", required = false, usage = "Use with -zscore: The criterion for the z-score calculation")
	private String criterion;
	
	@Option(name = "-gdb", required = true, usage = "Use with -zscore: The synonym database to use.")
	private File gdbFile;

	@Option(name = "-p", required = true, usage = "Use with -zscore: The directory containing the pathways.")
	private File pathwayDir;
	
	public static void main(String[] args) {
		PreferenceManager.init();
		BioDataSource.init();
		Logger.log.setLogLevel(true, true, true, true, true, true);

		AttributeExporter main = new AttributeExporter();
		CmdLineParser parser = new CmdLineParser(main);
		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			e.printStackTrace();
			parser.printUsage(System.err);
			System.exit(-1);
		}

		try {
			SimpleGex gex = null;
			gex = new SimpleGex("" + main.dataFile, false, new DataDerby());
			if(!main.zscore) {
				Logger.log.info("Exporting data...");
				export(gex, main.outFile, main.prefix);
			} else {
				Logger.log.info("Exporting z-scores...");
				IDMapperRdb gdb = SimpleGdbFactory.createInstance("" + main.gdbFile, new DataDerby(), 0);
				Criterion crit = new Criterion();
				crit.setExpression(main.criterion, gex.getSampleNames());
				exportZscores(gex, crit, main.pathwayDir, gdb, main.outFile, main.prefix);
			}
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(-2);
		}
	}
	
	public static void export(SimpleGex data, File outPath, String prefix) throws IOException, IDMapperException {
		outPath.mkdirs();
		
		//List the data columns
		Collection<Sample> samples = data.getSamples().values();
		for(Sample s : samples) {
			File outFile = new File(outPath, prefix + s.getName());
			Writer out = new BufferedWriter(new FileWriter(outFile));
			
			//Write header
			out.append(s.getName());
			out.append("\n");
			//Write values
			for(int i = 0; i < data.getNrRow(); i++) {
				ReporterData rdata = data.getRow(i);
				Object value = rdata.getSampleData(s);
				out.append(rdata.getXref().getId());
				out.append(" = ");
				out.append(value.toString());
				out.write("\n");
			}
			out.close();
		}
	}
	
	public static void exportZscores(SimpleGex data, Criterion crit, File pathwayDir, IDMapperRdb gdb, File outPath, String prefix) throws IDMapperException, IOException {
		outPath.mkdirs();
		File outFile = new File(outPath, prefix);
		Writer out = new BufferedWriter(new FileWriter(outFile));

		String label = crit.getExpression();
		label.replaceAll("<", ".");
		out.append(label);
		out.append("\n");
		
		ZScoreCalculator zsc = new ZScoreCalculator(
				crit, pathwayDir, data, gdb, null
		);
		StatisticsResult result = zsc.calculateAlternative();
		for(StatisticsPathwayResult pwr : result.getPathwayResults()) {
			out.append(pwr.getFile().getName());
			out.append(" = ");
			out.append(pwr.getZScore() + "");
			out.append("\n");
		}
		out.close();
	}
}