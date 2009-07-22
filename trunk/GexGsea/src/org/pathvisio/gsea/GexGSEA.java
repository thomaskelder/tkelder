package org.pathvisio.gsea;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.rdb.IDMapperRdb;
import org.pathvisio.debug.Logger;
import org.pathvisio.gex.ReporterData;
import org.pathvisio.gex.Sample;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.model.ConverterException;
import org.pathvisio.model.Pathway;
import org.pathvisio.util.FileUtils;

import xtools.gsea.Gsea;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.DefaultDataset;
import edu.mit.broad.genome.objects.DefaultGeneSetMatrix;
import edu.mit.broad.genome.objects.FSet;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.TemplateFactory;
import edu.mit.broad.genome.objects.TemplateImpl;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentScore;
import edu.mit.broad.genome.parsers.ClsParser;
import edu.mit.broad.genome.parsers.EdbFolderParser;
import edu.mit.broad.genome.parsers.GctParser;
import edu.mit.broad.genome.parsers.GmxParser;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.utils.ImageUtils;
import edu.mit.broad.xbench.heatmap.GramImagerImpl;

/**
 * Utility to use the GSEA libraries with PathVisio datasets.
 * @author thomas
 */
public class GexGSEA {
	private IDMapperRdb idMapper;
	private DataSource targetDs;

	private SimpleGex gex;

	public GexGSEA(IDMapperRdb idMapper, DataSource targetDs) {
		this.idMapper = idMapper;
		this.targetDs = targetDs;
	}

	/**
	 * Create a GSEA Dataset from a PathVisio dataset.
	 * @throws IDMapperException 
	 */
	public Dataset createDataSet(SimpleGex data, List<Sample> samples) throws IDMapperException {
		Logger.log.info("Start querying dataset");

		Logger.log.info("Calculating rows");

		Map<Xref, Map<Sample, Double>> transData = new HashMap<Xref, Map<Sample, Double>>();
		Map<Xref, Map<Sample, Integer>> nrDataPoints = new HashMap<Xref, Map<Sample, Integer>>();

		int maxRow = data.getMaxRow();
		for(int i = 0; i < maxRow; i++) {
			if(i % 100 == 0) {
				Logger.log.info("Processing row " + i + " out of " + maxRow);
			}
			ReporterData row = data.getRow(i);
			Xref reporter = row.getXref();
			if(!targetDs.equals(reporter.getDataSource())) { //Map the ids
				for(Xref x : idMapper.getCrossRefs(reporter, targetDs)) {
					addData(x, transData, nrDataPoints, samples, row);
				}
			} else {
				addData(reporter, transData,  nrDataPoints, samples, row);
			}
		}

		//Calculate averages
		for(Xref x : nrDataPoints.keySet()) {
			Map<Sample, Integer> sampleNrs = nrDataPoints.get(x);
			Map<Sample, Double> sampleData = transData.get(x);
			for(Sample s : samples) {
				int nr = sampleNrs.get(s);
				if(nr > 1) {
					double d = sampleData.get(s);
					sampleData.put(s, d / nr); //Set to average
				}
			}
		}

		//Translate to a matrix
		Matrix m = new Matrix(transData.size(), samples.size());

		List<Xref> rows = new ArrayList<Xref>(transData.keySet());
		for(int r = 0; r < rows.size(); r++) {
			for(int c = 0; c < samples.size(); c++) {
				Xref x = rows.get(r);
				Sample s = samples.get(c);
				double value = transData.get(x).get(s);
				m.setElement(r, c, (float)value);
			}
		}

		String[] rowNames = new String[rows.size()];
		for(int i = 0; i < rows.size(); i++) rowNames[i] = rows.get(i).getId();

		String[] colNames = new String[samples.size()];
		for(int i = 0; i < samples.size(); i++) colNames[i] = samples.get(i).getName();

		Logger.log.info("Creating dataset for " + rowNames.length + " rows and " + colNames.length + " cols.");
		Dataset d = new DefaultDataset(
				data.getDbName(), 
				m, 
				rowNames,
				colNames,
				true //TODO: find out what this parameter means!
		);
		return d;
	}

	public Dataset readDataset(File file) throws Exception {
		GctParser parser = new GctParser();
		List result = parser.parse(file.toString(), file);
		return (Dataset)result.get(0);
	}

	public Template readTemplate(File file) throws Exception {
		ClsParser parser = new ClsParser();
		List result = parser.parse(file.toString(), file);
		return (Template)result.get(0);
	}

	public EnrichmentDb readResult(File edbDir) throws Exception {
		return ParserFactory.readEdb(edbDir, false);
	}

	public void writeResult(File dir, EnrichmentDb result) throws Exception {
		EdbFolderParser parser = new EdbFolderParser();
		parser.export(result, dir);
	}

