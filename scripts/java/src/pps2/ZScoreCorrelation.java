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

public class ZScoreCorrelation {
	public static void main(String[] args) {
		try {
			new ZScoreCorrelation().start();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private File gdbFile = new File(
			"/home/thomas/PathVisio-Data/gene_databases/Mm_Derby_20090509.pgdb"
	);

	private File gexFile = new File(
			"/home/thomas/projects/pps2/stat_results/bigcat/correlation/corr_t0_p_t12.pgex"
	);

	private File pwDir = new File(
			"/home/thomas/data/pathways/20090730/mmu"
	);

	private File outDir = new File(
			"/home/thomas/projects/pps2/path_results/bigcat/correlation"
	);
	
	String[] measurements = new String[] {
			"Insulin_t12", "Glucose_t12", "HOMA_t12"
	};
	
	String[] diets = new String[] {
			"HF", "LF"
	};
	
	void start() throws IDMapperException, IOException {
		outDir.mkdirs();
		PreferenceManager.init();
		BioDataSource.init();
		
		SimpleGex data = new SimpleGex("" + gexFile, false, new DataDerby());
		IDMapperRdb idMapper = SimpleGdbFactory.createInstance("" + gdbFile, new DataDerby(), 0);
		
		StatisticsResult[][] resultAll = new StatisticsResult[diets.length][measurements.length];
		
		for(int d = 0; d < diets.length; d++) {
			String diet = diets[d];
			for(int j = 0; j < measurements.length; j++) {
				String m = measurements[j];
				String expr = "[" + diet + "_" + m + "] < 0.05";
				resultAll[d][j] = StatResultsUtil.calculateZscores(expr, pwDir, data, idMapper);
				resultAll[d][j].save(new File(outDir, "zscores_detail_" + diet + "_" + m + ".txt"));
			}
		}
		
		//Write to txt files
		int iHF = 0;
		int iLF = 1;
		//File for HF and LF comparing each measurement
		StatResultsUtil.write(new StatisticsResult[] {
				resultAll[iHF][0], 	resultAll[iHF][1], resultAll[iHF][2]
		}, paste("t0_vs_", measurements, ""), new File(outDir, "zscores_HF_z2.txt"),
		new FilterZScoreOptions().threshold(2));
		StatResultsUtil.write(new StatisticsResult[] {
				resultAll[iLF][0], 	resultAll[iLF][1], resultAll[iLF][2]
		}, paste("t0_vs_", measurements, ""), new File(outDir, "zscores_LF_z2.txt"), 
		new FilterZScoreOptions().threshold(2));
	}
	
	String[] paste(String before, String[] data, String after) {
		String[] pasted = new String[data.length];
		for(int i = 0; i < data.length; i++) pasted[i] = before + data[i] + after;
		return pasted;
	}
}
