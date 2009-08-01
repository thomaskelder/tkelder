package gcharts;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.DataDerby;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pathvisio.debug.Logger;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.preferences.PreferenceManager;

public class GChartsGexVennMain {
	@Option(name = "-gex", required = true, usage = "The dataset to user.")
	private File gexFile;

	@Option(name = "-c1", required = true, usage = "The first criterion.")
	private String c1;

	@Option(name = "-c2", required = true, usage = "The second criterion.")
	private String c2;
	
	@Option(name = "-c3", required = true, usage = "The third criterion.")
	private String c3;
	
	@Option(name = "-out", required = true, usage = "The file to save the image to.")
	private File outFile;
	
	@Option(name = "-title", required = false, usage = "The diagram title.")
	private String title;
	
	public static void main(String[] args) {
		PreferenceManager.init();
		BioDataSource.init();
		
		GChartsGexVennMain main = new GChartsGexVennMain();
		CmdLineParser parser = new CmdLineParser(main);
		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			e.printStackTrace();
			parser.printUsage(System.err);
			System.exit(-1);
		}
		
		try {
			SimpleGex data = new SimpleGex("" + main.gexFile, false, new DataDerby());
			
			GChartsGexVenn gexVenn = new GChartsGexVenn(data);
			
			Logger.log.info("Creating venn for: ");
			Logger.log.info(main.c1);
			Logger.log.info(main.c2);
			Logger.log.info(main.c3);
			
			gexVenn.calculateMatches(new String[] { main.c1, main.c2, main.c3 });
			BufferedImage venn = gexVenn.createDiagram(main.title, main.c1, main.c2, main.c3);
			
			if(main.outFile != null) {
				Logger.log.info("Downloading chart to " + main.outFile);
				ImageIO.write(venn, "png", main.outFile);
			}
		} catch(Throwable e) {
			e.printStackTrace();
			System.exit(-2);
		}
	}
}