	public void writeDataset(File file, Dataset ds) throws Exception {
		GctParser parser = new GctParser();
		parser.export(ds, file);
	}

	private void addData(Xref x, Map<Xref, Map<Sample, Double>> transData, Map<Xref, Map<Sample, Integer>> nrDataPoints, List<Sample> samples, ReporterData row) {
		Map<Sample, Double> sampleMap = transData.get(x);
		Map<Sample, Integer> nrMap = nrDataPoints.get(x);
		if(sampleMap == null) {
			sampleMap = new HashMap<Sample, Double>();
			transData.put(x, sampleMap);
		}
		if(nrMap == null) {
			nrMap = new HashMap<Sample, Integer>();
			nrDataPoints.put(x, nrMap);
		}
		for(Sample s : samples) {
			Object od = row.getSampleData(s);
			if(od instanceof Double) {
				double d = (Double)row.getSampleData(s);
				if(sampleMap.containsKey(s)) d += sampleMap.get(s);
				sampleMap.put(s, d);
				int nr = 1;
				if(nrMap.containsKey(s)) nr += nrMap.get(s);
				nrMap.put(s, nr);
			} else {
				Logger.log.warn("Data for sample " + s.getId() + " is not numeric, skipping...");
			}
		}
	}

	/**
	 * Create genesets from GPML pathways.
	 * @throws ConverterException 
	 * @throws IDMapperException 
	 */
	public Collection<GeneSet> createGeneSets(File pathwayDir) throws IDMapperException, ConverterException {
		List<GeneSet> sets = new ArrayList<GeneSet>();
		for(File f : FileUtils.getFiles(pathwayDir, "gpml", true)) {
			GeneSet gs = createGeneSet(f);
			sets.add(gs);
		}
		return sets;
	}

	/**
	 * Create a gene set translated to the given datasource
	 * @throws IDMapperException 
	 * @throws ConverterException 
	 */
	private GeneSet createGeneSet(File pathwayFile) throws IDMapperException, ConverterException {
		Pathway p = new Pathway();
		p.readFromXml(pathwayFile, true);

		Set<String> translatedIds = new HashSet<String>();
		for(Xref xref : p.getDataNodeXrefs()) {
			Logger.log.info("Adding " + xref);
			DataSource ds = xref.getDataSource();
			if(ds != null && xref.getId() != null && !"".equals(xref.getId())) {
				Logger.log.info("\tGetting cross-references");
				for(Xref cross : idMapper.getCrossRefs(xref, targetDs)) {
					Logger.log.info("\t\tAdding: " + cross);
					translatedIds.add(cross.getId());
				}
			}
		}
		return new FSet(
				pathwayFile.getName(), 
				p.getMappInfo().getMapInfoName(), 
				translatedIds.toArray(new String[0])
		);
	}

	public Template createTemplate(List<Sample> samples, List<String> classes, boolean continious) {
		List<Template.Class> tmpClasses = new ArrayList<Template.Class>();
		HashMap<String, Integer> classIds = new HashMap<String, Integer>();
		List<Template.Item> tmpItems = new ArrayList<Template.Item>();

		int i = 0;
		int nextId = 0;
		for(String s : classes) {
			if(!classIds.containsKey(s)) {
				Template.Class cl = new TemplateImpl.ClassImpl(s);
				tmpClasses.add(cl);
				classIds.put(s, nextId++);
			}
			Template.Item it = TemplateImpl.ItemImpl.createItem(s, i++);
			tmpItems.add(it);
		}
		return TemplateFactory.createTemplate(
				"classes", tmpItems, tmpClasses, continious

		);
	}

	public void doCalculation(SimpleGex gex, File pathwayPath, List<Sample> samples, List<String> classes, CalcParameters pars) throws Exception {
		if(!pars.gmx.exists()) {
			writeGeneSets(pars.gmx, pathwayPath.getName(), createGeneSets(pathwayPath));
		}

		if(!pars.res.exists()) {
			writeDataset(pars.res, createDataSet(gex, samples));
		}

		if(!pars.cls.exists()) {
			writeTemplate(pars.cls, createTemplate(
					samples,
					classes,
					false
			));
		}
		Gsea gsea = new Gsea(pars.asProperties());
		gsea.execute();
	}

