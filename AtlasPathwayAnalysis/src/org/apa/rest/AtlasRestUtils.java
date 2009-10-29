package org.apa.rest;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apa.ae.ArrayDefinitionCache;
import org.apa.data.Experiment;
import org.apa.data.ExperimentData;
import org.apa.data.Factor;
import org.apa.data.FactorValue;
import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.bio.Organism;
import org.bridgedb.rdb.DataDerby;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.pathvisio.Engine;
import org.pathvisio.gex.GexManager;
import org.pathvisio.gex.Sample;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.preferences.PreferenceManager;
import org.pathvisio.visualization.Visualization;
import org.pathvisio.visualization.VisualizationManager;
import org.pathvisio.visualization.VisualizationMethod;
import org.pathvisio.visualization.VisualizationMethodProvider;
import org.pathvisio.visualization.VisualizationMethodRegistry;
import org.pathvisio.visualization.colorset.ColorGradient;
import org.pathvisio.visualization.colorset.ColorSet;
import org.pathvisio.visualization.plugins.ColorByExpression;
import org.pathvisio.visualization.plugins.DataNodeLabel;

public class AtlasRestUtils {
	static final Logger log = Logger.getLogger("org.apa.rest");
	
	static {
		BioDataSource.init();
		PreferenceManager.init();
	}

	private static ObjectMapper jsonMapper = new ObjectMapper();
	private static String QUERY_EXP_BASE = "http://www.ebi.ac.uk/gxa/api?experiment=";
	private static final int NR_GENES_PER_REQUEST = 50;

	public static Set<String> listExperiments() throws IOException {
		Set<String> ids = new HashSet<String>();
		
		//load http://www.ebi.ac.uk/gxa/experiment/index.htm
		//look for pattern href="/gxa/experiment/E-MEXP-749"

		URL url = new URL("http://www.ebi.ac.uk/gxa/experiment/index.htm");
		String page = "";
		URLConnection connection =  url.openConnection();
		Scanner scanner = new Scanner(connection.getInputStream());
		scanner.useDelimiter("\\Z");
		page = scanner.next();
		
		Pattern p = Pattern.compile("href=\"/gxa/experiment/(.+?)\"");
		
		Matcher m = p.matcher(page);
		while(m.find()) {
			ids.add(m.group(1));
		}
		
		return ids;
	}

	public static AtlasExperiment loadExperiment(String accession) throws JsonParseException, JsonMappingException, MalformedURLException, IOException {
		String url = QUERY_EXP_BASE + accession;
		return jsonMapper.readValue(new URL(url), AtlasExperiment.class);
	}

	private static AtlasExperimentData loadExperimentData(AtlasExperimentData master, String accession, Collection<Xref> genes) throws JsonParseException, JsonMappingException, IOException {
		if(master == null) {
			master = doLoadExperimentData(accession, genes);
		} else {
			AtlasExperimentData data = doLoadExperimentData(accession, genes);
			//Merge into master
			for(String key : data.getGeneExpressions().keySet()) {
				AtlasGeneExpression exprMaster = master.getGeneExpressions().get(key);
				if(exprMaster == null) master.getGeneExpressions().put(key, data.getGeneExpressions().get(key));
				else {
					exprMaster.getGenes().putAll(data.getGeneExpressions().get(key).getGenes());
				}
			}
			for(String key : data.getGeneExpressionStatistics().keySet()) {
				AtlasExpressionStatistics statsMaster = master.getGeneExpressionStatistics().get(key);
				if(statsMaster == null) master.getGeneExpressions().put(key, data.getGeneExpressions().get(key));
				else {
					statsMaster.getGenes().putAll(data.getGeneExpressionStatistics().get(key).getGenes());
				}
			}
		}
		return master;
	}

	private static AtlasExperimentData doLoadExperimentData(String accession, Collection<Xref> genes) throws JsonParseException, JsonMappingException, MalformedURLException, IOException {
		String url = getExperimentDataUrl(accession, genes);
		log.finest("Query experiment data: " + url);
		return jsonMapper.readValue(new URL(url), AtlasExperimentData.class);
	}

	public static String getExperimentDataUrl(String accession, Collection<Xref> genes) {
		String url = QUERY_EXP_BASE + accession;
		for(Xref x : genes) {
			url += "&gene=" + x.getId();
		}
		return url;
	}

