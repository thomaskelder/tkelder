package pps2;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.DataDerby;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.preferences.PreferenceManager;
import org.pathvisio.visualization.colorset.Criterion.CriterionException;

import pathvisio.venn.GexVennData;
import venn.BallVennDiagram;
import venn.VennData;

public class VennTimeVsZero {

	public static void main(String[] args) {
		PreferenceManager.init();
		BioDataSource.init();

		try {
			VennTimeVsZero vd = new VennTimeVsZero();
			vd.timeVsZero();
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	File outPath = new File("/home/thomas/projects/pps2/stat_results/bigcat/TimeVsZero");
	File gexFile = new File("/home/thomas/projects/pps2/stat_results/PPS2_timecourse_pathvisio.pgex");

	String[] timePoints = new String[] {
			"t0.6", "t2", "t48"
	};

	SimpleGex gex;

	public VennTimeVsZero() throws IDMapperException {
		outPath.mkdirs();
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
}