	public void writeResultTable(EnrichmentDb edb, Writer out) throws IOException {
		out.append("Gene set\tSize\tNrHits\tES\tNES\tNP\tFDR\tFWER\n");

		for (int i = 0; i < edb.getNumResults(); i++) {
			EnrichmentResult result = edb.getResult(i);
			out.append(result.getGeneSetName() + "\t");
			out.append(result.getGeneSet().getNumMembers() + "\t");
			EnrichmentScore s = result.getScore();
			out.append(s.getNumHits() + "\t");
			out.append(s.getES() + "\t");
			out.append(s.getNES() + "\t");
			out.append(s.getNP() + "\t");
			out.append(s.getFDR() + "\t");
			out.append(s.getFWER() + "\t");
			out.append("\n");            
		}
		out.close();
	}

	public void writeNESTable(EnrichmentDb edb, Writer out) throws IOException {
		out.append("Gene set\tNES\n");

		for (int i = 0; i < edb.getNumResults(); i++) {
			EnrichmentResult result = edb.getResult(i);
			out.append(result.getGeneSetName() + "\t");
			EnrichmentScore s = result.getScore();
			out.append(s.getNES() + "\n");
		}
		out.close();
	}

	public void writeGeneSets(File file, String name, Collection<GeneSet> geneSets) throws Exception {
		DefaultGeneSetMatrix mtrx = new DefaultGeneSetMatrix(
				name,
				geneSets.toArray(new GeneSet[geneSets.size()])
		);
		GmxParser gmxParser = new GmxParser();
		gmxParser.export(mtrx, file);
	}

	public void writeTemplate(File file, Template template) throws Exception {
		ClsParser parser = new ClsParser();
		parser.export(template, file);
	}

	public Collection<GeneSet> readGeneSets(File gmxFile) throws Exception {
		GmxParser gmxParser = new GmxParser();
		List result = gmxParser.parse(gmxFile.toString(), gmxFile);
		DefaultGeneSetMatrix mtrx = (DefaultGeneSetMatrix)result.get(0);
		return Arrays.asList(mtrx.getGeneSets());
	}

	public void writeHeatMap(GeneSet geneSet, File file, Dataset dataSet, Template template) {
		GramImagerImpl gim = new GramImagerImpl();
		BufferedImage image = gim.createBpogImage(dataSet, template, geneSet);
		ImageUtils.savePng(image, file);
	}

	public static class CalcParameters {
		private int nperm = 1000;
		private String permute = "phenotype";
		private int set_min = 15;
		private int set_max = 500;
		private boolean gui = false;
		private int plot_top_x = 50;
		private boolean collapse = false;
		private String comparison = null;
		private String rpt_label = "my_analysis";
		
		private File res, cls, gmx, out;

		public CalcParameters(File res, File cls, File gmx, File out) {
			this.res = res;
			this.cls = cls;
			this.gmx = gmx;
			this.out = out;
		}

		public CalcParameters nperm(int n) { nperm = n; return this; }
		public CalcParameters permute_phenotype(String p) { permute = p; return this; }
		public CalcParameters set_min(int i) { set_min = i; return this; }
		public CalcParameters set_max(int i) { set_max = i; return this; }
		public CalcParameters plot_top_x(int i) { plot_top_x = i; return this; }
		public CalcParameters comparison(String s) { comparison = s; return this; }
		public CalcParameters rpt_label(String s) { rpt_label = s; return this; }
		
		public Properties asProperties() {
			Properties p = new Properties();
			p.put("nperm", "" + nperm);
			p.put("permute", "" + permute);
			p.put("set_min", "" + set_min);
			p.put("set_max", "" + set_max);
			p.put("gui", "" + gui);
			p.put("plot_top_x", "" + plot_top_x);
			p.put("res", "" + res);
			p.put("rpt_label", rpt_label);
			
			String clsTxt = cls + "";
			if(comparison != null) {
				clsTxt += "#" + comparison;
			}
			p.put("cls", clsTxt);
			p.put("gmx", "" + gmx);
			p.put("out", "" + out);
			p.put("collapse", "" + collapse);
			return p;
		}
		/*
		-res /home/thomas/projects/pps2/path_results/bigcat/gsea/data/dataset.gct 
		-cls /home/thomas/projects/pps2/path_results/bigcat/gsea/data/template.cls#LF_versus_HF 
		-gmx /home/thomas/projects/pps2/path_results/bigcat/gsea/data/pathways.gmx 
		-collapse false 
		-mode Max_probe 
		-norm meandiv 
		-nperm 1000 
		-permute phenotype 
		-rnd_type no_balance 
		-scoring_scheme weighted 
		-rpt_label my_analysis 
		-metric Signal2Noise 
		-sort real 
		-order descending 
		-include_only_symbols true 
		-make_sets true 
		-median false 
		-num 100 
		-plot_top_x 100 
		-rnd_seed timestamp 
		-save_rnd_lists false 
		-set_max 500 
		-set_min 15 
		-zip_report false 
		-out /home/thomas/gsea_home/output/jul21 
		-gui false
		 */
	}
}