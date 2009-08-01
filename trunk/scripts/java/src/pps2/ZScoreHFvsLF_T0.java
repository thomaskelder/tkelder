package pps2;

import java.io.File;
import java.io.IOException;

import org.bridgedb.IDMapperException;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.DataDerby;
import org.bridgedb.rdb.IDMapperRdb;
import org.bridgedb.rdb.SimpleGdbFactory;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.plugins.statistics.StatisticsResult;
import org.pathvisio.preferences.PreferenceManager;
import org.pathvisio.utils.StatResultsUtil;
import org.pathvisio.utils.StatResultsUtil.FilterZScoreOptions;

/**
 * Calculate z-scores for each time-point with criteria:
 * - up enriched ([q-value] < 0.05 && [fc] > 0)
 * - down enriched ([q-value] < 0.05 && [fc] < 0)
 * 
 * Merge into a single file so we can make a heatmap
 * @author thomas
 */
public class ZScoreHFvsLF_T0 {
	public static void main(String[] args) {
		try {
			new ZScoreHFvsLF_T0().start();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private File gdbFile = new File(
			"/home/thomas/PathVisio-Data/gene_databases/Mm_Derby_20090509.pgdb"
	);

	private File gexFile = new File(
			"/home/thomas/projects/pps2/stat_results/PPS2_HFvsLF t0_average 2logratio stats.pgex"
	);

	private File pwDir = new File(
			"/home/thomas/data/pathways/20090715"
	);

	private File outDir = new File(
			"/home/thomas/projects/pps2/path_results/bigcat/pathvisio"
	);

	void start() throws IDMapperException, IOException {
		outDir.mkdirs();
		PreferenceManager.init();
		BioDataSource.init();

		SimpleGex data = new SimpleGex("" + gexFile, false, new DataDerby());
		IDMapperRdb idMapper = SimpleGdbFactory.createInstance("" + gdbFile, new DataDerby(), 0);

		String exprAll = "[q-value] <= 0.05";
		String exprDown = "[q-value] <= 0.05 AND [log2_HFvsLF_t0] < 0";
		String exprUp = "[q-value] <= 0.05 AND [log2_HFvsLF_t0] > 0";

		StatisticsResult resultAll = StatResultsUtil.calculateZscores(exprAll, pwDir, data, idMapper);
		StatisticsResult resultDown = StatResultsUtil.calculateZscores(exprDown, pwDir, data, idMapper);
		StatisticsResult resultUp = StatResultsUtil.calculateZscores(exprUp, pwDir, data, idMapper);

		resultAll.save(new File(outDir, "zscores_detail_HFvsLF_t0_all.txt"));
		resultDown.save(new File(outDir, "zscores_detail_HFvsLF_t0_down.txt"));
		resultUp.save(new File(outDir, "zscores_detail_HFvsLF_t0_up.txt"));

		//Write to txt files
		StatResultsUtil.write(
				new StatisticsResult[] { resultAll,resultDown, resultUp },
				new String[] { "ZScore", "ZScore down", "ZScore up" },
				new File(outDir, "zscores_combined_HFvsLF_t0.txt"),
				new FilterZScoreOptions()
		);
	}
}
