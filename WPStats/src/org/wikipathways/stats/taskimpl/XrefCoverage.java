package org.wikipathways.stats.taskimpl;

import static org.wikipathways.stats.TaskParameters.BRIDGE_PATH;
import static org.wikipathways.stats.TaskParameters.GRAPH_HEIGHT;
import static org.wikipathways.stats.TaskParameters.GRAPH_WIDTH;
import static org.wikipathways.stats.TaskParameters.OUT_PATH;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bridgedb.BridgeDb;
import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.bio.Organism;
import org.bridgedb.rdb.SimpleGdb;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.data.time.Quarter;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.ui.RectangleEdge;
import org.wikipathways.stats.Task;
import org.wikipathways.stats.TaskException;
import org.wikipathways.stats.TaskParameters;
import org.wikipathways.stats.TimeInterval;
import org.wikipathways.stats.db.PathwayInfo;
import org.wikipathways.stats.db.WPDatabase;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * What portion of Ensembl genes can be mapped to a pathway
 * on WikiPathways? How does this change over time?
 * @author thomas
 */
public class XrefCoverage implements Task {
	static final String METABOLITES = "metabolites";
	
	Map<String, DataSource> org2ds = new HashMap<String, DataSource>();
	Map<String, SimpleGdb> org2gdb = new HashMap<String, SimpleGdb>();
	
	public void init(TaskParameters par) throws IDMapperException, ClassNotFoundException {
		Class.forName("org.bridgedb.rdb.IDMapperRdb");
		BioDataSource.init();
		
//		org2ds.put(Organism.AnophelesGambiae.latinName(), BioDataSource.ENSEMBL_MOSQUITO);
//		org2ds.put(Organism.ArabidopsisThaliana.latinName(), BioDataSource.TAIR);
//		org2ds.put(Organism.BosTaurus.latinName(), BioDataSource.ENSEMBL_COW);
		org2ds.put(Organism.CaenorhabditisElegans.latinName(), BioDataSource.ENSEMBL_CELEGANS);
//		org2ds.put(Organism.CanisFamiliaris.latinName(), BioDataSource.ENSEMBL_DOG);
		org2ds.put(Organism.DrosophilaMelanogaster.latinName(), BioDataSource.ENSEMBL_FRUITFLY);
//		org2ds.put(Organism.EquusCaballus.latinName(), BioDataSource.ENSEMBL_HORSE);
//		org2ds.put(Organism.GallusGallus.latinName(), BioDataSource.ENSEMBL_CHICKEN);
//		org2ds.put(Organism.OryzaSativa.latinName(), BioDataSource.TAIR);
//		org2ds.put(Organism.PanTroglodytes.latinName(), BioDataSource.ENSEMBL_CHIMP);
		org2ds.put(Organism.RattusNorvegicus.latinName(), BioDataSource.ENSEMBL_RAT);
		org2ds.put(Organism.SaccharomycesCerevisiae.latinName(), BioDataSource.ENSEMBL_SCEREVISIAE);
		org2ds.put(Organism.HomoSapiens.latinName(), BioDataSource.UNIPROT);
		org2ds.put(Organism.MusMusculus.latinName(), BioDataSource.ENSEMBL_MOUSE);
		org2ds.put(Organism.DanioRerio.latinName(), BioDataSource.ENSEMBL_ZEBRAFISH);
		org2ds.put(Organism.MycobacteriumTuberculosis.latinName(), BioDataSource.ENSEMBL_MTUBERCULOSIS);
		org2ds.put(METABOLITES, BioDataSource.HMDB);
		
		String path = par.getString(BRIDGE_PATH);
		addGdb(Organism.HomoSapiens, path + "Hs_Derby_20100601.bridge");
		addGdb(Organism.MusMusculus, path + "Mm_Derby_20100601.bridge");
//		addGdb(Organism.DanioRerio, path + "Dr_Derby_20090720.bridge");
//		addGdb(Organism.AnophelesGambiae, path + "Ag_Derby_20090720.bridge");
//		addGdb(Organism.ArabidopsisThaliana, path + "At_Derby_20100601.bridge");
//		addGdb(Organism.BosTaurus, path + "Bt_Derby_20090720.bridge");
		addGdb(Organism.CaenorhabditisElegans, path + "Ce_Derby_20100601.bridge");
//		addGdb(Organism.DrosophilaMelanogaster, path + "Dm_Derby_20090720.bridge");
//		addGdb(Organism.CanisFamiliaris, path + "Cf_Derby_20090720.bridge");
//		addGdb(Organism.EquusCaballus, path + "Ec_Derby_20090720.bridge");
//		addGdb(Organism.GallusGallus, path + "Gg_Derby_20090720.bridge");
//		addGdb(Organism.OryzaSativa, path + "Oj_Derby_20090720.bridge");
//		addGdb(Organism.PanTroglodytes, path + "Pt_Derby_20090720.bridge");
		addGdb(Organism.RattusNorvegicus, path + "Rn_Derby_20100601.bridge");
//		addGdb(Organism.SaccharomycesCerevisiae, path + "Sc_Derby_20090720.bridge");
		addGdb(Organism.MycobacteriumTuberculosis, path + "Mx_Derby_20100601.bridge");
		org2gdb.put(METABOLITES, (SimpleGdb)BridgeDb.connect("idmapper-pgdb:" + path + "metabolites_100227.bridge"));
	}
	
