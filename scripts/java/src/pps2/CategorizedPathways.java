package pps2;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.bridgedb.AttributeMapper;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.DataDerby;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.model.ConverterException;
import org.pathvisio.model.Pathway;
import org.pathvisio.plugins.statistics.StatisticsPathwayResult;
import org.pathvisio.plugins.statistics.StatisticsResult;
import org.pathvisio.preferences.PreferenceManager;
import org.pathvisio.utils.GexUtil;
import org.pathvisio.utils.StatsUtil;
import org.pathvisio.visualization.colorset.Criterion.CriterionException;

import pps2.PathwayCategories.Category;
import venn.GradientVennDiagram;
import venn.RelativeVennData;
import venn.VennData;
import venn.VennData.DoSomething;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class CategorizedPathways {
	public static void main(String[] args) {
		try {
			CategorizedPathways main = new CategorizedPathways();
			//main.venn();
			main.vennInflammation();
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	File hfVsLfDataFile = new File(
			"/home/thomas/projects/pps2/stat_results/PPS2_average 2logratio_HFvsLF per tp.pgex"
	);

	File timeVsZeroDataFile = new File(
			"/home/thomas/projects/pps2/stat_results/PPS2_timecourse_pathvisio.pgex"
	);

	SimpleGex hfVsLfData;
	SimpleGex timeVsZeroData;

	static String[] timePointsHFvsLF = new String[] {
		"t0", "t0.6", "t2", "t48"
	};

	static String[] timePointsTimeVsZero = new String[] {
		"t0.6", "t2", "t48"
	};

	static String[] diets = new String[] { "HF", "LF" };

	StatisticsResult[] hfVsLfAll = new StatisticsResult[timePointsHFvsLF.length];
	StatisticsResult[] hfVsLfDown = new StatisticsResult[timePointsHFvsLF.length];
	StatisticsResult[] hfVsLfUp = new StatisticsResult[timePointsHFvsLF.length];

	StatisticsResult[][] timeVsZeroAll = new StatisticsResult[diets.length][timePointsTimeVsZero.length];
	StatisticsResult[][] timeVsZeroDown = new StatisticsResult[diets.length][timePointsTimeVsZero.length];
	StatisticsResult[][] timeVsZeroUp = new StatisticsResult[diets.length][timePointsTimeVsZero.length];

	private File gexFile = new File(
			"/home/thomas/projects/pps2/stat_results/PPS2_average 2logratio_HFvsLF per tp.pgex"
	);

	IDMapper idMapper = ConstantsPPS2.getIdMapper();

	File categoryFile = new File("/home/thomas/projects/pps2/path_results/bigcat/categories/Pathways_catergorizatio_GO.txt");

	PathwayCategories categories;

	public CategorizedPathways() throws IDMapperException, IOException {
		PreferenceManager.init();
		BioDataSource.init();

		categories = PathwayCategories.parse(categoryFile, ConstantsPPS2.pathwayDir);

		hfVsLfData = new SimpleGex("" + hfVsLfDataFile, false, new DataDerby());
		timeVsZeroData = new SimpleGex("" + timeVsZeroDataFile, false, new DataDerby());

		//stats();
	}

	void stats() throws IDMapperException {
		for(int i = 0; i < timePointsHFvsLF.length; i++) {
			String time = timePointsHFvsLF[i];
			String exprAll = "[qvalue_HFvsLF_" + time + "] < 0.05";
			String exprDown = "[qvalue_HFvsLF_" + time + "] < 0.05 AND [logratio_" + time + "_HFvsLF] < 0";
			String exprUp = "[qvalue_HFvsLF_" + time + "] < 0.05 AND [logratio_" + time + "_HFvsLF] > 0";

			hfVsLfAll[i] = StatsUtil.calculateZscores(exprAll, ConstantsPPS2.pathwayDir, hfVsLfData, idMapper);
			hfVsLfDown[i] = StatsUtil.calculateZscores(exprDown, ConstantsPPS2.pathwayDir, hfVsLfData, idMapper);
			hfVsLfUp[i] = StatsUtil.calculateZscores(exprUp, ConstantsPPS2.pathwayDir, hfVsLfData, idMapper);
		}

		for(int d = 0; d < diets.length; d++) {
			String diet = diets[d];
			for(int t = 0; t < timePointsTimeVsZero.length; t++) {
				String time = timePointsTimeVsZero[t];
				String var_q = "[" + diet + "_limma_t0_vs_" + time + "_qvalue]";
				String var_r = "[" + diet + "_log2_" + time + "vst0]";
				String exprAll = var_q + " < 0.05";
				String exprDown = var_q + " < 0.05 AND " + var_r + " < 0";
				String exprUp = var_q + " < 0.05 AND " + var_r + " > 0"; 
				timeVsZeroAll[d][t] = StatsUtil.calculateZscores(exprAll, ConstantsPPS2.pathwayDir, timeVsZeroData, idMapper);
				timeVsZeroDown[d][t] = StatsUtil.calculateZscores(exprDown, ConstantsPPS2.pathwayDir, timeVsZeroData, idMapper);
				timeVsZeroUp[d][t] = StatsUtil.calculateZscores(exprUp, ConstantsPPS2.pathwayDir, timeVsZeroData, idMapper);
			}
		}
	}

	File vennOut = new File("/home/thomas/projects/pps2/path_results/bigcat/categories/venn");

	public void venn() throws ConverterException, IDMapperException, CriterionException, IOException {
		//HFvsLF
		//For each timepoint create a venn diagram with the three categories
		for(int i = 0; i < timePointsHFvsLF.length; i++) {
			Multimap<Category, Xref> sigByCat = new HashMultimap<Category, Xref>();
			Multimap<Category, Xref> allByCat = new HashMultimap<Category, Xref>();

			String time = timePointsHFvsLF[i];

			Set<Xref> measured = GexUtil.listXrefs(
					hfVsLfData
			);
			Set<Xref> significant = GexUtil.extractSignificant(
					hfVsLfData, "[qvalue_HFvsLF_" + time + "] < 0.05"
			);

			for(File f : categories.getCategorizedPathways()) {
				Pathway p = new Pathway();
				p.readFromXml(f, true);
				for(Xref x : p.getDataNodeXrefs()) {
					for(Xref xx : idMapper.mapID(x, BioDataSource.ENTREZ_GENE)) {
						if(significant.contains(xx)) {
							sigByCat.put(categories.getCategory(f), xx);
						} if(measured.contains(xx)) {
							allByCat.put(categories.getCategory(f), xx);
						}
					}
				}
			}

			List<Set<Xref>> mSets = new ArrayList<Set<Xref>>();
			mSets.add(new HashSet<Xref>(allByCat.get(Category.INFLAMMATION)));
			mSets.add(new HashSet<Xref>(allByCat.get(Category.METABOLISM)));
			mSets.add(new HashSet<Xref>(allByCat.get(Category.OXIDATIVE_STRESS)));
			List<Set<Xref>> sSets = new ArrayList<Set<Xref>>();
			sSets.add(new HashSet<Xref>(sigByCat.get(Category.INFLAMMATION)));
			sSets.add(new HashSet<Xref>(sigByCat.get(Category.METABOLISM)));
			sSets.add(new HashSet<Xref>(sigByCat.get(Category.OXIDATIVE_STRESS)));

			RelativeVennData<Xref> vdata = new RelativeVennData<Xref>(sSets, mSets);
			GradientVennDiagram venn = new GradientVennDiagram(vdata);
			venn.setGradient(100, Color.BLUE);
			venn.setLabels(Category.INFLAMMATION.getName(), Category.METABOLISM.getName(), Category.OXIDATIVE_STRESS.getName());
			venn.setTitle("Genes per pathway type at " + time + " (measured vs HFvsLF q < 0.05)");
			String tfile = time.equals("t0") ? "t0.0" : time;
			venn.saveImage(new File(vennOut, "HFvsLF_" + tfile + ".png"), "png");

			ConstantsPPS2.saveVennAsExcel(new File(vennOut, "sets_HFvsLF_" + tfile + ".xls"), vdata.getData(), new String[] {
				Category.INFLAMMATION.getName(), Category.METABOLISM.getName(), Category.OXIDATIVE_STRESS.getName() }
			);
			//			vdata.getData().saveUnions(new File(vennOut, "sets_HFvsLF_" + tfile + ".txt"), new String[] {
			//				Category.INFLAMMATION.getName(), Category.METABOLISM.getName(), Category.OXIDATIVE_STRESS.getName() },
			//				new DoSomething<Xref, String>() {
			//					public String doit(Xref obj) {
			//						return getSymbol(obj);
			//					}
			//				});
		}
	}

	/*
	 * Venn diagrams comparing TimeVsZero genes in inflammation pathways only
	 */
	private void vennInflammation() throws IDMapperException, CriterionException, ConverterException, IOException {
		for(int t = 0; t < timePointsTimeVsZero.length; t++) {
			String time = timePointsTimeVsZero[t];
			List<Set<Xref>> sList = new ArrayList<Set<Xref>>();
			sList.add(new HashSet<Xref>());
			sList.add(new HashSet<Xref>());

			Set<Xref> sigHF = GexUtil.extractSignificant(
					timeVsZeroData, "[HF_limma_t0_vs_" + time + "_qvalue] < 0.05"
			);
			Set<Xref> sigLF = GexUtil.extractSignificant(
					timeVsZeroData, "[LF_limma_t0_vs_" + time + "_qvalue] < 0.05"
			);

			//Venn diagram with all genes, for comparison
			List<Set<Xref>> sigList = new ArrayList<Set<Xref>>();
			sigList.add(sigHF);
			sigList.add(sigLF);
			VennData<Xref> vdata = new VennData<Xref>(sigList);
			GradientVennDiagram venn = new GradientVennDiagram(vdata);
			venn.setGradient(1000, Color.BLUE);
			venn.setLabels("HF", "LF");
			venn.setTitle("Genes with " + time + " vs t0, q < 0.05");
			String tfile = time.equals("t0") ? "t0.0" : time;
			venn.saveImage(new File(vennOut, "TimeVsZero_all_" + tfile + ".png"), "png");

			//Venn diagrams per category
			for(Category cat : Category.values()) {
				for(File f : categories.getCategorizedPathways()) {
					if(categories.getCategory(f) != cat) {
						continue;
					}
					Pathway p = new Pathway();
					p.readFromXml(f, false);
					for(Xref x : p.getDataNodeXrefs()) {
						for(Xref xx : idMapper.mapID(x, BioDataSource.ENTREZ_GENE)) {
							if(sigHF.contains(xx)) {
								sList.get(0).add(xx);
							}
							if(sigLF.contains(xx)) {
								sList.get(1).add(xx);
							}
						}
					}
				}
				vdata = new VennData<Xref>(sList);
				venn = new GradientVennDiagram(vdata);
				venn.setGradient(500, Color.BLUE);
				venn.setLabels("HF", "LF");
				venn.setTitle("Genes in " + cat.getName() + " pathways with " + time + " vs t0, q < 0.05");
				tfile = time.equals("t0") ? "t0.0" : time;
				venn.saveImage(new File(vennOut, "TimeVsZero_" + cat.getName() + "_" + tfile + ".png"), "png");

				ConstantsPPS2.saveVennAsExcel(new File(vennOut, "sets_TimeVsZer_" + cat.getName() + "_" + tfile + ".xls"), vdata,
						new String[] { "HF", "LF" }
				);
			}
		}
	}

	double findZscore(StatisticsResult r, File pathway) {
		for(StatisticsPathwayResult pr : r.getPathwayResults()) {
			if(pr.getFile().getName().equals(pathway.getName())) {
				return pr.getZScore();
			}
		}
		return Double.NaN;
	}
}
