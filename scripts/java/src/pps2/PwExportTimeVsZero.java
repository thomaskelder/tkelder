package pps2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bridgedb.IDMapperException;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.DataDerby;
import org.bridgedb.rdb.IDMapperRdb;
import org.pathvisio.Engine;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.gui.swing.PvDesktop;
import org.pathvisio.gui.swing.SwingEngine;
import org.pathvisio.model.ConverterException;
import org.pathvisio.plugin.PluginManager;
import org.pathvisio.plugins.statistics.StatisticsExporter;
import org.pathvisio.plugins.statistics.StatisticsResult;
import org.pathvisio.preferences.PreferenceManager;
import org.pathvisio.util.FileUtils;
import org.pathvisio.utils.StatsUtil;
import org.pathvisio.visualization.Visualization;

/**
 * Calculate z-scores for each t0 versus tn comparison with criteria:
 * - up enriched ([q-value] < 0.05 && [fc] > 0)
 * - down enriched ([q-value] < 0.05 && [fc] < 0)
 * 
 * @author thomas
 */
public class PwExportTimeVsZero {
	public static void main(String[] args) {
		try {
			new PwExportTimeVsZero().start();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private File gexFile = new File(
			"/home/thomas/projects/pps2/stat_results/PPS2_timecourse_pathvisio.pgex"
	);

	private File outDir = new File(
			"/home/thomas/projects/pps2/path_results/bigcat/TimeVsZero/pathways"
	);
	
	String[] timePoints = new String[] {
			"t0.6", "t2", "t48"
	};
	
	String[] diets = new String[] { "HF", "LF" };
	SimpleGex data;
	IDMapperRdb idMapper;
	PvDesktop pvDesktop;
	StatisticsExporter exporter;
	List<File> gpmlFiles = FileUtils.getFiles(ConstantsPPS2.pathwayDir, "gpml", true);
	
	public PwExportTimeVsZero() throws IDMapperException {
		outDir.mkdirs();
		PreferenceManager.init();
		BioDataSource.init();
		
		idMapper = ConstantsPPS2.getIdMapper();

		Engine engine = new Engine();
		pvDesktop = new PvDesktop(new SwingEngine(engine));
		List<String> plugins = new ArrayList<String>();
		plugins.add("/home/thomas/code/git/pathvisio/visplugins.jar");
		PluginManager pluginManager = new PluginManager(
			plugins, pvDesktop
		);
		
		data = new SimpleGex("" + gexFile, false, new DataDerby());
		pvDesktop.getGexManager().setCurrentGex(data);

		exporter = new StatisticsExporter(
			idMapper, pvDesktop.getVisualizationManager(), pvDesktop.getGexManager().getCurrentGex()	
		);
	}
	
	void setVisualization(String visName) {
		for(Visualization v : pvDesktop.getVisualizationManager().getVisualizations()) {
			if(v.getName().equals(visName)) {
				pvDesktop.getVisualizationManager().setActiveVisualization(v);
			}
		}
	}
	
	void start() throws IDMapperException, IOException, ConverterException {
		for(int d = 0; d < diets.length; d++) {
			String diet = diets[d];
			for(int t = 0; t < timePoints.length; t++) {
				String time = timePoints[t];
				String var_q = "[" + diet + "_limma_t0_vs_" + time + "_qvalue]";
				String expr = var_q + " < 0.05";
				
				StatisticsResult result = StatsUtil.calculateZscores(
						expr, ConstantsPPS2.pathwayDir, data, idMapper);
				setVisualization(diet);
				exporter.export(new File(outDir, "html_" + diet + "_" + time), result, pvDesktop.getVisualizationManager().getActiveVisualization());
			}
		}
	}
}
