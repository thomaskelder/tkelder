package pps2;

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
import venn.VennData;
import venn.ZScoreVennData;

public class VennCorrelation {

	public static void main(String[] args) {
		PreferenceManager.init();
		BioDataSource.init();

		try {
			VennCorrelation vd = new VennCorrelation();
			vd.genes();
			vd.pathways();
			vd.go();
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	File outPath = new File("/home/thomas/projects/pps2/stat_results/bigcat/correlation/venn");
	File pathOutPath = new File("/home/thomas/projects/pps2/path_results/bigcat/correlation/venn");
	File gexFile = new File("/home/thomas/projects/pps2/stat_results/bigcat/correlation/corr_t0_p_t12.pgex");

	File oboFile = new File("/home/thomas/projects/pps2/path_results/bigcat/go-venn/data/gene_ontology_edit.obo");
	File annotFile = new File("/home/thomas/projects/pps2/path_results/bigcat/go-venn/data/mart_mm_proc.txt");

	
	String[] measurements = new String[] {
			"Insulin", "Glucose", "HOMA"
	};
	
	SimpleGex gex;
	double p = 0.05;

	IDMapperRdb idMapper;
	
	public VennCorrelation() throws IDMapperException {
		outPath.mkdirs();
		pathOutPath.mkdirs();
		gex = new SimpleGex("" + gexFile, false, new DataDerby());
		idMapper = ConstantsPPS2.getIdMapper();
	}

	void genes() throws IDMapperException, CriterionException, MalformedURLException, IOException {

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
				StatsUtil.parseZScoreResults(
						new File(zscorePath, "zscores_detail_" + diet + "_Insulin_t12.txt"), 
						Column.ZSCORE
			);
			Map<String, Double> zGlucose = 
				StatsUtil.parseZScoreResults(
						new File(zscorePath, "zscores_detail_" + diet + "_Glucose_t12.txt"), 
						Column.ZSCORE
			);
			Map<String, Double> zHOMA = 
				StatsUtil.parseZScoreResults(
						new File(zscorePath, "zscores_detail_" + diet + "_HOMA_t12.txt"), 
						Column.ZSCORE
			);
			VennData<String> vdata = PathwayVennData.create(2, zInsulin, zGlucose, zHOMA);
			BallVennDiagram venn = new BallVennDiagram(vdata);
			venn.setLabels("Insulin", "Glucose", "HOMA");
			String title = "Pathways (z>=2) in " + diet + " with correlation p <= 0.05";
			venn.setTitle(title);
			venn.saveImage(new File(pathOutPath, "venn_corr_" + diet + "_pathways.png"), "png");
		}
	}
	
	void go() throws IOException, IDMapperException, CriterionException {
		GOTree tree = GOReader.readGOTree(oboFile);
		final DataSource ds = BioDataSource.ENSEMBL_MOUSE;
		final Set<DataSource> dsTarget = new HashSet<DataSource>();
		dsTarget.add(BioDataSource.ENTREZ_GENE);

		GOAnnotations<XrefAnnotation> geneAnnot = ConstantsPPS2.getGOAnnotations(tree);
		GOTerm[] terms = ConstantsPPS2.getGOTerms(tree);
		String[] termLabels = ConstantsPPS2.getGOTermLabels(terms);
		
		for(String diet : new String[] { "HF", "LF"}) {
			for(int i = 0; i < measurements.length; i++) {
				String title = "Genes in " + diet + " with correlation to " + measurements[i] + ", p <= " + p;
				
				String c  = "[" + diet + "_" + measurements[i] + "_t12] <= " + p;
				List<Set<Xref>> goSets = GoVenn.getAnnotatedSetsByCriterion(gex, c, tree, geneAnnot, terms);
				List<Set<Xref>> goSetsAll = GoVenn.getAnnotatedSetsByCriterion(gex, "1<2", tree, geneAnnot, terms);
				//Create a venn diagram from the go sets
				VennData<Xref> vdataRel = new VennData<Xref>(goSets);
				BallVennDiagram venn = new BallVennDiagram(vdataRel);
				venn.setLabels(termLabels);
				venn.setTitle(title);
				venn.saveImage(new File(pathOutPath, "venn_GO_" + measurements[i] + "_" + diet + ".png"), "png");
				
				int bigN = 0;
				int bigR = 0;
				Criterion crit = new Criterion();
				crit.setExpression(c, gex.getSampleNames());
				for(int r = 0; r < gex.getNrRow(); r++) {
					Map<String, Object> sdata = gex.getRow(r).getByName();
					if(crit.evaluate(sdata)) bigR++;
					bigN++;
				}
				
				ZScoreVennData<Xref> vdataZ = new ZScoreVennData<Xref>(goSets, goSetsAll, bigN, bigR);
				venn = new BallVennDiagram(vdataZ);
				venn.setLabels(termLabels);
				venn.setTitle(title + " (scaled to z-score for area)");
				venn.saveImage(new File(pathOutPath, "venn_GO_zscore_" + measurements[i] + "_" + diet + ".png"), "png");
				
			}
			
		}
	}
}