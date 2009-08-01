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
 * Calculate z-scores for each t0 versus tn comparison with criteria:
 * - up enriched ([q-value] < 0.05 && [fc] > 0)
 * - down enriched ([q-value] < 0.05 && [fc] < 0)
 * 
 * Merge into a single file so we can make a heatmap
 * @author thomas
 */
public class ZScoreTimeVsZero {
	public static void main(String[] args) {
		try {
			new ZScoreTimeVsZero().start();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private File gdbFile = new File(
			"/home/thomas/PathVisio-Data/gene_databases/Mm_Derby_20090509.pgdb"
	);

	private File gexFile = new File(
			"/home/thomas/projects/pps2/stat_results/PPS2_timecourse_pathvisio.pgex"
	);

	private File pwDir = new File(
			"/home/thomas/data/pathways/20090715"
	);

	private File outDir = new File(
			"/home/thomas/projects/pps2/path_results/bigcat/zscore-time-vs-t0"
	);
	
	String[] timePoints = new String[] {
			"t0.6", "t2", "t48"
	};
	
	String[] diets = new String[] { "HF", "LF" };
	
	void start() throws IDMapperException, IOException {
		outDir.mkdirs();
		PreferenceManager.init();
		BioDataSource.init();
		
		SimpleGex data = new SimpleGex("" + gexFile, false, new DataDerby());
		IDMapperRdb idMapper = SimpleGdbFactory.createInstance("" + gdbFile, new DataDerby(), 0);
		
		StatisticsResult[][] resultAll = new StatisticsResult[diets.length][timePoints.length];
		StatisticsResult[][] resultDown = new StatisticsResult[diets.length][timePoints.length];
		StatisticsResult[][] resultUp = new StatisticsResult[diets.length][timePoints.length];
		
		for(int d = 0; d < diets.length; d++) {
			String diet = diets[d];
			for(int t = 0; t < timePoints.length; t++) {
				String time = timePoints[t];
				String var_q = "[" + diet + "_limma_t0_vs_" + time + "_qvalue]";
				String var_r = "[" + diet + "_log2_" + time + "vst0]";
				String exprAll = var_q + " < 0.05";
				String exprDown = var_q + " < 0.05 AND " + var_r + " < 0";
				String exprUp = var_q + " < 0.05 AND " + var_r + " > 0"; 
				resultAll[d][t] = StatResultsUtil.calculateZscores(exprAll, pwDir, data, idMapper);
				resultDown[d][t] = StatResultsUtil.calculateZscores(exprDown, pwDir, data, idMapper);
				resultUp[d][t] = StatResultsUtil.calculateZscores(exprUp, pwDir, data, idMapper);
				
				resultAll[d][t].save(new File(outDir, "zscores_detail_" + diet + "_" + time + "_all.txt"));
				resultDown[d][t].save(new File(outDir, "zscores_detail_" + diet + "_" + time + "_down.txt"));
				resultUp[d][t].save(new File(outDir, "zscores_detail_" + diet + "_" + time + "_up.txt"));
			}
		}
		
		//Write to txt files
		int iHF = 0;
		int iLF = 1;
		//File for each timepoint comparing HF and LF
		for(int t = 0; t < timePoints.length; t++) {
			String time = timePoints[t];
			
			StatResultsUtil.write(new StatisticsResult[] {
					resultAll[iHF][t], 	resultAll[iLF][t]
			}, paste("t0_vs_" + time + "_", diets, ""), new File(outDir, "zscores_HF_LF_t0_vs_" + time + "_z2.txt"),
				new FilterZScoreOptions().threshold(2)
			);
			
		}
		//File for HF and LF comparing each timepoint
		StatResultsUtil.write(new StatisticsResult[] {
				resultAll[iHF][0], 	resultAll[iHF][1], resultAll[iHF][2]
		}, paste("t0_vs_", timePoints, ""), new File(outDir, "zscores_HF_time_vs_t0_z2.txt"), 
		new FilterZScoreOptions().threshold(2));
		StatResultsUtil.write(new StatisticsResult[] {
				resultAll[iLF][0], 	resultAll[iLF][1], resultAll[iLF][2]
		}, paste("t0_vs_", timePoints, ""), new File(outDir, "zscores_LF_time_vs_t0_z2.txt"), 
		new FilterZScoreOptions().threshold(2));
	}
	
	String[] paste(String before, String[] data, String after) {
		String[] pasted = new String[data.length];
		for(int i = 0; i < data.length; i++) pasted[i] = before + data[i] + after;
		return pasted;
	}
}