	private void addGdb(Organism org, String file) throws IDMapperException {
		System.out.println(file);
		org2gdb.put(org.latinName(), (SimpleGdb)BridgeDb.connect("idmapper-pgdb:" + file));
	}
	
	public void start(WPDatabase db, TaskParameters par) throws TaskException {
		try {
			init(par);
			
			PrintWriter txtout = new PrintWriter(new File(par.getFile(TaskParameters.OUT_PATH), "xrefcounts_species.txt"));
			txtout.println("date\tspecies\tcount\ttotal");
			SimpleDateFormat dformat = new SimpleDateFormat("yyyy/MM/dd");
			
			//Find out total gene count per species
			Map<String, Integer> genesPerSpecies = new HashMap<String, Integer>();
			for(String org : org2gdb.keySet()) {
				genesPerSpecies.put(org, org2gdb.get(org).getGeneCount(org2ds.get(org)));
			}
			
			Date start = db.getWpStart();
			TimeInterval timeInterval = new TimeInterval(start, Quarter.class);

			TimeTableXYDataset data = new TimeTableXYDataset();
			
			RegularTimePeriod period = null;
			while((period = timeInterval.getNext()) != null) {
				System.out.println("Processing " + period);
				Date time = new Date(period.getLastMillisecond());
				Set<PathwayInfo> snapshot = PathwayInfo.getSnapshot(db, time);
				Multimap<String, Xref> xrefsPerSpecies = new HashMultimap<String, Xref>();
				
				for(PathwayInfo i : snapshot) {
					String s = i.getSpecies();
					Set<Xref> xrefs = i.getXrefs();
					//Map genes
					IDMapper idm = org2gdb.get(s);
					if(idm != null) {
						for(Xref x : xrefs) xrefsPerSpecies.putAll(s, idm.mapID(x, org2ds.get(s)));
					}
					//Map metabolites
					s = METABOLITES;
					idm = org2gdb.get(s);
					if(idm != null) {
						for(Xref x : xrefs) xrefsPerSpecies.putAll(s, idm.mapID(x, org2ds.get(s)));
					}
				}
				for(String s : xrefsPerSpecies.keySet()) {
					Organism org = Organism.fromLatinName(s);
					DataSource ds = org2ds.get(s);
					String label = ds.getFullName();
					if(!label.contains("Ensembl")) {
						label += " (" + (org == null ? s : org.shortName()) + ")";
					}
					data.add(period, 
							100.0 * xrefsPerSpecies.get(s).size() / genesPerSpecies.get(s), 
							label);
					
					txtout.println(
							dformat.format(time) + "\t" + label + "\t" + xrefsPerSpecies.get(s).size() + "\t" + genesPerSpecies.get(s)
					);
				}
				
				System.err.println(Runtime.getRuntime().totalMemory() / 1000);
				Runtime.getRuntime().gc();
				System.err.println(Runtime.getRuntime().totalMemory() / 1000);
				System.err.println("---");
			}
			
			txtout.close();
			
			JFreeChart chart = ChartFactory.createTimeSeriesChart(
					"WikiPathways gene / metabolite reference coverage", "Date", "% coverage", data, true, false, false);
			DateAxis axis = new DateAxis("Date");
			chart.getXYPlot().setDomainAxis(axis);
			chart.getLegend().setPosition(RectangleEdge.RIGHT);
			chart.getXYPlot().setBackgroundPaint(Color.WHITE);
			Stroke s = new BasicStroke(3);
			for(int i = 0; i < chart.getXYPlot().getSeriesCount(); i++) {
				chart.getXYPlot().getRenderer().setSeriesStroke(i, s);
			}
			
			ChartUtilities.saveChartAsPNG(
					new File(par.getFile(OUT_PATH), "xrefcoverage_species.png"), 
					chart, par.getInt(GRAPH_WIDTH), par.getInt(GRAPH_HEIGHT)
			);
		} catch (Exception e) {
			throw new TaskException(e);
		}
	}
}