	public static AtlasExperimentData loadExperimentData(String accession, Collection<Xref> genes) throws JsonParseException, JsonMappingException, IOException {
		List<Xref> sortedGenes = new ArrayList<Xref>(genes);

		AtlasExperimentData data = null;
		int step = NR_GENES_PER_REQUEST;
		for(int i = 0; i < genes.size(); i += step) {
			log.fine("Loading experiment data part " + i / step + " out of " + genes.size() / step);
			data = loadExperimentData(data, accession, sortedGenes.subList(i, Math.min(genes.size(), i + step)));
		}
		if(data == null) {
			log.warning("Couldn't load experiment data, no genes for " + accession);
		}
		return data;
	}

	public static Set<Xref> getAllExperimentGenes(AtlasExperiment experiment, ArrayDefinitionCache adCache, DataSource targetDs, IDMapper idMapper) throws FileNotFoundException, IOException, IDMapperException {
		Set<Xref> genes = new HashSet<Xref>();
		for(AtlasArrayDesign design : experiment.getExperimentDesign().getArrayDesigns()) {
			Set<Xref> xrefs = adCache.get(design.getAccession()).getXrefs(targetDs);
			if(xrefs.size() == 0 && idMapper != null) {
				Set<Xref> refseqs = adCache.get(design.getAccession()).getXrefs(BioDataSource.REFSEQ);
				//Fallback on refseq and map to target datasource
				for(Xref refseq : refseqs) {
					xrefs.addAll(idMapper.mapID(refseq, targetDs));
				}
				//Or fallback on probe identifiers and assume affy
				if(refseqs.size() == 0) {
					for(Xref affy : adCache.get(design.getAccession()).getXrefs(BioDataSource.AFFY)) {
						xrefs.addAll(idMapper.mapID(affy, targetDs));
					}
				}
			}
			genes.addAll(xrefs);
		}
		return genes;
	}

	public static Experiment asExperiment(AtlasExperimentData restExp, Organism organism, String name) {
		Experiment exp = new Experiment(restExp.getExperimentInfo().getAccession());
		exp.setOrganism(organism.latinName());
		exp.setDesciption(restExp.getExperimentInfo().getDescription());
		exp.setName(name);
		
		String[] factors = restExp.getExperimentDesign().getExperimentalFactors();

		for(String fn : factors) {
			for(String fv : restExp.getExperimentDesign().getFactorValues(fn)) {
				FactorValue f = new FactorValue(new Factor(fn), fv);
				exp.addFactorValue(f);
				exp.setData(f, new ExperimentData(exp, f));
			}
		}

		for(String design : restExp.getGeneExpressions().keySet()) {
			AtlasGeneExpression expr = restExp.getGeneExpressions().get(design);
			AtlasExpressionStatistics stats = restExp.getGeneExpressionStatistics().get(design);
			for(String gene : expr.getGenes().keySet()) {
				Map<String, Double[]> xexpr = expr.getGenes().get(gene);
				Map<String, AtlasStatGene[]> xstat = stats.getGenes().get(gene);

				Map<FactorValue, Double> exprByFactor = new HashMap<FactorValue, Double>();
				Map<FactorValue, AtlasStatValue> statByFactor = new HashMap<FactorValue, AtlasStatValue>();

				for(String xgene : xexpr.keySet()) {
					Double[] exprValues = xexpr.get(xgene);
					AtlasStatGene[] statValues = xstat.get(xgene);

					for(int e = 0; e < exprValues.length; e++) {
						int assayIndex = expr.getAssays()[e];
						AtlasAssay assay = restExp.getExperimentDesign().getAssays()[assayIndex];
						for(String fn : assay.getFactorValues().keySet()) {
							String fv = assay.getFactorValues().get(fn);
							if(fv != null) {
								FactorValue f = new FactorValue(new Factor(fn), fv);
								exprByFactor.put(f, exprValues[e]);
							}
						}
					}

					if(statValues != null) {
						for(AtlasStatGene s : statValues) {
							FactorValue f = new FactorValue(new Factor(s.getEf()), s.getEfv());
							statByFactor.put(f, s.getStat());
						}
					}

					for(FactorValue f : exp.getFactorValues()) {
						AtlasStatValue sv = statByFactor.get(f);
						Double ev = exprByFactor.get(f);
						
						exp.getData(f).addEntry(gene, 
								sv == null ? 1 : sv.getPvalue(), 
								sv == null ? 0 : sv.getTstat(),
								ev == null ? Double.NaN : ev.doubleValue()
						);
					}
				}
			}
		}

		return exp;
	}

