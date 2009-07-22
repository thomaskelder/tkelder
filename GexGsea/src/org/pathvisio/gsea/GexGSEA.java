package org.pathvisio.gsea;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.DBConnector;
import org.bridgedb.rdb.DataDerby;
import org.bridgedb.rdb.IDMapperRdb;
import org.bridgedb.rdb.SimpleGdbFactory;
import org.pathvisio.data.DBConnDerby;
import org.pathvisio.debug.Logger;
import org.pathvisio.gex.Sample;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.model.ConverterException;
import org.pathvisio.model.Pathway;

import edu.mit.broad.genome.alg.gsea.GeneSetScoringTable;
import edu.mit.broad.genome.alg.gsea.GeneSetScoringTables;
import edu.mit.broad.genome.alg.gsea.KSTests;
import edu.mit.broad.genome.alg.gsea.PValueCalculator;
import edu.mit.broad.genome.alg.gsea.PValueCalculatorImpls;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.FSet;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.TemplateFactory;
import edu.mit.broad.genome.objects.TemplateImpl;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentScore;
import edu.mit.broad.genome.utils.ImageUtils;
import edu.mit.broad.xbench.heatmap.GramImagerImpl;

public class GexGSEA {
	SimpleGex gex;
	IDMapperRdb idMapper;
	
	public GexGSEA(IDMapperRdb idm, SimpleGex gex) {
		this.gex = gex;
		this.idMapper = idm;
	}
	
	public void setGdb(IDMapperRdb idm) {
		this.idMapper = idm;
	}
	
	public IDMapperRdb getIDMapper() {
		return idMapper;
	}
	
	/**
	 * Check whether all conditions are met
	 * to create the dataset
	 * @see #createDataSet()
	 * @return True if there is a connection to the gdb and gex, false if not
	 */
	public boolean canCreateDataset() {
		return idMapper != null && idMapper.isConnected() &&
				gex != null && gex.isConnected();
	}
	
	/**
	 * Check whether all conditions are met
	 * to create the gene sets
	 * @return True if there is a connection to the gdb
	 */
	public boolean canCreateGeneSets() {
		return idMapper != null && idMapper.isConnected();
	}
	
	public void setGex(SimpleGex gex) {
		this.gex = gex;
	}
	
	public SimpleGex getGex() {
		return gex;
	}
	
	public EnrichmentDb doCalculation(Dataset dataSet, Template template, List<GeneSet> geneSets) throws SQLException {
		KSTests tests = new KSTests();

        int num_permutations = 1000;
        int num_markers = 100; // used only for marker analysis - not a gsea algorithm parameter

        boolean permute_phenotype = true; // if false, then gene sets are permuted

        GeneSetScoringTable scoring_scheme = new GeneSetScoringTables.Weighted();

        // OK, parameters all done, make the call
        EnrichmentDb edb = tests.executeGsea(
        		dataSet, 
        		template, 
        		geneSets.toArray(new GeneSet[0]), 
        		num_permutations, 
        		scoring_scheme, 
        		permute_phenotype, 
        		num_markers
        );

        // EDB now has the stats but not the FDRs yet
        // FDRs can be calc in many ways, heres the gsea way:
        PValueCalculator pvc = new PValueCalculatorImpls.GseaImpl("meandiv");
        EnrichmentResult[] results = pvc.calcNPValuesAndFDR(edb.getResults());
        EnrichmentDb edb_with_fdr = edb.cloneDeep(results);
        return edb_with_fdr;
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
	
	public void saveHeatMap(GeneSet geneSet, File file, Dataset dataSet, Template template) {
        GramImagerImpl gim = new GramImagerImpl();
        BufferedImage image = gim.createBpogImage(dataSet, template, geneSet);
        ImageUtils.savePng(image, file);
	}
	
	Template createTemplate(List<Sample> samples, List<String> classes) {
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
			Template.Item it = TemplateImpl.ItemImpl.createItem(classIds.get(s).toString(), i++);
			tmpItems.add(it);
		}
		return TemplateFactory.createTemplate(
				"classes", tmpItems, tmpClasses, false
				
		);
	}
	
	List<GeneSet> createGeneSets(List<Pathway> pathways, DataSource targetDs) throws IDMapperException {
		List<GeneSet> sets = new ArrayList<GeneSet>();
		for(Pathway p : pathways) {
			sets.add(createGeneSet(p, targetDs));
		}
		return sets;
	}
	
	/**
	 * Create a gene set translated to the given datasource
	 * @throws IDMapperException 
	 */
	private GeneSet createGeneSet(Pathway p, DataSource targetDs) throws IDMapperException {
		Set<String> ensIds = new HashSet<String>();
		for(Xref xref : p.getDataNodeXrefs()) {
			Logger.log.info("Adding " + xref);
			DataSource ds = xref.getDataSource();
			if(ds != null && xref.getId() != null && !"".equals(xref.getId())) {
				if(ds == targetDs) {
					Logger.log.info("\tIs target datasource already, no mapping needed");
					ensIds.add(xref.getId());
				} else {
					Logger.log.info("\tGetting ensembl ids");
					for(Xref cross : idMapper.getCrossRefs(xref, targetDs)) {
						Logger.log.info("\t\tAdding: " + cross);
						ensIds.add(cross.getId());
					}
				}
			}
		}
		return new FSet(
				p.getSourceFile().getAbsolutePath(), 
				p.getMappInfo().getMapInfoName(), 
				ensIds.toArray(new String[0])
		);
	}
	
	Dataset createDataSet() throws SQLException {
		return Query.getDataUnique(gex);
	}
	
	public static void main(String[] args) {
		try {
			String gdbName = args[0];
			String gexName = args[1];
			String pwString = args[2];
			String[] sampleNames = args[3].split(",");
			List<String> classes = Arrays.asList(args[4].split(","));

			List<Pathway> pathways = new ArrayList<Pathway>();
			loadPathways(new File(pwString), pathways);

			DBConnector dbconn = new DBConnDerby();
			IDMapperRdb gdb = SimpleGdbFactory.createInstance("" + gdbName, new DataDerby(), 0);
			SimpleGex gex = new SimpleGex(gexName, false, dbconn);
			
			List<Sample> samples = new ArrayList<Sample>();
			for(String sn : sampleNames) {
				for(Sample s : gex.getSamples().values()) {
					if(sn.equals(s.getName())) samples.add(s);
				}
			}
			GexGSEA gg = new GexGSEA(gdb, gex);
			Template template = gg.createTemplate(samples, classes);
			Dataset dataSet = gg.createDataSet();
			List<GeneSet> geneSets = gg.createGeneSets(pathways, BioDataSource.UNIPROT);
			EnrichmentDb edb = gg.doCalculation(dataSet, template, geneSets);

			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
			String name = "gsea-" + sdf.format(cal.getTime()) + ".txt";
			BufferedWriter out = new BufferedWriter(new FileWriter(name));
			gg.writeResultTable(edb, out);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	static void loadPathways(File dir, List<Pathway> pathways) throws ConverterException {
		if(dir.isDirectory()) {
			for(File f : dir.listFiles()) {
				loadPathways(f, pathways);
			}
		} else if(dir.getName().endsWith(".gpml")) {
			try {
				pathways.add(loadPathway(dir));
			} catch(ConverterException e) {
				Logger.log.error("Error reading pathway " + dir, e);
			}
		}
	}
	
	static Pathway loadPathway(File f) throws ConverterException {
		Pathway p = new Pathway();
		p.readFromXml(f, true);
		return p;
	}
}
