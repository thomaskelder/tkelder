package org.pathvisio.plugins.statistics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.bridgedb.DataException;
import org.bridgedb.Gdb;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.model.ConverterException;
import org.pathvisio.model.Pathway;
import org.pathvisio.plugins.HtmlExporter;
import org.pathvisio.visualization.Visualization;
import org.pathvisio.visualization.VisualizationManager;

/**
 * Exports PathVisio statistics results to a set of HTML pages. The index page consists
 * of two frames:
 * - left: a table with the statistics results (sorted by z-scores)
 * - right: the pathway image (shown by clicking a pathway in the left table)
 * @author thomas
 */
public class StatisticsHtmlExporter {
	Gdb gdb;
	VisualizationManager visMgr;
	SimpleGex gex;

	public StatisticsHtmlExporter(Gdb gdb, VisualizationManager visMgr, SimpleGex gex) {
		this.gdb = gdb;
		this.visMgr = visMgr;
		this.gex = gex;
	}

	public void export(File htmlPath, StatisticsResult result, Visualization visualization) throws ConverterException, IOException, DataException {
		visMgr.setActiveVisualization(visualization);
		HtmlExporter exporter = new HtmlExporter(gdb, visMgr, gex);

		String statTable = "<HTML>" +
				"<HEAD><LINK rel=\"stylesheet\" type=\"text/css\" href=\"stats.css\"></HEAD>";
		statTable += "<BODY><P>";
		
		//Add header with general info
		statTable += "<UL>";
		statTable += "<LI><B>Dataset:</B> " + result.gex.getDbName();
		statTable += "<LI><B>Pathway directory:</B> " + result.pwDir;
		statTable += "<LI><B>Gene database:</B> " + result.gdb.getDbName();
		statTable += "<LI><B>Criterion:</B> " + result.crit.getExpression();
		statTable += "<LI><B>Rows in data (N):</B> " + result.bigN;
		statTable += "<LI><B>Rows meeting criterion (R):</B> " + result.bigR;
		
		statTable += "</UL><TABLE><TBODY>";
		statTable += "<TH>Pathway<TH>n<TH>r<TH>z";
		
		for(StatisticsPathwayResult r : result.getPathwayResults()) {
			//Export the pathway
			Pathway p = new Pathway();
			p.readFromXml(r.getFile(), true);
			if(gex != null) gex.cacheData(p.getDataNodeXrefs(), null, gdb);

			String prefix = r.getFile().getName();
			File htmlFile = exporter.doExport(p, prefix, htmlPath);
			
			//Add a row to the table
			String pwLink = "<A href=\"" + htmlFile.getName() + "\"" +
					" target=\"pathwayframe\">" +
					r.getProperty(Column.PATHWAY_NAME) + "</A>";
			statTable += "<TR><TD>" + pwLink;
			statTable += "<TD>" + r.getProperty(Column.N);
			statTable += "<TD>" + r.getProperty(Column.R);
			statTable += "<TD>" + r.getProperty(Column.ZSCORE);
		}
		statTable += "</TBODY></TABLE></BODY></HTML>";
		
		File statFile = new File(htmlPath, "stats.html");
		Writer statOut = new FileWriter(statFile);
		statOut.append(statTable);
		statOut.close();
		
		//Page with two frames:
		//left -> zscore table
		//right -> pathway image
		String frames = "<FRAMESET cols=\"25%,*\">";
		frames += "<FRAME src=\"" + statFile.getName() + "\"/>";
		frames += "<FRAME src=\"\" name = \"pathwayframe\" />";
		frames += "</FRAMESET>";
		
		File indexFile = new File(htmlPath, "index.html");
		Writer indexOut = new FileWriter(indexFile);
		indexOut.append("<HTML>" + frames + "</HTML>");
		indexOut.close();
		
		String css = 
			" *{ font-size: 10pt; }" +	
			" ul { padding: 0; margin: 1em; }" +
			" table { margin:1px 1px 1px 0; background:#F9F9F9; border:1px #AAA solid; " +
			" border-collapse:collapse; }" +
			" td { border: solid 1px rgb(200,200,200); padding:1px 3px 1px 3px; }";
		File cssFile = new File(htmlPath, "stats.css");
		Writer cssOut = new FileWriter(cssFile);
		cssOut.append(css);
		cssOut.close();
	}
}
