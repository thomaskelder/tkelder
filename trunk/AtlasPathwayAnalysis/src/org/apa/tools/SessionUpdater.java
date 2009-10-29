package org.apa.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apa.AtlasSessionManager;
import org.apa.AtlasSessionUtils;
import org.apa.rest.AtlasRestCache;
import org.bridgedb.IDMapperStack;
import org.hibernate.SessionFactory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pathvisio.wikipathways.WikiPathwaysCache;

/**
 * Command-line utility to update an Atlas hibernate session
 * @author thomas
 */
public class SessionUpdater {
	private enum Action { ALL, PATHWAY, ATLAS }
	
	static final Logger log = Logger.getLogger("org.apa.tools");

	@Option(name = "-sessionConfig", required = true, usage = "Session configuration xml file.")
	File sessionConfig;

	@Option(name = "-wpCache", required = true, usage = "Cache path for WikiPathways cache.")
	File wpCachePath;

	@Option(name = "-atlasCache", required = true, usage = "Cache path for Atlas REST cache.")
	File atlasCachePath;

	@Option(name = "-idmConfig", required = true, usage = "A text file containing the connection strings to the id mappers (one per line).")
	File idmConfig;

	@Option(name = "-action", required = false, usage = "Which action to take (ALL, PATWHAY or ATLAS, default=ALL).")
	Action action = Action.ALL;
	
	@Option(name = "-ignoreExperiments", required = false, usage = "A text file containing experiments to ignore (one per line).")
	File ignoreExperimentsFile;
	
	public static void main(String[] args) {
		SessionUpdater main = new SessionUpdater();
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
			Class.forName("org.bridgedb.rdb.IDMapperRdb");
			Class.forName("org.bridgedb.file.IDMapperText");

			log.info("Connecting to identifier mappers");

			IDMapperStack idMapper = new IDMapperStack();
			BufferedReader in = new BufferedReader(new FileReader(main.idmConfig));
			String line = "";
			while((line = in.readLine()) != null) {
				log.fine("Connecting to " + line);
				idMapper.addIDMapper(line);
			}
			in.close();

			Set<String> ignoreExperiments = new HashSet<String>();
			if(main.ignoreExperimentsFile != null) {
				in = new BufferedReader(new FileReader(main.ignoreExperimentsFile));
				while((line = in.readLine()) != null) {
					log.fine("Ignore experiment " + line);
					ignoreExperiments.add(line);
				}
			}
			in.close();
			
			AtlasRestCache atlasCache = new AtlasRestCache(main.atlasCachePath, idMapper);
			WikiPathwaysCache wpCache = new WikiPathwaysCache(main.wpCachePath);

			log.info("Starting hibernate session");
			sessionFactory = AtlasSessionUtils.createSessionFactory(main.sessionConfig);
			sessionFactory.getCurrentSession().beginTransaction();
			
			AtlasSessionManager sessionMgr = new AtlasSessionManager(sessionFactory);

			switch(main.action) {
			case ALL:
				sessionMgr.updateAtlas(atlasCache, ignoreExperiments);
				sessionMgr.updateWikiPathways(wpCache, idMapper);
				break;
			case PATHWAY:
				sessionMgr.updateWikiPathways(wpCache, idMapper);
				break;
			case ATLAS:
				sessionMgr.updateAtlas(atlasCache, ignoreExperiments);
				break;
			}


		} catch(Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			e.printStackTrace();
			System.exit(-2);
		} finally {
			log.info("Closing hibernate session");
			if(sessionFactory != null) sessionFactory.close();
		}
	}
}