	public static SimpleGex asGex(File gexPath, AtlasExperimentData data, Organism organism) throws JsonParseException, JsonMappingException, IOException, IDMapperException, URISyntaxException, SQLException {
		//List all experimental factors
		String[] factors = data.getExperimentDesign().getExperimentalFactors();
		String accession = data.getExperimentInfo().getAccession();

		//Create the dataset
		SimpleGex gex = new SimpleGex(new File(gexPath, accession).getAbsolutePath(), true, new DataDerby());
		gex.prepare();

		//Create a sample series for each experimental factor
		AtlasSampleMap sampleMap = new AtlasSampleMap();
		Set<Sample> tstatSamples = new HashSet<Sample>();
		Set<Sample> pvalueSamples = new HashSet<Sample>();
		Set<Sample> exprSamples = new HashSet<Sample>();

		int sampleId = 0;
		for(String f : factors) {
			for(String v : data.getExperimentDesign().getFactorValues(f)) {
				Sample es = new Sample(sampleId++, AtlasSampleMap.getExprSampleName(f, v), Types.REAL);
				Sample ts = new Sample(sampleId++, AtlasSampleMap.getTstatSampleName(f, v), Types.REAL);
				Sample ps = new Sample(sampleId++, AtlasSampleMap.getPvalueSampleName(f, v), Types.REAL);
				sampleMap.addSample(es);
				sampleMap.addSample(ts);
				sampleMap.addSample(ps);
				tstatSamples.add(ts);
				pvalueSamples.add(ps);
				exprSamples.add(es);
			}
		}
		for(Sample s : sampleMap.getAllSamples()) gex.addSample(s.getId(), s.getName(), s.getDataType());

		//Extract and store their p-values and expression
		int line = 0;

		for(String design : data.getGeneExpressions().keySet()) {
			log.info("Processing design " + design);
			int i = 0;

			//Expression
			AtlasGeneExpression expr = data.getGeneExpressions().get(design);
			AtlasExpressionStatistics stats = data.getGeneExpressionStatistics().get(design);
			for(String gene : expr.getGenes().keySet()) {
				log.fine("\tImporting gene " + ++i + " of " + expr.getGenes().size());
				//TESTING
				//if(line > 100) break;

				Xref x = new Xref(gene, getResultDatasource(organism));

				Map<String, Double[]> xexpr = expr.getGenes().get(gene);
				Map<String, AtlasStatGene[]> xstat = stats.getGenes().get(gene);

				for(String xgene : xexpr.keySet()) {
					Set<Sample> addedSamples = new HashSet<Sample>();
					Double[] exprValues = xexpr.get(xgene);
					AtlasStatGene[] statValues = xstat.get(xgene);

					for(int e = 0; e < exprValues.length; e++) {
						int assayIndex = expr.getAssays()[e];
						AtlasAssay assay = data.getExperimentDesign().getAssays()[assayIndex];
						for(String f : assay.getFactorValues().keySet()) {
							String fv = assay.getFactorValues().get(f);
							if(fv != null) {
								Sample s = sampleMap.getExprSample(f, fv);
								gex.addExpr(x, s.getId() + "", exprValues[e] + "", line);
								addedSamples.add(s);
							}
						}
					}

					if(statValues != null) {
						for(AtlasStatGene s : statValues) {
							Sample sP = sampleMap.getPvalueSample(s.getEf(), s.getEfv());
							Sample sT = sampleMap.getTstatSample(s.getEf(), s.getEfv());
							gex.addExpr(x, sP.getId() + "", s.getStat().getPvalue() + "", line);
							gex.addExpr(x, sT.getId() + "", s.getStat().getTstat() + "", line);
							addedSamples.add(sP);
							addedSamples.add(sT);
						}
					}
					//Fill empty samples with 0 for T and 1 for p
					for(Sample s : tstatSamples) {
						if(!addedSamples.contains(s)) {
							gex.addExpr(x, s.getId() + "", "0", line);
						}
					}
					for(Sample s : pvalueSamples) {
						if(!addedSamples.contains(s)) {
							gex.addExpr(x, s.getId() + "", "1", line);
						}
					}
					for(Sample s : exprSamples) {
						if(!addedSamples.contains(s)) {
							gex.addExpr(x, s.getId() + "", "NaN", line);
						}
					}
					line++;
				}
			}
		}

		//Close and re-open the read-only gex
		log.info("Defragmenting database");
		gex.finalize();
		gex = new SimpleGex(new File(gexPath, accession + ".pgex").getAbsolutePath(), false, new DataDerby());

		Engine engine = new Engine();
		final GexManager gexMgr = new GexManager();
		gexMgr.setCurrentGex(gex);
		VisualizationMethodRegistry visReg = VisualizationMethodRegistry.getCurrent();
		visReg.registerMethod(
				ColorByExpression.class.toString(), 
				new VisualizationMethodProvider() {
					public VisualizationMethod create(Visualization v, String registeredName) {
						return new ColorByExpression(v, registeredName, gexMgr);
					}
				}
		);
		visReg.registerMethod(
				DataNodeLabel.class.toString(), 
				new VisualizationMethodProvider() {
					public VisualizationMethod create(Visualization v, String registeredName) {
						return new DataNodeLabel(v, registeredName);
					}
				}
		);
		VisualizationManager visMgr = new VisualizationManager(visReg, engine, gexMgr);
		ColorSet csE = new ColorSet(visMgr.getColorSetManager());
		visMgr.getColorSetManager().addColorSet(csE);
		ColorGradient gE = new ColorGradient(csE);
		gE.addColorValuePair(gE.new ColorValuePair(Color.WHITE, 0));
		gE.addColorValuePair(gE.new ColorValuePair(Color.RED, 15));
		csE.addObject(gE);
		csE.setName("Expression");

		ColorSet csT = new ColorSet(visMgr.getColorSetManager());
		ColorGradient gT = new ColorGradient(csT);
		visMgr.getColorSetManager().addColorSet(csT);
		gT.addColorValuePair(gT.new ColorValuePair(Color.GREEN, -10));
		gT.addColorValuePair(gT.new ColorValuePair(Color.WHITE, 0));
		gT.addColorValuePair(gT.new ColorValuePair(Color.RED, 10));
		csT.addObject(gT);
		csT.setName("T statistic");

		//Create a visualization for each experimental factor
		for(String factor : factors) {
			List<String> factorValues = new ArrayList<String>(data.getExperimentDesign().getFactorValues(factor));
			Collections.sort(factorValues);

			Visualization vE = new Visualization("expr_" + factor);
			for(VisualizationMethod m : vE.getMethods()) {
				if(m instanceof ColorByExpression) {
					ColorByExpression cm = (ColorByExpression)m;
					for(String fv : factorValues) {
						cm.addUseSample(sampleMap.getExprSample(factor, fv));
					}
					cm.setSingleColorSet(csE);
					cm.setActive(true);
				}
				if(m instanceof DataNodeLabel) {
					m.setActive(true);
				}
			}
			visMgr.addVisualization(vE);

			Visualization vT = new Visualization("tstat_" + factor);
			for(VisualizationMethod m : vT.getMethods()) {
				if(m instanceof ColorByExpression) {
					ColorByExpression cm = (ColorByExpression)m;
					for(String fv : factorValues) {
						cm.addUseSample(sampleMap.getTstatSample(factor, fv));
					}
					cm.setSingleColorSet(csT);
					cm.setActive(true);
				}
				if(m instanceof DataNodeLabel) {
					m.setActive(true);
				}
			}
			visMgr.addVisualization(vT);
		}
		visMgr.saveXML();
		return gex;
	}
	
	public static DataSource getQueryDatasource(Organism organism) {
		//Prefer Ensembl
		DataSource ds = BioDataSource.getSpeciesSpecificEnsembl(organism);
		
		//For arabidopsis, prefer TAIR
		if(organism == Organism.ArabidopsisThaliana) return BioDataSource.TAIR;
		
		//For fly, prefer flybase
		if(organism == Organism.DrosophilaMelanogaster) return BioDataSource.FLYBASE;
		
		//Unless not available
		if(ds == null) {
			return BioDataSource.UNIPROT;
		}
		return ds;
	}
	
	public static DataSource getResultDatasource(Organism organism) {
		//If Ensembl is available, atlas service returns Ensembl
		DataSource ds = BioDataSource.getSpeciesSpecificEnsembl(organism);

		//Otherwise, it returns UniProt
		if(ds == null) {
			return BioDataSource.UNIPROT;
		}
		return ds;
	}
}
