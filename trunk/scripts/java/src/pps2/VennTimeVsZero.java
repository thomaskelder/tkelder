package pps2;

import gcharts.GChartsGexVenn;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.imageio.ImageIO;

import org.bridgedb.IDMapperException;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.DataDerby;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.preferences.PreferenceManager;
import org.pathvisio.visualization.colorset.Criterion.CriterionException;

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

	String[] colors = new String[] {
			"FF9955",
			"75A4FB",
			"FFCC33",
			"7FC659",
	};
	String[] timePoints = new String[] {
			"t0.6", "t2", "t18", "t48"
	};

	SimpleGex gex;

	public VennTimeVsZero() throws IDMapperException {
		gex = new SimpleGex("" + gexFile, false, new DataDerby());
	}

	void timeVsZero() throws IDMapperException, CriterionException, MalformedURLException, IOException {
		double q = 0.05;

		//HF vs LF
		for(String time : timePoints) {
			String critLF = "[LF_limma_t0_vs_" + time + "_qvalue] < " + q;
			String critHF = "[HF_limma_t0_vs_" + time + "_qvalue] < " + q;
			GChartsGexVenn gv = new GChartsGexVenn(gex);
			String[] criteria = new String[] { critLF, critHF };
			gv.calculateMatches(criteria);
			gv.setColors(criteria, colors);
			gv.setLabels(criteria, new String[] { "LF t0 vs " + time, "HF t0 vs " + time });
			BufferedImage venn = gv.createDiagram("t0 vs " + time, critLF, critHF, null);
			ImageIO.write(venn, "png", new File(outPath, "venn_t0_vs_" + time + ".png"));
		}
		//consecutive times for HF and LF
		for(String diet : new String[] { "HF", "LF"}) {
			String[] criteria = new String[timePoints.length ];

			for(int i = 0; i < timePoints.length; i++) {
				String t1 = timePoints[i];
				String c = "[" + diet + "_limma_t0_vs_" + t1 + "_qvalue] < " + q;
				criteria[i] = c;
			}
			GChartsGexVenn gv = new GChartsGexVenn(gex);
			gv.calculateMatches(criteria);
			gv.setColors(criteria, colors);
			gv.setLabels(criteria, timePoints);
			
			//Skip t18 for now...
			BufferedImage venn = gv.createDiagram(diet + " t0 vs time", criteria[0], criteria[1], criteria[3]);
			ImageIO.write(venn, "png", new File(outPath, "venn_" + diet + "_t0_vs_time.png"));
		}
	}
}