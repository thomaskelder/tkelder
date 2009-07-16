package walkietalkie;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bridgedb.DataSource;
import org.bridgedb.rdb.DataDerby;
import org.bridgedb.rdb.IDMapperRdb;
import org.bridgedb.rdb.SimpleGdbFactory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pathvisio.debug.Logger;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.plugins.statistics.ZScoreCalculator;
import org.pathvisio.visualization.colorset.Criterion;
import org.pathvisio.visualization.colorset.Criterion.CriterionException;

import walkietalkie.WalkieTalkie.Parameters;
import walkietalkie.WalkieTalkie.PathwayInfo;

public class WalkieTalkieMain {

	@Option(name = "-mc", required = false, usage = "The minimum number of pathways a gene should connect" +
	" to in order to be included in the network.")
	private int minConnections = 1;

	@Option(name = "-data", required = false, usage = "The dataset to get the genes from.")
	private File gexFile;	

	@Option(name = "-gdb", required = true, usage = "The synonym database to use.")
	private File gdbFile;

	@Option(name = "-p", required = true, usage = "The directory containing the pathways.")
	private File pathwayDir;

	@Option(name = "-s", required = true, usage = "The file to write the sif network to.")
	private File sif;

	@Option(name = "-crit", required = false, usage = "The criterion to find significant genes in the dataset.")
	private String criterion;
	
	@Option(name = "-attrType", required = false, usage = "The file to write type attributes to.")
	private File attrTypeFile;
	
	@Option(name = "-attrLabel", required = false, usage = "The file to write label attributes to.")
	private File attrLabelFile;
	
	@Option(name = "-filterPathways", required = false, usage = "Only show these pathways (specify file names, without path).")
	private List<String> filterPathways = new ArrayList<String>();
	
	@Option(name = "-firstNeighbours", required = false, usage = "Use in combination with -filterPathways, to also include the first neighbours of these pathways.")
	boolean firstNeighbours;
	
	@Option(name = "-filterZscore", required = false, usage = "Only include pathways with a z-score >= the given value.")
	private double filterZscore = Double.MIN_VALUE;
	
	@Option(name = "-dataSource", required = false, usage = "Translate everything to the specified datasource.")
	private String dataSource;
	
	private WalkieTalkieMain() {
	}

	public static void main(String[] args) throws CriterionException {
		Logger.log.setLogLevel(true, true, true, true, true, true);

		WalkieTalkieMain main = new WalkieTalkieMain();
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
			if(main.gexFile != null) {
				gex = new SimpleGex("" + main.gexFile, false, new DataDerby());
			}
			IDMapperRdb gdb =SimpleGdbFactory.createInstance("" + main.gdbFile, new DataDerby(), 0);
			
			Criterion crit = null;
			if(main.criterion != null) {
				crit = new Criterion();
				crit.setExpression(main.criterion, gex.getSampleNames());
			}

			//Read the pathway information
			Set<PathwayInfo> pathways = null;
			if(main.filterZscore > Double.MIN_VALUE) { //Filter by z-score
				ZScoreCalculator zsc = new ZScoreCalculator(
						crit, main.pathwayDir, gex, gdb, null
				);
				pathways = WalkieTalkieUtils.pathwaysByZScore(
						main.pathwayDir, zsc.calculateAlternative(), main.filterZscore
				);
			} else { //Read all pathways
				pathways = WalkieTalkieUtils.readPathways(main.pathwayDir);
			}
			
			Writer out = new BufferedWriter(new FileWriter(main.sif));
			
			Parameters par = Parameters.create()
			.minGeneConnections(main.minConnections)
			.firstNeighbours(main.firstNeighbours);
			
			if(main.dataSource != null) {
				DataSource ds = DataSource.getBySystemCode(main.dataSource);
				if(ds == null) ds = DataSource.getByFullName(main.dataSource);
				par = par.dataSource(ds);
			}
			
			WalkieTalkie wt = new WalkieTalkie(
					par, 
					crit, 
					pathways, 
					gdb, 
					gex
			);
			Set<PathwayInfo> filterPathways = new HashSet<PathwayInfo>();
			for(String pwFile : main.filterPathways) {
				for(PathwayInfo pwi : pathways) {
					if(pwi.getFile().getName().equals(pwFile)) {
						filterPathways.add(pwi);
						Logger.log.trace("Filtering by pathway " + pwFile + ", with neighbours.");
					}
				}
			}
			wt.writeSif(out, filterPathways);
			out.flush();
			out.close();
			
			if(main.attrLabelFile != null) {
				Writer attrOut = new BufferedWriter(new FileWriter(main.attrLabelFile));
				wt.writeLabelAttributes(attrOut);
				attrOut.flush();
				attrOut.close();
			}
			if(main.attrTypeFile != null) {
				Writer attrOut = new BufferedWriter(new FileWriter(main.attrTypeFile));
				wt.writeTypeAttributes(attrOut);
				attrOut.flush();
				attrOut.close();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
