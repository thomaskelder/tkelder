package org.wikipathways.stats;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.wikipathways.stats.db.WPDatabase;

/**
 * Generate WikiPathways usage statistics and graphs
 * @author thomas
 *
 */
public class WikiPathwaysStats {
	@Option(name = "-tasks", required = false, usage = "Task(s) to perform.")
	private List<String> taskNames = new ArrayList<String>();

	@Option(name = "-server", required = true, usage = "Server containing the WikiPathway database.")
	private String server;
	
	@Option(name = "-database", required = true, usage = "Database name.")
	private String database;
	
	@Option(name = "-user", required = true, usage = "MySQL username.")
	private String user;
	
	@Option(name = "-pass", required = true, usage = "MySQL pass.")
	private String pass;
	
	private TaskRegistry tasks = new TaskRegistry();
	
	public static void main(String[] args) {
		WikiPathwaysStats main = new WikiPathwaysStats();
		CmdLineParser parser = new CmdLineParser(main);
		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			e.printStackTrace();
			parser.printUsage(System.err);
			System.exit(-1);
		}
		
		try {
			main.runAll();
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(-2);
		}
	}
	
	public void runAll() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, TaskException {
		WPDatabase db = new WPDatabase(server, database, user, pass);
		TaskParameters par = new TaskParameters();
		
		if(taskNames.size() == 0) {
			for(Task t : tasks.getAllTasks()) {
				t.start(db, par);
			}
		} else {
			for(String tn : taskNames) {
				System.out.println("Starting task " + tn);
				Task t = tasks.getTask(tn);
				t.start(db, par);
			}
		}
	}
}
