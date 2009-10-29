package pps2;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bridgedb.IDMapperException;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.DataDerby;
import org.pathvisio.Engine;
import org.pathvisio.gex.CachedData;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.gui.swing.PvDesktop;
import org.pathvisio.gui.swing.SwingEngine;
import org.pathvisio.model.ConverterException;
import org.pathvisio.plugins.statistics.StatisticsExporter;
import org.pathvisio.plugins.statistics.StatisticsResult;
import org.pathvisio.preferences.PreferenceManager;
import org.pathvisio.utils.StatsUtil;
import org.pathvisio.view.MIMShapes;
import org.pathvisio.view.ShapeRegistry;
import org.pathvisio.visualization.Visualization;
import org.pathvisio.visualization.plugins.VisualizationPlugin;

/**
 * Calculate z-scores for pps2 illumina data on kidney samples.
 * 
 * @author thomas
 */
public class ZScoreKidney {
	public static void main(String[] args) {
		try {
			ZScoreKidney main = new ZScoreKidney();
			main.start();
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
	
	private double pvalue = 0.05;
	
	private File gexFile = new File(
			"/home/thomas/projects/pps2/heeringa/pps2_kidney_illumina.pgex"
	);

	private File outDir = new File(
			"/home/thomas/projects/pps2/heeringa/pathway"
	);
	
	SimpleGex data;
	CachedData cache;
	
	public ZScoreKidney() throws IDMapperException {
		outDir.mkdirs();
		PreferenceManager.init();
		BioDataSource.init();
		
		data = new SimpleGex("" + gexFile, false, new DataDerby());
		cache = new CachedData(data);
		cache.setMapper(ConstantsPPS2.getIdMapper());
	}
	
	
	void start() throws IDMapperException, IOException, ConverterException {
		String[] tissues = new String[] { "COR", "MED", "WK" };
		
		//Setup engine and desktop for html exporter
		Engine engine = new Engine();
		PvDesktop pvDesktop = new PvDesktop(new SwingEngine(engine));
		new VisualizationPlugin().init(pvDesktop);

		pvDesktop.getGexManager().setCurrentGex(data);

		Map<String, Visualization> visByName = new HashMap<String, Visualization>();
		for(Visualization vis : pvDesktop.getVisualizationManager().getVisualizations()) {
			visByName.put(vis.getName(), vis);
		}
		
		StatisticsExporter exporter = new StatisticsExporter(
				ConstantsPPS2.getIdMapper(), pvDesktop.getVisualizationManager(), pvDesktop.getGexManager()
		);
		
		for(String tis : tissues) {
			String expr = "[p.value." + tis + ".HFvsLF] <= " + pvalue;
			StatisticsResult res = StatsUtil.calculateZscores(
					expr, ConstantsPPS2.pathwayDir, pvDesktop.getGexManager(), ConstantsPPS2.getIdMapper()
			);
			res.save(new File(outDir, "zscore_summary_" + tis + ".txt"));
			
			//Write html files to browse the results
			File htmlPath = new File(outDir, "pathways_" + tis);
			htmlPath.mkdirs();
			exporter.export(htmlPath, res, visByName.get(tis + " HFvsLF"));
		}
	}
}