package pps.walkietalkie;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pathvisio.data.DataDerby;
import org.pathvisio.data.Gdb;
import org.pathvisio.data.SimpleGdbFactory;
import org.pathvisio.debug.Logger;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.plugins.statistics.PathwayMap;
import org.pathvisio.plugins.statistics.StatisticsPathwayResult;
import org.pathvisio.plugins.statistics.StatisticsResult;
import org.pathvisio.plugins.statistics.ZScoreCalculator;
import org.pathvisio.plugins.statistics.PathwayMap.PathwayInfo;
import org.pathvisio.util.swing.SearchTableModel.Column;
import org.pathvisio.visualization.colorset.Criterion;
import org.pathvisio.visualization.colorset.Criterion.CriterionException;

import pps.walkietalkie.WalkieTalkie.Parameters;

public class WalkieTalkieMain {

	@Option(name = "-mc", required = false, usage = "The minimum number of pathways a gene should connect" +
	" to in order to be included in the network.")
	private int minConnections = 2;

	@Option(name = "-gex", required = true, usage = "The dataset to get the genes from.")
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
	
	@Option(name = "-filterPathways", required = false, usage = "Only show these pathways (specify file names, without path) and their neighbours.")
	private List<String> filterPathways = new ArrayList<String>();
	
	@Option(name = "-filterZscore", required = false, usage = "Only include pathways with a z-score >= the given value.")
	private double filterZscore = Double.MIN_VALUE;
	
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
			SimpleGex gex = new SimpleGex("" + main.gexFile, false, new DataDerby());
			Gdb gdb = SimpleGdbFactory.createInstance("" + main.gdbFile, new DataDerby(), 0);
			
			List<PathwayInfo> pathways = new PathwayMap(main.pathwayDir).getPathways();
			Writer out = new BufferedWriter(new FileWriter(main.sif));
			
			Criterion crit = null;
			if(main.criterion != null) {
				crit = new Criterion();
				crit.setExpression(main.criterion, gex.getSampleNames());
			}
			
			if(main.filterZscore > Double.MIN_VALUE) {
				ZScoreCalculator zsc = new ZScoreCalculator(
						crit, main.pathwayDir, gex, gdb, null
				);
				StatisticsResult result = zsc.calculateAlternative();
				for(StatisticsPathwayResult pwr : result.getPathwayResults()) {
					if(pwr.getZScore() < main.filterZscore) {
						Set<PathwayInfo> remove = new HashSet<PathwayInfo>();
						for(PathwayInfo pwi : pathways) {
							if(pwi.getFile().equals(pwr.getFile())) {
								Logger.log.trace("Removing " + pwi.getName() + ", zscore < " + main.filterZscore);
								remove.add(pwi);
							}
						}
						pathways.removeAll(remove);
					}
				}
			}
			
			WalkieTalkie wt = new WalkieTalkie(
					Parameters.create()
					.minGeneConnections(main.minConnections), 
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