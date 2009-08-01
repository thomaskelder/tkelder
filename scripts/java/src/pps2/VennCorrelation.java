package pps2;

import gcharts.GChartsGexVenn;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.bridgedb.IDMapperException;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.DataDerby;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.plugins.statistics.Column;
import org.pathvisio.preferences.PreferenceManager;
import org.pathvisio.visualization.colorset.Criterion.CriterionException;

public class VennCorrelation {

	public static void main(String[] args) {
		PreferenceManager.init();
		BioDataSource.init();

		try {
			VennCorrelation vd = new VennCorrelation();
			//vd.genes();
			vd.pathways();
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	File outPath = new File("/home/thomas/projects/pps2/stat_results/bigcat/correlation/venn");
	File gexFile = new File("/home/thomas/projects/pps2/stat_results/bigcat/correlation/corr_t0_p_t12.pgex");

	String[] colors = new String[] {
			"FF9955",
			"75A4FB",
			"FFCC33",
	};
	String[] measurements = new String[] {
			"Insulin_t12", "Glucose_t12", "HOMA_t12"
	};
	
	SimpleGex gex;

	public VennCorrelation() throws IDMapperException {
		gex = new SimpleGex("" + gexFile, false, new DataDerby());
	}

	void genes() throws IDMapperException, CriterionException, MalformedURLException, IOException {
		double p = 0.01;

		//HF vs LF
		for(String m : measurements) {
			String critLF = "[LF_" + m + "] <= " + p;
			String critHF = "[HF_" + m + "] <= " + p;
			GChartsGexVenn gv = new GChartsGexVenn(gex);
			String[] criteria = new String[] { critLF, critHF };
			gv.calculateMatches(criteria);
			gv.setColors(criteria, colors);
			gv.setLabels(criteria, new String[] { "LF " + m, "HF " + m });
			BufferedImage venn = gv.createDiagram(
					"Correlation HF and LF gene expression with " + m + " ( p <= " + p + ")", 
					critLF, critHF, null);
			ImageIO.write(venn, "png", new File(outPath, "venn_" + m + "_HFvsLF.png"));
		}
		
		//Compare all measurements
		for(String diet : new String[] { "HF", "LF"}) {
			String[] criteria = new String[measurements.length ];

			for(int i = 0; i < measurements.length; i++) {
				criteria[i]  = "[" + diet + "_" + measurements[i] + "] <= " + p;
			}
			
			GChartsGexVenn gv = new GChartsGexVenn(gex);
			gv.calculateMatches(criteria);
			gv.setColors(criteria, colors);
			gv.setLabels(criteria, measurements);
			
			BufferedImage venn = gv.createDiagram("Correlation gene expression with HOMA, Insulin and Glucose (p <= " + p + ")", criteria[0], criteria[1], criteria[2]);
			ImageIO.write(venn, "png", new File(outPath, "venn_corr_" + diet + ".png"));
		}
	}
	
	void pathways() throws IOException, IDMapperException, CriterionException {
		File zscorePath = new File("/home/thomas/projects/pps2/path_results/bigcat/correlation");
		for(String diet : new String[] { "HF", "LF" }) {
			Map<String, Map<String, Double>> zscores = new HashMap<String, Map<String, Double>>();
			zscores.put(measurements[0], StatResultsUtil.parseZScoreResults(new File(zscorePath, "zscores_detail_" + diet + "_Insulin_t12.txt"), Column.ZSCORE));
			zscores.put(measurements[1], StatResultsUtil.parseZScoreResults(new File(zscorePath, "zscores_detail_" + diet + "_Glucose_t12.txt"), Column.ZSCORE));
			zscores.put(measurements[2], StatResultsUtil.parseZScoreResults(new File(zscorePath, "zscores_detail_" + diet + "_HOMA_t12.txt"), Column.ZSCORE));
			
			GChartsGexVenn gv = new GChartsGexVenn(gex);
			gv.calculateZScoreMatches(zscores, 2, Double.MAX_VALUE);
			gv.setColors(measurements, colors);
			gv.setLabels(measurements, measurements);
			
			String title = "Pathways with z >= 2 (" + diet + " correlation p <= 0.05)";
			
			BufferedImage venn = gv.createDiagram(title, measurements[0], measurements[1], measurements[2]);
			ImageIO.write(venn, "png", new File(outPath, "venn_corr_" + diet + "_pathways.png"));
		}
	}
}