package pps;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bridgedb.DataDerby;
import org.bridgedb.DataException;
import org.bridgedb.Gdb;
import org.bridgedb.SimpleGdbFactory;
import org.pathvisio.Engine;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.gui.swing.PvDesktop;
import org.pathvisio.gui.swing.SwingEngine;
import org.pathvisio.plugins.statistics.StatisticsHtmlExporter;
import org.pathvisio.plugins.statistics.StatisticsResult;
import org.pathvisio.plugins.statistics.ZScoreCalculator;
import org.pathvisio.preferences.PreferenceManager;
import org.pathvisio.visualization.Visualization;
import org.pathvisio.visualization.colorset.Criterion;
import org.pathvisio.visualization.plugins.VisualizationPlugin;

/**
 * PPS1 pathway analysis. Calculates z-scores for each tissue and
 * export a html pathway set.
 * @author thomas
 */
public class Analysis {
	private static final File gdbFile = new File(
			"/home/thomas/PathVisio-Data/gene databases/Mm_Derby_20081119.pgdb"
	);

	private static final File gexFile = new File(
			"/home/thomas/projects/pps1/stat_results/pps1_expr_anova.pgex"
	);

	private static final File pwDir = new File(
			"/home/thomas/data/pathways/pps1-gpml"
	);

	private static final File outDir = new File(
			"/home/thomas/projects/pps1/path_results/pathvisio-z"
	);

	private static final String[] tissues = new String[] {
		"Liver", "Muscle", "WAT"
	};

	private static final String[] crits = new String[] {
		"[anova_qvalue_TISSUE] <= 0.01",
		//"[anova_qvalue_TISSUE] <= 0.01 AND [max_abs_fc_TISSUE] >= 0.25",
	};

	private static final String[] critNames = new String[] {
		"anova_q0.01",
		//"anova_q0.01_maxfc0.25"
	};

	public static void main(String[] args) {
		try {
			PreferenceManager.init();

			SimpleGex gex = new SimpleGex("" + gexFile, false, new DataDerby());
			Gdb gdb = SimpleGdbFactory.createInstance("" + gdbFile, new
					DataDerby(), 0);

			Engine engine = new Engine();
			PvDesktop pvDesktop = new PvDesktop(new SwingEngine(engine));
			new VisualizationPlugin().init(pvDesktop);

			pvDesktop.getGexManager().setCurrentGex(gex);

			Map<String, Visualization> visByName = new HashMap<String, Visualization>();
			for(Visualization vis : pvDesktop.getVisualizationManager().getVisualizations()) {
				visByName.put(vis.getName(), vis);
			}

			StatisticsHtmlExporter exporter = new StatisticsHtmlExporter(
					gdb, pvDesktop.getVisualizationManager(), gex
			);
			
			//Calculate z-score for each sample and cluster
			for(String t : tissues) {
				for(int i = 0; i < crits.length; i++) {
					StatisticsResult r = calculateZscore(gex, gdb, pwDir,
							new File(outDir, "zscore_" + t + "_" + critNames[i] + ".txt"),
							crits[i].replaceAll("TISSUE", t)
					);
					
					File htmlPath = new File(outDir, "html_" + t + "_" + critNames[i]);
					htmlPath.mkdir();
					exporter.export(
							htmlPath, 
							r, visByName.get(t)
					);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static StatisticsResult calculateZscore(SimpleGex gex, Gdb gdb, File pwDir, File report, String critStr) throws DataException, IOException {
		Criterion crit = new Criterion ();
		crit.setExpression(critStr, gex.getSampleNames());
		ZScoreCalculator zsc = new ZScoreCalculator(
				crit, pwDir, gex, gdb, null
		);
		StatisticsResult result = zsc.calculateAlternative();
		result.save(report);
		return result;
	}
}
