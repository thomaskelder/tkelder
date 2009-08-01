package pps2;

import gcharts.GChartsGexVenn;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

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

public class VennTimeHFvsLF {

	public static void main(String[] args) {
		PreferenceManager.init();
		BioDataSource.init();
		
		try {
			VennTimeHFvsLF vd = new VennTimeHFvsLF();
			vd.HFvsLF();
			vd.HFvsLF_pathway();
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
	
	File outPath = new File("/home/thomas/projects/pps2/stat_results/bigcat/HFvsLF");
	File gexFile = new File("/home/thomas/projects/pps2/stat_results/pps2_HFvsLF_alltimes.pgex");

	String[] labels = new String[] {
			"t0", "t0.6", "t2", "t48"
	};
	
	Color[] colors = new Color[] {
			new Color(54, 211, 255, 200),
			new Color(174, 93, 178, 200),
			new Color(102, 204, 0, 200),
			new Color(182, 1, 0, 200),
	};

	SimpleGex gex;

	public VennTimeHFvsLF() throws IDMapperException {
		gex = new SimpleGex("" + gexFile, false, new DataDerby());
	}

	void HFvsLF() throws IDMapperException, CriterionException, MalformedURLException, IOException {
		String title = "Genes with q <= 0.05 for HF vs LF";
		
		double q = 0.05;
		String c0 = "[q_t0] <= " + q;
		String c1 = "[q_t0.6] <= " + q;
		String c2 = "[q_t2] <= " + q;
		String c3 = "[q_t48] <= " + q;
		
		VennData<Xref> vdata = GexVennData.create(gex, c0, c1, c2);
		BallVennDiagram venn = new BallVennDiagram(vdata);
		venn.setBallColor(vdata.getUnionIndex(0), colors[0]);
		venn.setBallColor(vdata.getUnionIndex(1), colors[1]);
		venn.setBallColor(vdata.getUnionIndex(2), colors[2]);
		venn.setLabels(labels[0], labels[1], labels[2]);
		venn.setTitle(title);
		venn.saveImage(new File(outPath, "venn_HFvsLF_t0_0.6_2.png"), "png");
		
		vdata = GexVennData.create(gex, c1, c2, c3);
		venn = new BallVennDiagram(vdata);
		venn.setBallColor(vdata.getUnionIndex(0), colors[1]);
		venn.setBallColor(vdata.getUnionIndex(1), colors[2]);
		venn.setBallColor(vdata.getUnionIndex(2), colors[3]);
		venn.setLabels(labels[1], labels[2], labels[3]);
		venn.setTitle(title);
		venn.saveImage(new File(outPath, "venn_HFvsLF_t0.6_2_48.png"), "png");
		
		vdata = GexVennData.create(gex, c0, c2, c3);
		venn = new BallVennDiagram(vdata);
		venn.setBallColor(vdata.getUnionIndex(0), colors[0]);
		venn.setBallColor(vdata.getUnionIndex(1), colors[2]);
		venn.setBallColor(vdata.getUnionIndex(2), colors[3]);
		venn.setLabels(labels[0], labels[2], labels[3]);
		venn.setTitle(title);
		venn.saveImage(new File(outPath, "venn_HFvsLF_t0_2_48.png"), "png");
	}
	
	void HFvsLF_pathway() throws IOException, IDMapperException, CriterionException {
		File zscorePath = new File("/home/thomas/projects/pps2/path_results/bigcat/zscore-LFvsHF-time");
		Map<String, Double> z0 = StatResultsUtil.parseZScoreResults(new File(zscorePath, "zscores_detail_t0_all.txt"), Column.ZSCORE);
		Map<String, Double> z1 = StatResultsUtil.parseZScoreResults(new File(zscorePath, "zscores_detail_t0.6_all.txt"), Column.ZSCORE);
		Map<String, Double> z2 = StatResultsUtil.parseZScoreResults(new File(zscorePath, "zscores_detail_t2_all.txt"), Column.ZSCORE);
		Map<String, Double> z3 = StatResultsUtil.parseZScoreResults(new File(zscorePath, "zscores_detail_t48_all.txt"), Column.ZSCORE);
		
		String title = "Pathways with z >= 2 (q <= 0.05)";

		VennData<String> vdata = ZScoreVennData.create(2, z0, z1, z2);
		BallVennDiagram venn = new BallVennDiagram(vdata);
		venn.setBallColor(vdata.getUnionIndex(0), colors[0]);
		venn.setBallColor(vdata.getUnionIndex(1), colors[1]);
		venn.setBallColor(vdata.getUnionIndex(2), colors[2]);
		venn.setLabels(labels[0], labels[1], labels[2]);
		venn.setTitle(title);
		venn.saveImage(new File(outPath, "venn_HFvsLF_t0_0.6_2_pathway.png"), "png");
		
		vdata = ZScoreVennData.create(2, z1, z2, z3);
		venn = new BallVennDiagram(vdata);
		venn.setBallColor(vdata.getUnionIndex(0), colors[1]);
		venn.setBallColor(vdata.getUnionIndex(1), colors[2]);
		venn.setBallColor(vdata.getUnionIndex(2), colors[3]);
		venn.setLabels(labels[1], labels[2], labels[3]);
		venn.setTitle(title);
		venn.saveImage(new File(outPath, "venn_HFvsLF_t0.6_2_48_pathway.png"), "png");
		
		vdata = ZScoreVennData.create(2, z0, z2, z3);
		venn = new BallVennDiagram(vdata);
		venn.setBallColor(vdata.getUnionIndex(0), colors[0]);
		venn.setBallColor(vdata.getUnionIndex(1), colors[2]);
		venn.setBallColor(vdata.getUnionIndex(2), colors[3]);
		venn.setLabels(labels[0], labels[2], labels[3]);
		venn.setTitle(title);
		venn.saveImage(new File(outPath, "venn_HFvsLF_t0_2_48_pathway.png"), "png");
	}
}
