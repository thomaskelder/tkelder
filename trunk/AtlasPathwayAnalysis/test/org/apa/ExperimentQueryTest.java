package org.apa;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.apa.ae.ArrayDefinition;
import org.apa.ae.ArrayDefinitionCache;
import org.apa.rest.AtlasArrayDesign;
import org.apa.rest.AtlasRestCache;
import org.apa.rest.AtlasExperiment;
import org.apa.rest.AtlasExperimentData;
import org.apa.rest.AtlasRestUtils;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.pathvisio.gex.ReporterData;
import org.pathvisio.gex.SimpleGex;


public class ExperimentQueryTest extends TestCase {
	static final String EXPERIMENT_ID = "E-TABM-34";
	static final String EXP_FACTOR = "organismpart";
	static final String[] FACTOR_VALUES = new String[] { "blood", "tonsil" };
	static final String ARRAY_DESIGN = "A-AFFY-44";

	AtlasRestCache cache = new AtlasRestCache(new File("/home/thomas/data/atlas/"), null);
	
	protected void setUp() throws Exception {
		BioDataSource.init();
	}

	public void testExperimentAsGex() {
		try {
			SimpleGex gex = cache.getGex(EXPERIMENT_ID);
			for(int i = 0; i < gex.getNrRow(); i++) {
				ReporterData d = gex.getRow(i);
				System.err.println(d.getSampleData());
			}
		} catch(Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testLoadExperiment() {
		try {
			AtlasExperiment exp = cache.getExperiment(EXPERIMENT_ID);
			assert(exp != null);

			//Test factor values
			Set<String> factorValues = exp.getExperimentDesign().getFactorValues(EXP_FACTOR);
			assertEquals(factorValues.size(), FACTOR_VALUES.length);
			for(String v : FACTOR_VALUES) {
				assertTrue("Factor " + v + " not in factor values", factorValues.contains(v));
			}
		} catch(Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testLoadExperimentData() {
		try {
			Set<Xref> genes = new HashSet<Xref>();
			genes.add(new Xref("ENSG00000167600", BioDataSource.ENSEMBL_HUMAN));
			genes.add(new Xref("ENSG00000182162", BioDataSource.ENSEMBL_HUMAN));
			genes.add(new Xref("ENSG00000067369", BioDataSource.ENSEMBL_HUMAN));
						
			AtlasExperimentData data = cache.getExperimentData(EXPERIMENT_ID);
			assertTrue(data != null);
			
			Set<String> expGenes = data.getGeneExpressions().get(ARRAY_DESIGN).getGenes().keySet();
			
			//Check the json content for genes that are not in the object
			String json = readUrl(new URL(AtlasRestUtils.getExperimentDataUrl(EXPERIMENT_ID, genes)));
			Pattern p = Pattern.compile("ENSG[0-9]{11}");
			Matcher m = p.matcher(json);
			while(m.find()) {
				String ensId = m.group();
				assertTrue("Missing " + ensId + " from expression genes", expGenes.contains(ensId));
			}
		} catch(Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testArrayDefinition() throws IOException {
		try {
			ArrayDefinitionCache cache = getArrayDefinitionCache();

			AtlasExperiment exp = AtlasRestUtils.loadExperiment(EXPERIMENT_ID);
			AtlasArrayDesign[] designs = exp.getExperimentDesign().getArrayDesigns();
			for(AtlasArrayDesign d : designs) {
				ArrayDefinition def = cache.get(d.getAccession());
				Set<Xref> xrefs = def.getXrefs(BioDataSource.ENSEMBL_HUMAN);
				System.out.println(xrefs.size());
				assertTrue(xrefs.size() > 0);
			}
		} catch(Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private ArrayDefinitionCache getArrayDefinitionCache() throws IOException {
		return new ArrayDefinitionCache(
				getTempDir()
		);
	}

	private String readUrl(URL url) throws IOException {
		StringBuilder content = new StringBuilder();
		URLConnection urlConnection = url.openConnection();
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

		String line;
		while ((line = bufferedReader.readLine()) != null)
		{
			content.append(line + "\n");
		}
		bufferedReader.close();

		return content.toString();
	}
	
	private File getTempDir() {
		return new File(System.getProperty("java.io.tmpdir"));
	}
}
