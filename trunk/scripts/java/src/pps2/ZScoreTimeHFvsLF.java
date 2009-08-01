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

import pps2.StatResultsUtil.WriteZScoreOptions;

/**
 * Calculate z-scores for each time-point with criteria:
 * - up enriched ([q-value] < 0.05 && [fc] > 0)
 * - down enriched ([q-value] < 0.05 && [fc] < 0)
 * 
 * Merge into a single file so we can make a heatmap
 * @author thomas
 */
public class ZScoreTimeHFvsLF {
	public static void main(String[] args) {
		try {
			new ZScoreTimeHFvsLF().start();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private File gdbFile = new File(
			"/home/thomas/PathVisio-Data/gene_databases/Mm_Derby_20090509.pgdb"
	);

	private File gexFile = new File(
			"/home/thomas/projects/pps2/stat_results/pps2_HFvsLF_alltimes.pgex"
	);

	private File pwDir = new File(
			"/home/thomas/data/pathways/20090715"
	);

	private File outDir = new File(
			"/home/thomas/projects/pps2/path_results/bigcat/zscore-LFvsHF-time"
	);
	
	String[] timePoints = new String[] {
			"t0", "t0.6", "t2", "t48"
	};
	
	void start() throws IDMapperException, IOException {
		outDir.mkdirs();
		PreferenceManager.init();
		BioDataSource.init();
		
		SimpleGex data = new SimpleGex("" + gexFile, false, new DataDerby());
		IDMapperRdb idMapper = SimpleGdbFactory.createInstance("" + gdbFile, new DataDerby(), 0);
		
		StatisticsResult[] resultAll = new StatisticsResult[timePoints.length];
		StatisticsResult[] resultDown = new StatisticsResult[timePoints.length];
		StatisticsResult[] resultUp = new StatisticsResult[timePoints.length];
		
		for(int i = 0; i < timePoints.length; i++) {
			String time = timePoints[i];
			String exprAll = "[q_" + time + "] < 0.05";
			String exprDown = "[q_" + time + "] < 0.05 AND [HF_vs_LF_" + time + "] < 0";
			String exprUp = "[q_" + time + "] < 0.05 AND [HF_vs_LF_" + time + "] > 0";
			
			resultAll[i] = StatResultsUtil.calculateZscores(exprAll, pwDir, data, idMapper);
			resultDown[i] = StatResultsUtil.calculateZscores(exprDown, pwDir, data, idMapper);
			resultUp[i] = StatResultsUtil.calculateZscores(exprUp, pwDir, data, idMapper);
			
			resultAll[i].save(new File(outDir, "zscores_detail_" + time + "_all.txt"));
			resultDown[i].save(new File(outDir, "zscores_detail_" + time + "_down.txt"));
			resultUp[i].save(new File(outDir, "zscores_detail_" + time + "_up.txt"));
		}
		
		//Write to txt files
		StatResultsUtil.write(resultAll, paste("", timePoints, "_all"), new File(outDir, "zscores_all_HFvsLF_time.txt"), 
				new FilterZScoreOptions());
		StatResultsUtil.writeSigned(resultUp, resultDown, timePoints, new File(outDir, "zscores_signed_HFvsLF_time.txt"), 
				new FilterZScoreOptions());
		StatResultsUtil.writeSigned(resultUp, resultDown, timePoints, new File(outDir, "zscores_signed_HFvsLF_time_z2.txt"),
				new FilterZScoreOptions().threshold(2));
	}
	
	String[] paste(String before, String[] data, String after) {
		String[] pasted = new String[data.length];
		for(int i = 0; i < data.length; i++) pasted[i] = before + data[i] + after;
		return pasted;
	}
}
