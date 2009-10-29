package org.apa.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.logging.Logger;

import org.apa.AtlasSessionUtils;
import org.apa.analysis.AnalysisUtils;
import org.apa.data.ExperimentAnalysis;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class ExportStatistics {
	static final Logger log = Logger.getLogger("org.apa.tools");
	
	@Option(name = "-sessionConfig", required = true, usage = "Session configuration xml file.")
	File sessionConfig;

	@Option(name = "-out", required = true, usage = "The directory to save the result files.")
	File outPath;
	
	public static void main(String[] args) {
		ExportStatistics main = new ExportStatistics();
		CmdLineParser parser = new CmdLineParser(main);
		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			e.printStackTrace();
			parser.printUsage(System.err);
			System.exit(-1);
		}

		SessionFactory sessionFactory = null;
		
		try {
			log.info("Creating hibernate session");
			sessionFactory = AtlasSessionUtils.createSessionFactory(main.sessionConfig);
			sessionFactory.getCurrentSession().beginTransaction();
			Session session = sessionFactory.getCurrentSession();
			
			main.outPath.mkdirs();
			
			log.info("Querying analyses");
			Collection<ExperimentAnalysis> analyses = AtlasSessionUtils.getAnalyses(session);
			for(ExperimentAnalysis ea : analyses) {
				log.fine("Exporting " + ea);
				String f = ea.getFactor().getValue().replaceAll("/", "_");
				File outFile = new File(
						main.outPath, ea.getType() + "_" + ea.getExperiment() + "_" + f + ".txt"
				);
				
				AnalysisUtils.asText(AnalysisUtils.getFormat(ea), ea, new BufferedWriter(new FileWriter(outFile)));
			}
		} catch(Exception e) {
			log.warning("Error performing analysis: " + e.getMessage());
			e.printStackTrace();
			System.exit(-2);
		} finally {
			log.info("Closing hibernate session");
			if(sessionFactory != null) sessionFactory.close();
		}
	}
}
