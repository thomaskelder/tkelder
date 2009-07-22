package pps2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bridgedb.DataSource;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.DataDerby;
import org.bridgedb.rdb.IDMapperRdb;
import org.bridgedb.rdb.SimpleGdbFactory;
import org.pathvisio.gex.Sample;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.gsea.GexGSEA;
import org.pathvisio.gsea.GexGSEA.CalcParameters;

import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;

/**
 * Perform GSEA for the pps2 data.
 * @author thomas
 */
public class GseaAnalysisTimeCourse {
	public static void main(String[] args) {
		GseaAnalysisTimeCourse main = new GseaAnalysisTimeCourse();
		try {
			main.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private File gdbFile = new File(
			"/home/thomas/PathVisio-Data/gene_databases/Mm_Derby_20090509.pgdb"
	);

	private File gexFile = new File(
			"/home/thomas/projects/pps2/stat_results/PPS2_filteredlist_2log.pgex"
	);

	private File pwDir = new File(
			"/home/thomas/data/pathways/20090715-wikipathways"
	);

	private File outDir = new File(
			"/home/thomas/projects/pps2/path_results/bigcat/gsea"
	);
	
	private File dataDir = new File(outDir.getAbsolutePath() + "/data-wikipathways");
	private File reportDir = new File(outDir.getAbsolutePath() + "/report-wikipathways");
	
	private DataSource targetDs;
	
	private String[] timePoints = new String[] {
			"t0", "t0.6", "t2", "t48"
	};
	
	private static final String HF = "HF";
	private static final String LF = "LF";
	
	private String[] classes = new String[] {
		LF, LF, LF, LF, LF, LF, LF, LF,
		HF, HF, HF, HF, HF, HF, HF, HF,
	};

	private Map<String, String[]> samples = new HashMap<String, String[]>();

	public GseaAnalysisTimeCourse() {
		samples.put(timePoints[0], new String[] {
			"LF_t0_200",	"LF_t0_201",	"LF_t0_202",	"LF_t0_203",	"LF_t0_205",	"LF_t0_206",	"LF_t0_204",	"LF_t0_207",
			"HF_t0_252",	"HF_t0_253",	"HF_t0_255",	"HF_t0_256",	"HF_t0_257",	"HF_t0_258",	"HF_t0_259",	"HF_t0_260",	
		});
		samples.put(timePoints[1], new String[] {
			"LF_t0.6_208",	"LF_t0.6_209",	"LF_t0.6_210",	"LF_t0.6_211",	"LF_t0.6_212",	"LF_t0.6_213",	"LF_t0.6_214",	"LF_t0.6_216",
			"HF_t0.6_261",	"HF_t0.6_262",	"HF_t0.6_263",	"HF_t0.6_264",	"HF_t0.6_265",	"HF_t0.6_266",	"HF_t0.6_267",	"HF_t0.6_268",	
		});
		samples.put(timePoints[2], new String[] {
			"LF_t2_217",	"LF_t2_218",	"LF_t2_219",	"LF_t2_220",	"LF_t2_221",	"LF_t2_222",	"LF_t2_223",	"LF_t2_224",
			"HF_t2_269",	"HF_t2_270",	"HF_t2_271",	"HF_t2_272",	"HF_t2_273",	"HF_t2_274",	"HF_t2_275",	"HF_t2_276",
		});
		samples.put(timePoints[3], new String[] {
			"LF_t48_233",	"LF_t48_234",	"LF_t48_235",	"LF_t48_236",	"LF_t48_237",	"LF_t48_238",	"LF_t48_239",	"LF_t48_240",
			"HF_t48_285",	"HF_t48_287",	"HF_t48_288",	"HF_t48_289",	"HF_t48_290",	"HF_t48_291",	"HF_t48_292",	"HF_t48_286",
		});
	}

	void start() throws Exception {
		dataDir.mkdirs();
		reportDir.mkdirs();
		
		BioDataSource.init();
		
		targetDs = BioDataSource.ENTREZ_GENE;
		
		SimpleGex gex = new SimpleGex("" + gexFile, false, new DataDerby());
		IDMapperRdb idMapper =SimpleGdbFactory.createInstance("" + gdbFile, new DataDerby(), 0);
		
		//Prepare the GSEA
		GexGSEA gsea = new GexGSEA(idMapper, targetDs);

		//GSEA for each timepoint
		for(String time : timePoints) {
			File timeReportDir = new File(reportDir, time);
			File timeDataDir = new File(dataDir, time);
			
			if(timeReportDir.exists()) {
				continue; //Skip if the calculation has already been performed
			}
			
			//Find the samples
			String[] sampleNames = samples.get(time);
			Sample[] samples = new Sample[sampleNames.length];
			Map<String, Sample> samplesByName = new HashMap<String, Sample>();
			for(Sample s : gex.getSamples().values()) samplesByName.put(s.getName(), s);
			
			for(int i = 0; i < sampleNames.length; i++) {
				samples[i] = samplesByName.get(sampleNames[i]);
			}
			
			timeDataDir.mkdirs();
			timeReportDir.mkdirs();
			File gmxFile = new File(dataDir, "pathways.gmx");
			File gctFile = new File(timeDataDir, "dataset.gct");
			File clsFile = new File(timeDataDir, "template.cls");
			
			CalcParameters pars = new CalcParameters(gctFile, clsFile, gmxFile, timeReportDir);
			pars.comparison("HF_versus_LF").plot_top_x(250).rpt_label(time);
			gsea.doCalculation(gex, pwDir, Arrays.asList(samples), Arrays.asList(classes), pars);
		}
		
		//Create a table that combines the normalized enrichment scores
		List<String> geneSetNames = null;
		Map<String, double[]> scores = new HashMap<String, double[]>();
		for(int t = 0; t < timePoints.length; t++) {
			String time = timePoints[t];
			
			//Load the result sets
			File edbDir = new File(reportDir + "/" + time + "/edb/");
			EnrichmentDb edb = gsea.readResult(edbDir);
			if(geneSetNames == null) { //Fill the initial map
				geneSetNames = edb.getGeneSetNames();
				for(String gsn : geneSetNames) {
					double[] values = new double[timePoints.length];
					Arrays.fill(values, Double.NaN);
					scores.put(gsn, values);
				}
			}
			for(int i = 0; i < edb.getNumResults(); i++) {
				EnrichmentResult r = edb.getResult(i);
				scores.get(r.getGeneSetName())[t] = r.getScore().getNES();
			}
		}
		
		writeNESFile(new File(reportDir, "totalTable.txt"), geneSetNames, scores, Double.NaN, false);
		writeNESFile(new File(reportDir, "totalTable-min1.txt"), geneSetNames, scores, 1, false);
		writeNESFile(new File(reportDir, "totalTable-min1.2.txt"), geneSetNames, scores, 1.2, false);
		writeNESFile(new File(reportDir, "totalTable-min1.2-rel.txt"), geneSetNames, scores, 1.2, true);
	}
	
	void writeNESFile(File file, List<String> geneSetNames, Map<String, double[]> scores, double minNES, boolean relative) throws IOException {
		//Write the table
		Writer out = new FileWriter(file);
		//Write header
		out.append("Pathway");
		for(String time : timePoints) out.append("\tNES_" + time);
		out.append("\n");
		for(String gsn : geneSetNames) {
			double[] values = scores.get(gsn);
			
			if(!Double.isNaN(minNES)) {
				boolean include = false;
				for(double value : values) {
					if(Math.abs(value) >= minNES) {
						include = true;
						break;
					}
				}
				if(!include) continue; //Skip this pathway
			}
			
			if(relative) {
				for(int i = 0; i < values.length; i++) {
					if(values[i] >= 1) values[i] -= 1;
					else if(values[i] <= -1) values[i] += 1;
					else values[i] = 0;
				}
			}
			out.append(gsn);
			for(int t = 0; t < timePoints.length; t++) {
				out.append("\t" + values[t]);
			}
			out.append("\n");
		}
		out.close();
	}
}
