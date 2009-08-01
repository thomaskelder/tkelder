package pps2;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.DataDerby;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.plugins.statistics.Column;
import org.pathvisio.preferences.PreferenceManager;
import org.pathvisio.utils.StatResultsUtil;
import org.pathvisio.visualization.colorset.Criterion.CriterionException;

import pathvisio.venn.GexVennData;
import pathvisio.venn.ZScoreVennData;
import venn.BallVennDiagram;
import venn.VennData;

public class VennCorrelation {

	public static void main(String[] args) {
		PreferenceManager.init();
		BioDataSource.init();

		try {
			VennCorrelation vd = new VennCorrelation();
			vd.genes();
			vd.pathways();
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	File outPath = new File("/home/thomas/projects/pps2/stat_results/bigcat/correlation/venn");
	File gexFile = new File("/home/thomas/projects/pps2/stat_results/bigcat/correlation/corr_t0_p_t12.pgex");

	String[] measurements = new String[] {
			"Insulin", "Glucose", "HOMA"
	};
	
	SimpleGex gex;

	public VennCorrelation() throws IDMapperException {
		gex = new SimpleGex("" + gexFile, false, new DataDerby());
	}

	void genes() throws IDMapperException, CriterionException, MalformedURLException, IOException {
		double p = 0.05;

		//HF vs LF
		for(String m : measurements) {
			String critLF = "[LF_" + m + "_t12] <= " + p;
			String critHF = "[HF_" + m + "_t12] <= " + p;
			VennData<Xref> vdata = GexVennData.create(gex, critLF, critHF);
			BallVennDiagram venn = new BallVennDiagram(vdata);
			venn.setLabels("LF", "HF");
			String title = "Genes in " + m + " with correlation p <= " + p;
			venn.setTitle(title);
			venn.saveImage(new File(outPath, "venn_corr_" + m + "_HFvsLF.png"), "png");
		}
		
		//Compare all measurements
		for(String diet : new String[] { "HF", "LF"}) {
			String[] criteria = new String[measurements.length ];

			for(int i = 0; i < measurements.length; i++) {
				criteria[i]  = "[" + diet + "_" + measurements[i] + "_t12] <= " + p;
			}
			
			VennData<Xref> vdata = GexVennData.create(gex, criteria);
			BallVennDiagram venn = new BallVennDiagram(vdata);
			String title = "Genes in " + diet + " with correlation p <= " + p + ")";
			venn.setLabels(measurements);
			venn.setTitle(title);
			venn.saveImage(new File(outPath, "venn_corr_" + diet + ".png"), "png");
		}
	}
	
	void pathways() throws IOException, IDMapperException, CriterionException {
		File zscorePath = new File("/home/thomas/projects/pps2/path_results/bigcat/correlation");
		for(String diet : new String[] { "HF", "LF" }) {
			Map<String, Double> zInsulin = 
				StatResultsUtil.parseZScoreResults(
						new File(zscorePath, "zscores_detail_" + diet + "_Insulin_t12.txt"), 
						Column.ZSCORE
			);
			Map<String, Double> zGlucose = 
				StatResultsUtil.parseZScoreResults(
						new File(zscorePath, "zscores_detail_" + diet + "_Glucose_t12.txt"), 
						Column.ZSCORE
			);
			Map<String, Double> zHOMA = 
				StatResultsUtil.parseZScoreResults(
						new File(zscorePath, "zscores_detail_" + diet + "_HOMA_t12.txt"), 
						Column.ZSCORE
			);
			VennData<String> vdata = ZScoreVennData.create(2, zInsulin, zGlucose, zHOMA);
			BallVennDiagram venn = new BallVennDiagram(vdata);
			venn.setLabels("Insulin", "Glucose", "HOMA");
			String title = "Pathways (z>=2) in " + diet + " with correlation p <= 0.05";
			venn.setTitle(title);
			venn.saveImage(new File(outPath, "venn_corr_" + diet + "_pathways.png"), "png");
		}
	}
}