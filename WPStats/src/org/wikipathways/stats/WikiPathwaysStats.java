package org.wikipathways.stats;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.jfree.chart.JFreeChart;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
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
				System.out.println("Running task " + t);
				t.start(db, par);
				System.err.println("---");
			}
		} else {
			for(String tn : taskNames) {
				System.out.println("Starting task " + tn);
				Task t = tasks.getTask(tn);
				t.start(db, par);
				Runtime.getRuntime().gc();
			}
		}
	}
	
	/**
	 * Exports a JFreeChart to a SVG file.
	 * 
	 * From: http://dolf.trieschnigg.nl/jfreechart/
	 * 
	 * @param chart JFreeChart to export
	 * @param bounds the dimensions of the viewport
	 * @param svgFile the output file.
	 * @throws IOException if writing the svgFile fails.
	 */
	public static void exportChartAsSVG(JFreeChart chart, Rectangle bounds, File svgFile) throws IOException {
        // Get a DOMImplementation and create an XML document
        DOMImplementation domImpl =
            GenericDOMImplementation.getDOMImplementation();
        Document document = domImpl.createDocument(null, "svg", null);

        // Create an instance of the SVG Generator
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

        // draw the chart in the SVG generator
        chart.draw(svgGenerator, bounds);

        // Write svg file
        OutputStream outputStream = new FileOutputStream(svgFile);
        Writer out = new OutputStreamWriter(outputStream, "UTF-8");
        svgGenerator.stream(out, true /* use css */);						
        outputStream.flush();
        outputStream.close();
	}
}
