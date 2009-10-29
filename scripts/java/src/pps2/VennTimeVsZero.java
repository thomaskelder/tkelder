package pps2;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.DataDerby;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.go.GOAnnotations;
import org.pathvisio.go.GOReader;
import org.pathvisio.go.GOTerm;
import org.pathvisio.go.GOTree;
import org.pathvisio.go.XrefAnnotation;
import org.pathvisio.go.gui.GOInfoPanel;
import org.pathvisio.go.venn.GoVenn;
import org.pathvisio.model.Pathway;
import org.pathvisio.preferences.PreferenceManager;
import org.pathvisio.utils.GexUtil;
import org.pathvisio.visualization.colorset.Criterion;
import org.pathvisio.visualization.colorset.Criterion.CriterionException;

import pathvisio.venn.GexVennData;
import pps2.PathwayCategories.Category;
import venn.BallVennDiagram;
import venn.GradientVennDiagram;
import venn.RelativeVennData;
import venn.VennData;
import venn.ZScoreVennData;

public class VennTimeVsZero {

	public static void main(String[] args) {
		PreferenceManager.init();
		BioDataSource.init();

		try {
			VennTimeVsZero vd = new VennTimeVsZero();
			//vd.timeVsZero();
			//vd.go();
			vd.goInflammation();
			
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	File outPath = new File("/home/thomas/projects/pps2/stat_results/bigcat/TimeVsZero");
	File gexFile = new File("/home/thomas/projects/pps2/stat_results/PPS2_timecourse_pathvisio.pgex");
	File goOutPath = new File("/home/thomas/projects/pps2/path_results/bigcat/TimeVsZero/venn/protein");
	String[] timePoints = new String[] {
			"t0.6", "t2", "t48"
	};

	SimpleGex gex;

	public VennTimeVsZero() throws IDMapperException {
		outPath.mkdirs();
		goOutPath.mkdirs();
		gex = new SimpleGex("" + gexFile, false, new DataDerby());
	}

	void timeVsZero() throws IDMapperException, CriterionException, MalformedURLException, IOException {
		double q = 0.05;

		//HF vs LF
		for(String time : timePoints) {
			String critLF = "[LF_limma_t0_vs_" + time + "_qvalue] < " + q;
			String critHF = "[HF_limma_t0_vs_" + time + "_qvalue] < " + q;
			VennData<Xref> vdata = GexVennData.create(gex, critLF, critHF);
			BallVennDiagram venn = new BallVennDiagram(vdata);
			venn.setLabels("LF", "HF");
			venn.setTitle(time + " vs t0");
			venn.saveImage(new File(outPath, "venn_t0_vs_" + time + ".png"), "png");
		}
		//consecutive times for HF and LF
		for(String diet : new String[] { "HF", "LF"}) {
			String[] criteria = new String[timePoints.length ];

			for(int i = 0; i < timePoints.length; i++) {
				String t1 = timePoints[i];
				String c = "[" + diet + "_limma_t0_vs_" + t1 + "_qvalue] < " + q;
				criteria[i] = c;
			}

			VennData<Xref> vdata = GexVennData.create(gex, criteria);
			BallVennDiagram venn = new BallVennDiagram(vdata);
			venn.setLabels(timePoints);
			venn.setTitle(diet + " t0 vs time");
			venn.saveImage(new File(outPath, "venn_" + diet + "_t0_vs_time.png"), "png");
		}
	}

	void go() throws IOException, IDMapperException, CriterionException {
		GOTree tree = GOReader.readGOTree(ConstantsPPS2.goOboFile);
		final Set<DataSource> dsTarget = new HashSet<DataSource>();
		dsTarget.add(BioDataSource.ENTREZ_GENE);

		GOAnnotations<XrefAnnotation> geneAnnot = ConstantsPPS2.getGOAnnotations(tree);
		GOTerm[] terms = ConstantsPPS2.getGOTerms(tree);
		String[] termLabels = ConstantsPPS2.getGOTermLabels(terms);

		double q = 0.05;

		//Calculate the venn sets and find max counts
		int maxSetCount = 0;
		Map<String, List<Set<Xref>>> goSetsByLabel = new HashMap<String, List<Set<Xref>>>();
		Map<String, List<Set<Xref>>> goSetsAllByLabel = new HashMap<String, List<Set<Xref>>>();

		for(String diet : new String[] { "LF", "HF" }) {
			for(String t : timePoints) {
				String c = "[" + diet + "_limma_t0_vs_" + t + "_qvalue] < " + q;
				List<Set<Xref>> goSets = GoVenn.getAnnotatedSetsByCriterion(gex, c, tree, geneAnnot, terms);
				List<Set<Xref>> goSetsAll = GoVenn.getAnnotatedSetsByCriterion(gex, "1<2", tree, geneAnnot, terms);
				goSetsByLabel.put(c, goSets);
				goSetsAllByLabel.put(c, goSetsAll);
				maxSetCount = findMax(goSets);
			}
		}

		Color gradientColor = new Color(100, 100, 255);

		//Draw the venn diagrams
		for(String diet : new String[] { "LF", "HF" }) {
			for(String t : timePoints) {
				String tfile = t;
				if(t.equals("t0")) tfile = "t0.0"; //For proper sorting in ubuntu
				String c = "[" + diet + "_limma_t0_vs_" + t + "_qvalue] < " + q;
				String title = "Genes with q<0.05 for " + t + "vst0 in " + diet;
				List<Set<Xref>> goSets = goSetsByLabel.get(c);
				List<Set<Xref>> goSetsAll = goSetsAllByLabel.get(c);

				//Create a venn diagram from the go sets
				//Absolute counts
				VennData<Xref> vdata = new VennData<Xref>(goSets);
				GradientVennDiagram venn = new GradientVennDiagram(vdata);
				venn.setGradient(maxSetCount, gradientColor);
				venn.setLabels(termLabels);
				venn.setTitle(title);
				venn.saveImage(new File(goOutPath, "venn_" + tfile + "vst0_" + diet + "_GO.png"), "png");

				//Relative
				RelativeVennData<Xref> rvdata = new RelativeVennData<Xref>(goSets, goSetsAll);
				venn = new GradientVennDiagram(rvdata);
				venn.setGradient(100, gradientColor);
				venn.setLabels(termLabels);
				venn.setTitle(title + " (scaled to total gene counts)");
				venn.saveImage(new File(goOutPath, "venn_relative_" + tfile + "vst0_" + diet + "_GO.png"), "png");

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
				venn = new GradientVennDiagram(zvdata);
				venn.setGradient(40, gradientColor);
				venn.setLabels(termLabels);
				venn.setTitle(title + " (scaled to z-score for area)");
				venn.saveImage(new File(goOutPath, "venn_zscore_" + tfile + "vst0_" + diet + "_GO.png"), "png");
			}
		}
	}

	void goInflammation() throws IOException, IDMapperException, CriterionException {
		GOTree tree = GOReader.readGOTree(ConstantsPPS2.goOboFile);
		final Set<DataSource> dsTarget = new HashSet<DataSource>();
		dsTarget.add(BioDataSource.ENTREZ_GENE);

		GOTerm term = tree.getTerm("GO:0006954");
		GOAnnotations<XrefAnnotation> geneAnnot = ConstantsPPS2.getGOAnnotations(tree);

		Map<String, List<Set<Xref>>> setsByTime = new HashMap<String, List<Set<Xref>>>();

		double q = 0.05;

		//Calculate the venn sets and find max counts
		for(String t : timePoints) {
			String cHF = "[HF_limma_t0_vs_" + t + "_qvalue] < " + q;
			String cLF = "[LF_limma_t0_vs_" + t + "_qvalue] < " + q;
			List<Set<Xref>> timeSets = new ArrayList<Set<Xref>>();

			timeSets.add(GoVenn.getAnnotatedSetsByCriterion(gex, cHF, tree, geneAnnot, term).get(0));
			timeSets.add(GoVenn.getAnnotatedSetsByCriterion(gex, cLF, tree, geneAnnot, term).get(0));

			setsByTime.put(t, timeSets);
		}

		for(int t = 0; t < timePoints.length; t++) {
			String time = timePoints[t];
			List<Set<Xref>> sList = setsByTime.get(time);

			VennData<Xref> vdata = new VennData<Xref>(sList);
			GradientVennDiagram venn = new GradientVennDiagram(vdata);
			venn.setGradient(50, Color.BLUE);
			venn.setLabels("HF", "LF");
			venn.setTitle("Genes in " + term.getName() + " pathways with " + time + " vs t0, q < 0.05");
			String tfile = time.equals("t0") ? "t0.0" : time;
			venn.saveImage(new File(outPath, "TimeVsZero_" + term.getName() + "_" + tfile + ".png"), "png");
		}
	}
	
	
	int findMax(List<Set<Xref>> sets) {
		int max = Integer.MIN_VALUE;
		for(Set<?> set : sets) {
			max = Math.max(max, set.size());
		}
		return max;
	}
}