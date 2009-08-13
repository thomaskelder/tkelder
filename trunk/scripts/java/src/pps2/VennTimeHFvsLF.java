package pps2;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.DataDerby;
import org.bridgedb.rdb.IDMapperRdb;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.go.GOAnnotations;
import org.pathvisio.go.GOReader;
import org.pathvisio.go.GOTerm;
import org.pathvisio.go.GOTree;
import org.pathvisio.go.XrefAnnotation;
import org.pathvisio.go.venn.GoVenn;
import org.pathvisio.plugins.statistics.Column;
import org.pathvisio.preferences.PreferenceManager;
import org.pathvisio.utils.StatsUtil;
import org.pathvisio.visualization.colorset.Criterion;
import org.pathvisio.visualization.colorset.Criterion.CriterionException;

import pathvisio.venn.GexVennData;
import pathvisio.venn.PathwayVennData;
import venn.BallVennDiagram;
import venn.NumberVennDiagram;
import venn.RelativeVennData;
import venn.VennData;
import venn.ZScoreVennData;

public class VennTimeHFvsLF {

	public static void main(String[] args) {
		PreferenceManager.init();
		BioDataSource.init();
		
		try {
			VennTimeHFvsLF vd = new VennTimeHFvsLF();
			vd.genes();
			//vd.go();
			//vd.pathway();
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
	
	File outPath = new File("/home/thomas/projects/pps2/stat_results/bigcat/HFvsLF");
	File pathOutPath = new File("/home/thomas/projects/pps2/path_results/bigcat/HFvsLF/venn");
	File goOutPath = pathOutPath;
	
	File gexFile = new File("/home/thomas/projects/pps2/stat_results/PPS2_average 2logratio_HFvsLF per tp.pgex");
	
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
	IDMapperRdb idMapper;
	
	public VennTimeHFvsLF() throws IDMapperException {
		gex = new SimpleGex("" + gexFile, false, new DataDerby());
		idMapper = ConstantsPPS2.getIdMapper();
		outPath.mkdirs();
		pathOutPath.mkdirs();
	}

	void genes() throws IDMapperException, CriterionException, MalformedURLException, IOException {
		String title = "Genes with q < 0.05 for HF vs LF";
		
		double q = 0.05;
		String c0 = "[qvalue_HFvsLF_t0] < " + q;
		String c1 = "[qvalue_HFvsLF_t0.6] < " + q;
		String c2 = "[qvalue_HFvsLF_t2] < " + q;
		String c3 = "[qvalue_HFvsLF_t48] < " + q;
		
		VennData<Xref> vdata = GexVennData.create(gex, c0, c1, c2);
		NumberVennDiagram venn = new NumberVennDiagram(vdata);
//		venn.setBallColor(vdata.getUnionIndex(0), colors[0]);
//		venn.setBallColor(vdata.getUnionIndex(1), colors[1]);
//		venn.setBallColor(vdata.getUnionIndex(2), colors[2]);
		venn.setLabels(labels[0], labels[1], labels[2]);
		venn.setTitle(title);
		venn.saveImage(new File(outPath, "venn_HFvsLF_t0_0.6_2.png"), "png");
		
		vdata = GexVennData.create(gex, c1, c2, c3);
		venn = new NumberVennDiagram(vdata);
//		venn.setBallColor(vdata.getUnionIndex(0), colors[1]);
//		venn.setBallColor(vdata.getUnionIndex(1), colors[2]);
//		venn.setBallColor(vdata.getUnionIndex(2), colors[3]);
		venn.setLabels(labels[1], labels[2], labels[3]);
		venn.setTitle(title);
		venn.saveImage(new File(outPath, "venn_HFvsLF_t0.6_2_48.png"), "png");
		
		vdata = GexVennData.create(gex, c0, c2, c3);
		venn = new NumberVennDiagram(vdata);
//		venn.setBallColor(vdata.getUnionIndex(0), colors[0]);
//		venn.setBallColor(vdata.getUnionIndex(1), colors[2]);
//		venn.setBallColor(vdata.getUnionIndex(2), colors[3]);
		venn.setLabels(labels[0], labels[2], labels[3]);
		venn.setTitle(title);
		venn.saveImage(new File(outPath, "venn_HFvsLF_t0_2_48.png"), "png");
	}
	
	void go() throws IOException, IDMapperException, CriterionException {
		GOTree tree = GOReader.readGOTree(ConstantsPPS2.goOboFile);
		final DataSource ds = BioDataSource.ENSEMBL_MOUSE;
		final Set<DataSource> dsTarget = new HashSet<DataSource>();
		dsTarget.add(BioDataSource.ENTREZ_GENE);
		
		GOAnnotations<XrefAnnotation> geneAnnot = ConstantsPPS2.getGOAnnotations(tree);
		GOTerm[] terms = ConstantsPPS2.getGOTerms(tree);
		String[] termLabels = ConstantsPPS2.getGOTermLabels(terms);
		
		double q = 0.05;
		for(String t : labels) {
			String title = "Genes with q < 0.05 for HF vs LF at " + t;
			String c = "[qvalue_HFvsLF_" + t + "] < " + q;
			List<Set<Xref>> goSets = GoVenn.getAnnotatedSetsByCriterion(gex, c, tree, geneAnnot, terms);
			List<Set<Xref>> goSetsAll = GoVenn.getAnnotatedSetsByCriterion(gex, "1<2", tree, geneAnnot, terms);
			
			//Create a venn diagram from the go sets
			//Absolute counts
			VennData<Xref> vdata = new VennData<Xref>(goSets);
			BallVennDiagram venn = new BallVennDiagram(vdata);
			venn.setLabels(termLabels);
			venn.setTitle(title);
			venn.saveImage(new File(goOutPath, "venn_HFvsLF_" + t + "_GO.png"), "png");
			
			//Relative
			RelativeVennData<Xref> rvdata = new RelativeVennData<Xref>(goSets, goSetsAll);
			venn = new BallVennDiagram(rvdata);
			venn.setLabels(termLabels);
			venn.setTitle(title + " (scaled to total gene counts)");
			venn.saveImage(new File(goOutPath, "venn_HFvsLF_" + t + "_GO_relative.png"), "png");
			
			//Z-score
			int bigN = 0;
			int bigR = 0;
			Criterion crit = new Criterion();
			crit.setExpression(c, gex.getSampleNames());
			for(int i = 0; i < gex.getNrRow(); i++) {
				Map<String, Object> sdata = gex.getRow(i).getByName();
				if(crit.evaluate(sdata)) bigR++;
				bigN++;
			}
			ZScoreVennData<Xref> zvdata = new ZScoreVennData<Xref>(goSets, goSetsAll, bigN, bigR);
			venn = new BallVennDiagram(zvdata);
			venn.setLabels(termLabels);
			venn.setTitle(title + " (scaled to z-score for area)");
			venn.saveImage(new File(goOutPath, "venn_HFvsLF_" + t + "_GO_zscore.png"), "png");
		}
	}
	
	void pathway() throws IOException, IDMapperException, CriterionException {
		File zscorePath = new File("/home/thomas/projects/pps2/path_results/bigcat/HFvsLF/zscores");
		Map<String, Double> z0 = StatsUtil.parseZScoreResults(new File(zscorePath, "zscores_detail_t0_all.txt"), Column.ZSCORE);
		Map<String, Double> z1 = StatsUtil.parseZScoreResults(new File(zscorePath, "zscores_detail_t0.6_all.txt"), Column.ZSCORE);
		Map<String, Double> z2 = StatsUtil.parseZScoreResults(new File(zscorePath, "zscores_detail_t2_all.txt"), Column.ZSCORE);
		Map<String, Double> z3 = StatsUtil.parseZScoreResults(new File(zscorePath, "zscores_detail_t48_all.txt"), Column.ZSCORE);
		
		String title = "Pathways with z >= 2 (q < 0.05)";

		VennData<String> vdata = PathwayVennData.create(2, z0, z1, z2);
		BallVennDiagram venn = new BallVennDiagram(vdata);
		venn.setBallColor(vdata.getUnionIndex(0), colors[0]);
		venn.setBallColor(vdata.getUnionIndex(1), colors[1]);
		venn.setBallColor(vdata.getUnionIndex(2), colors[2]);
		venn.setLabels(labels[0], labels[1], labels[2]);
		venn.setTitle(title);
		venn.saveImage(new File(pathOutPath, "venn_HFvsLF_t0_0.6_2_pathway.png"), "png");
		
		vdata = PathwayVennData.create(2, z1, z2, z3);
		venn = new BallVennDiagram(vdata);
		venn.setBallColor(vdata.getUnionIndex(0), colors[1]);
		venn.setBallColor(vdata.getUnionIndex(1), colors[2]);
		venn.setBallColor(vdata.getUnionIndex(2), colors[3]);
		venn.setLabels(labels[1], labels[2], labels[3]);
		venn.setTitle(title);
		venn.saveImage(new File(pathOutPath, "venn_HFvsLF_t0.6_2_48_pathway.png"), "png");
		
		vdata = PathwayVennData.create(2, z0, z2, z3);
		venn = new BallVennDiagram(vdata);
		venn.setBallColor(vdata.getUnionIndex(0), colors[0]);
		venn.setBallColor(vdata.getUnionIndex(1), colors[2]);
		venn.setBallColor(vdata.getUnionIndex(2), colors[3]);
		venn.setLabels(labels[0], labels[2], labels[3]);
		venn.setTitle(title);
		venn.saveImage(new File(pathOutPath, "venn_HFvsLF_t0_2_48_pathway.png"), "png");
	}
}
