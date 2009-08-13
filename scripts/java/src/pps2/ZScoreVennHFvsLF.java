package pps2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.DataDerby;
import org.bridgedb.rdb.IDMapperRdb;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.plugins.statistics.StatisticsResult;
import org.pathvisio.preferences.PreferenceManager;
import org.pathvisio.utils.StatsUtil;
import org.pathvisio.utils.StatsUtil.FilterZScoreOptions;
import org.pathvisio.visualization.colorset.Criterion.CriterionException;

import pathvisio.venn.GexVennData;
import venn.VennData;
import venn.VennData.DoSomething;

/**
 * Calculate z-scores for each intersection set of the venn diagram for HF vs LF
 * @author thomas
 */
public class ZScoreVennHFvsLF {
	public static void main(String[] args) {
		try {
			ZScoreVennHFvsLF main = new ZScoreVennHFvsLF();
			main.start();
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
	
	private File gexFile = new File(
			"/home/thomas/projects/pps2/stat_results/PPS2_average 2logratio_HFvsLF per tp.pgex"
	);

	private File pathOutDir = new File(
			"/home/thomas/projects/pps2/path_results/bigcat/HFvsLF/venn/zscores"
	);
	private File statOutDir = new File(
			"/home/thomas/projects/pps2/stat_results/bigcat/HFvsLF/"
	);
	
	String[] timePoints = new String[] {
			"t0", "t0.6", "t2", "t48"
	};
	
	SimpleGex data;
	IDMapperRdb idMapper;
	
	public ZScoreVennHFvsLF() throws IDMapperException {
		pathOutDir.mkdirs();
		statOutDir.mkdirs();
		PreferenceManager.init();
		BioDataSource.init();
		
		data = new SimpleGex("" + gexFile, false, new DataDerby());
		idMapper = ConstantsPPS2.getIdMapper();
	}
	
	void start() throws IDMapperException, IOException, CriterionException {
		double q = 0.05;
		String c0 = "[qvalue_HFvsLF_t0] < " + q;
		String c1 = "[qvalue_HFvsLF_t0.6] < " + q;
		String c2 = "[qvalue_HFvsLF_t2] < " + q;
		String c3 = "[qvalue_HFvsLF_t48] < " + q;
		
		//Early timepoints
		VennData<Xref> vdata = GexVennData.create(data, c0, c1, c2);
		vdata.saveUnions(new File(statOutDir, "venn_genes_HFvsLF_t0_0.6_2.txt"), new String[] {
			timePoints[0], timePoints[1], timePoints[2]
		}, new DoSomething<Xref, String>() {
			public String doit(Xref obj) {
				return obj.getId();
			}
		});
		List<StatisticsResult> results = new ArrayList<StatisticsResult>();
		//Unique for t0
		results.add(StatsUtil.calculateZscoresFromSet(
				vdata.getUnion(vdata.getUnionIndex(0)), 
				ConstantsPPS2.pathwayDir, data, idMapper
		));
		results.add(StatsUtil.calculateZscoresFromSet(
				vdata.getUnion(vdata.getUnionIndex(1)), 
				ConstantsPPS2.pathwayDir, data, idMapper
		));
		results.add(StatsUtil.calculateZscoresFromSet(
				vdata.getUnion(vdata.getUnionIndex(2)), 
				ConstantsPPS2.pathwayDir, data, idMapper
		));
		results.add(StatsUtil.calculateZscoresFromSet(
				vdata.getUnion(vdata.getUnionIndex(0,1)), 
				ConstantsPPS2.pathwayDir, data, idMapper
		));
		results.add(StatsUtil.calculateZscoresFromSet(
				vdata.getUnion(vdata.getUnionIndex(0,2)), 
				ConstantsPPS2.pathwayDir, data, idMapper
		));
		results.add(StatsUtil.calculateZscoresFromSet(
				vdata.getUnion(vdata.getUnionIndex(1,2)), 
				ConstantsPPS2.pathwayDir, data, idMapper
		));
		results.add(StatsUtil.calculateZscoresFromSet(
				vdata.getUnion(vdata.getUnionIndex(0,1,2)), 
				ConstantsPPS2.pathwayDir, data, idMapper
		));
		String[] labels = new String[] {
				timePoints[0], timePoints[1], timePoints[2],
				timePoints[0] + "-" + timePoints[1], timePoints[0] + "-" + timePoints[2], timePoints[1] + "-" + timePoints[2],
				timePoints[0] + "-" + timePoints[1] + "-" + timePoints[2]
		};
		for(int i = 0; i < results.size(); i++) {
			results.get(i).save(new File(pathOutDir, "venn_zscores_HFvsLF_" + labels[i] + ".txt"));
		}
		StatsUtil.write(results.toArray(new StatisticsResult[0]), labels, 
				new File(pathOutDir, "venn_zscores_HFvsLF_combined_t0_0.6_2.txt"),
				new FilterZScoreOptions()
		);
	}
}
