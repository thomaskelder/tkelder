package pps2;

import java.io.File;
import java.io.IOException;

import org.bridgedb.IDMapperException;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.DataDerby;
import org.bridgedb.rdb.IDMapperRdb;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.plugins.statistics.StatisticsResult;
import org.pathvisio.preferences.PreferenceManager;
import org.pathvisio.utils.GexUtil;
import org.pathvisio.utils.StatResultsUtil;
import org.pathvisio.utils.StatResultsUtil.FilterZScoreOptions;
import org.pathvisio.visualization.colorset.Criterion.CriterionException;

/**
 * Calculate z-scores for each time-point with criteria:
 * - up enriched ([q-value] < 0.05 && [fc] > 0)
 * - down enriched ([q-value] < 0.05 && [fc] < 0)
 * 
 * Also merge into a single file so we can make a heatmap
 * @author thomas
 */
public class ZScoreTimeHFvsLF {
	public static void main(String[] args) {
		try {
			ZScoreTimeHFvsLF main = new ZScoreTimeHFvsLF();
			main.start();
			main.togetherAtT2();
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
	
	private File gexFile = new File(
			"/home/thomas/projects/pps2/stat_results/PPS2_average 2logratio_HFvsLF per tp.pgex"
	);

	private File outDir = new File(
			"/home/thomas/projects/pps2/path_results/bigcat/HFvsLF/zscores"
	);
	
	String[] timePoints = new String[] {
			"t0", "t0.6", "t2", "t48"
	};
	
	SimpleGex data;
	IDMapperRdb idMapper;
	
	public ZScoreTimeHFvsLF() throws IDMapperException {
		outDir.mkdirs();
		PreferenceManager.init();
		BioDataSource.init();
		
		data = new SimpleGex("" + gexFile, false, new DataDerby());
		idMapper = ConstantsPPS2.getIdMapper();
	}
	
	void start() throws IDMapperException, IOException {
		StatisticsResult[] resultAll = new StatisticsResult[timePoints.length];
		StatisticsResult[] resultDown = new StatisticsResult[timePoints.length];
		StatisticsResult[] resultUp = new StatisticsResult[timePoints.length];
		
		for(int i = 0; i < timePoints.length; i++) {
			String time = timePoints[i];
			String exprAll = "[qvalue_HFvsLF_" + time + "] < 0.05";
			String exprDown = "[qvalue_HFvsLF_" + time + "] < 0.05 AND [logratio_" + time + "_HFvsLF] < 0";
			String exprUp = "[qvalue_HFvsLF_" + time + "] < 0.05 AND [logratio_" + time + "_HFvsLF] > 0";
			
			resultAll[i] = StatResultsUtil.calculateZscores(exprAll, ConstantsPPS2.pathwayDir, data, idMapper);
			resultDown[i] = StatResultsUtil.calculateZscores(exprDown, ConstantsPPS2.pathwayDir, data, idMapper);
			resultUp[i] = StatResultsUtil.calculateZscores(exprUp, ConstantsPPS2.pathwayDir, data, idMapper);
			
			resultAll[i].save(new File(outDir, "zscores_detail_" + time + "_all.txt"));
			resultDown[i].save(new File(outDir, "zscores_detail_" + time + "_down.txt"));
			resultUp[i].save(new File(outDir, "zscores_detail_" + time + "_up.txt"));
			StatResultsUtil.writeDetailed(resultAll[i], resultUp[i], resultDown[i], new File(outDir, "zscores_detail_" + time + "_allupdown.txt"));
		}
		
		//Write to txt files
		StatResultsUtil.write(resultAll, paste("", timePoints, "_all"), new File(outDir, "zscores_all_HFvsLF_time.txt"), 
				new FilterZScoreOptions());
		StatResultsUtil.writeSigned(resultUp, resultDown, timePoints, new File(outDir, "zscores_signed_HFvsLF_time.txt"), 
				new FilterZScoreOptions());
		StatResultsUtil.writeSigned(resultUp, resultDown, timePoints, new File(outDir, "zscores_signed_HFvsLF_time_z2.txt"),
				new FilterZScoreOptions().threshold(2));
	}
	
	/**
	 * Finds genes that "come together" at t2 (are differentially expressed at t0, but not at t2).
	 */
	void togetherAtT2() throws IDMapperException, IOException, CriterionException {
		String crit = "( [qvalue_HFvsLF_t0] < 0.05 AND ( [logratio_t0_HFvsLF] >= 0.26 OR [logratio_t0_HFvsLF] <= -0.26)) AND " +
				"([qvalue_HFvsLF_t2] >= 0.05 AND ([logratio_t2_HFvsLF] <= 0.07 AND [logratio_t2_HFvsLF] >= -0.07))";
		
		StatisticsResult result = StatResultsUtil.calculateZscores(crit, ConstantsPPS2.pathwayDir, data, idMapper);
		result.save(new File(outDir, "zscores_togetherAtT2.txt"));
		
		//Also print a file containing the genes that satisfy this criterion
		GexUtil.exportGenesMatchingCriterion(new File(outDir, "genes_togetherAtT2.txt"), crit, data, idMapper);
	}
	
	String[] paste(String before, String[] data, String after) {
		String[] pasted = new String[data.length];
		for(int i = 0; i < data.length; i++) pasted[i] = before + data[i] + after;
		return pasted;
	}
}
