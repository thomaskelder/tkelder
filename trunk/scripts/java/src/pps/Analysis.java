package pps;

import java.io.File;
import java.io.IOException;

import org.pathvisio.data.DataDerby;
import org.pathvisio.data.DataException;
import org.pathvisio.data.Gdb;
import org.pathvisio.data.SimpleGdbFactory;
import org.pathvisio.data.SimpleGex;
import org.pathvisio.plugins.statistics.StatisticsResult;
import org.pathvisio.plugins.statistics.ZScoreCalculator;
import org.pathvisio.visualization.colorset.Criterion;

public class Analysis {
	private static final File gdb = new File(
			"/home/thomas/PathVisio-Data/gene databases/Mm_Derby_20081119.pgdb"
	);
	
	private static final File gex = new File(
			"/home/thomas/projects/pps/filtered_data/Combined_total.pgex"
	);

	private static final File pwDir = new File(
			"/home/thomas/data/pathways/WP20090220/mouse"
	);
	
	private static final File outDir = new File(
			"/home/thomas/projects/pps/path_results"
	);

	private static final String[] tissues = new String[] {
		"Liver", "Muscle", "WAT"
	};
	
	private static final String[] crits = new String[] {
		//"[TISSUE_oriogen_profile] = 1",
		//"[TISSUE_oriogen_profile] = 5",
		//"[TISSUE_oriogen_profile] = 2 OR [TISSUE_oriogen_profile] = 3 OR [TISSUE_oriogen_profile] = 4",
		//"[TISSUE_oriogen_profile] = 6 OR [TISSUE_oriogen_profile] = 7 OR [TISSUE_oriogen_profile] = 8",
		//"[TISSUE_oriogen_profile] > 0"
		"[anova_pvalue_TISSUE] < 0.05"
	};
	
	private static final String[] critNames = new String[] {
		//"p1",
		//"p5",
		//"p2.3.4",
		//"p6.7.8",
		//"pany",
		"anov0.05",
	};
	
	public static void main(String[] args) {
		try {
		//Calculate z-score for each sample and cluster
		for(String t : tissues) {
			for(int i = 0; i < crits.length; i++) {
				calculateZscore(gex, gdb, pwDir,
						new File(outDir, "zscore_" + t + "_" + critNames[i] + ".txt"),
						crits[i].replaceAll("TISSUE", t)
				);
			}
		}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static void calculateZscore(File pgex, File pgdb, File pwDir, File report, String critStr) throws DataException, IOException {
		SimpleGex gex = new SimpleGex("" + pgex, false, new DataDerby());
		Gdb gdb = SimpleGdbFactory.createInstance("" + pgdb, new
				DataDerby(), 0);
		Criterion crit = new Criterion ();
		crit.setExpression(critStr, gex.getSampleNames().toArray(new String[0]));
		ZScoreCalculator zsc = new ZScoreCalculator(
				crit, pwDir, gex, gdb, null
		);
		StatisticsResult result = zsc.calculateAlternative();
		result.save(report);
	}
}
