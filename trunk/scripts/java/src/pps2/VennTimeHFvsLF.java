package pps2;

import gcharts.GChartsGexVenn;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
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

public class VennTimeHFvsLF {

	public static void main(String[] args) {
		PreferenceManager.init();
		BioDataSource.init();
		
		try {
			VennTimeHFvsLF vd = new VennTimeHFvsLF();
			//vd.HFvsLF();
			vd.HFvsLF_pathway();
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
	
	File outPath = new File("/home/thomas/projects/pps2/stat_results/bigcat/HFvsLF");
	File gexFile = new File("/home/thomas/projects/pps2/stat_results/pps2_HFvsLF_alltimes.pgex");

	String[] colors = new String[] {
			"FF9955",
			"75A4FB",
			"FFCC33",
			"7FC659",
	};
	String[] labels = new String[] {
			"t0", "t0.6", "t2", "t48"
	};
	
	SimpleGex gex;

	public VennTimeHFvsLF() throws IDMapperException {
		gex = new SimpleGex("" + gexFile, false, new DataDerby());
	}

	void HFvsLF() throws IDMapperException, CriterionException, MalformedURLException, IOException {
		String title = "HF vs LF, q <= 0.05";
		
		double q = 0.05;
		String c1 = "[q_t0] <= " + q;
		String c2 = "[q_t0.6] <= " + q;
		String c3 = "[q_t2] <= " + q;
		String c4 = "[q_t48] <= " + q;
		String[] criteria = new String[] { c1, c2, c3, c4 };
		
		GChartsGexVenn gv = new GChartsGexVenn(gex);
		gv.calculateMatches(criteria);
		gv.setColors(criteria, colors);
		gv.setLabels(criteria, labels);
		
		BufferedImage venn = gv.createDiagram(title, c1, c2, c3);
		ImageIO.write(venn, "png", new File(outPath, "venn_HFvsLF_t0_0.6_2.png"));
		
		venn = gv.createDiagram(title, c2, c3, c4);
		ImageIO.write(venn, "png", new File(outPath, "venn_HFvsLF_t0.6_2_48.png"));
		
		venn = gv.createDiagram(title, c1, c2, c4);
		ImageIO.write(venn, "png", new File(outPath, "venn_HFvsLF_t0_0.6_48.png"));
		
		Writer out = new BufferedWriter(new FileWriter(new File(outPath, "vennMappings.txt")));
		gv.writeMatchTxt(out);
		out.close();
	}
	
	void HFvsLF_pathway() throws IOException, IDMapperException, CriterionException {
		File zscorePath = new File("/home/thomas/projects/pps2/path_results/bigcat/zscore-LFvsHF-time");
		Map<String, Map<String, Double>> zscores = new HashMap<String, Map<String, Double>>();
		zscores.put(labels[0], StatResultsUtil.parseZScoreResults(new File(zscorePath, "zscores_detail_t0_all.txt"), Column.ZSCORE));
		zscores.put(labels[1], StatResultsUtil.parseZScoreResults(new File(zscorePath, "zscores_detail_t0.6_all.txt"), Column.ZSCORE));
		zscores.put(labels[2], StatResultsUtil.parseZScoreResults(new File(zscorePath, "zscores_detail_t2_all.txt"), Column.ZSCORE));
		zscores.put(labels[3], StatResultsUtil.parseZScoreResults(new File(zscorePath, "zscores_detail_t48_all.txt"), Column.ZSCORE));
		
		GChartsGexVenn gv = new GChartsGexVenn(gex);
		gv.calculateZScoreMatches(zscores, 2, Double.MAX_VALUE);
		gv.setColors(labels, colors);
		gv.setLabels(labels, labels);
		
		String title = "Pathways with z >= 2 (q <= 0.05)";
		
		BufferedImage venn = gv.createDiagram(title, labels[0], labels[1], labels[2]);
		ImageIO.write(venn, "png", new File(outPath, "venn_HFvsLF_t0_0.6_2_pathways.png"));
		
		venn = gv.createDiagram(title, labels[1], labels[2], labels[3]);
		ImageIO.write(venn, "png", new File(outPath, "venn_HFvsLF_t0.6_2_48_pathways.png"));
		
		venn = gv.createDiagram(title, labels[0], labels[1], labels[3]);
		ImageIO.write(venn, "png", new File(outPath, "venn_HFvsLF_t0_0.6_48_pathways.png"));
		
//		//P values
//		Map<String, Map<String, Double>> pvals = new HashMap<String, Map<String, Double>>();
//		pvals.put(labels[0], StatResultsUtil.parseZScoreResults(new File(zscorePath, "zscores_detail_t0_all.txt"), Column.PERMPVAL));
//		pvals.put(labels[1], StatResultsUtil.parseZScoreResults(new File(zscorePath, "zscores_detail_t0.6_all.txt"), Column.PERMPVAL));
//		pvals.put(labels[2], StatResultsUtil.parseZScoreResults(new File(zscorePath, "zscores_detail_t2_all.txt"), Column.PERMPVAL));
//		pvals.put(labels[3], StatResultsUtil.parseZScoreResults(new File(zscorePath, "zscores_detail_t48_all.txt"), Column.PERMPVAL));
//		
//		double p = 0.01;
//		gv.calculateZScoreMatches(pvals, 0, p);
//		gv.setColors(labels, colors);
//		gv.setLabels(labels, labels);
//		
//		title = "Pathways with p <= " + p + " (q <= 0.05)";
//		
//		venn = gv.createDiagram(title, labels[0], labels[1], labels[2]);
//		ImageIO.write(venn, "png", new File(outPath, "venn_HFvsLF_t0_0.6_2_pathways.p.png"));
//		
//		venn = gv.createDiagram(title, labels[1], labels[2], labels[3]);
//		ImageIO.write(venn, "png", new File(outPath, "venn_HFvsLF_t0.6_2_48_pathways.p.png"));
//		
//		venn = gv.createDiagram(title, labels[0], labels[1], labels[3]);
//		ImageIO.write(venn, "png", new File(outPath, "venn_HFvsLF_t0_0.6_48_pathways.p.png"));
	}
}
