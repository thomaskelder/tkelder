package pvtools;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.bridgedb.IDMapperException;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.DataDerby;
import org.bridgedb.rdb.IDMapperRdb;
import org.bridgedb.rdb.SimpleGdbFactory;
import org.pathvisio.debug.Logger;
import org.pathvisio.gex.ReporterData;
import org.pathvisio.gex.Sample;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.plugins.statistics.StatisticsPathwayResult;
import org.pathvisio.plugins.statistics.StatisticsResult;
import org.pathvisio.preferences.PreferenceManager;

import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.visual.CalculatorCatalog;
import cytoscape.visual.NodeAppearanceCalculator;
import cytoscape.visual.VisualPropertyType;
import cytoscape.visual.VisualStyle;
import cytoscape.visual.calculators.BasicCalculator;
import cytoscape.visual.mappings.BoundaryRangeValues;
import cytoscape.visual.mappings.ContinuousMapping;
import cytoscape.visual.mappings.ObjectMapping;
import cytoscape.visual.ui.LegendDialog;

public class PVToolsPlugin extends CytoscapePlugin {
	public PVToolsPlugin() {
		BioDataSource.init();
		PreferenceManager.init();
		Logger.log.setLogLevel(true, true, true, true, true, true);
	}

	public static SimpleGex openDataSet(File file) throws IDMapperException {
		return new SimpleGex("" + file, false, new DataDerby());
	}

	public static IDMapperRdb openIDMapper(File file) throws IDMapperException {
		return SimpleGdbFactory.createInstance("" + file, new DataDerby(), 0);
	}

	public static void loadTextAttributes(SimpleGex data, Collection<String> colNames) throws IDMapperException {
		CyAttributes attr = Cytoscape.getNodeAttributes();
		
		Collection<Sample> allSamples = data.getSamples().values();
		List<Sample> samples = new ArrayList<Sample>();
		for(Sample s : allSamples) {
			if(colNames.contains(s.getName())) {
				samples.add(s);
			}
		}
		
		for(int i = 0; i < data.getNrRow(); i++) {
			ReporterData rdata = data.getRow(i);
			
			for(Sample s : samples) {
				Object value = rdata.getSampleData(s);
				attr.setAttribute(rdata.getXref().getId(), s.getName(), "" + value);
			}
		}
	}
	
	public static void loadAttributes(SimpleGex data, AttributeOptions options) throws IDMapperException {
		CyAttributes attr = Cytoscape.getNodeAttributes();

		Collection<Sample> samples = data.getSamples().values();

		Sample compareSample = null;
		if(options != null && options.compareColumn != null) {
			for(Sample s : samples) {
				if(options.compareColumn.equals(s.getName())) {
					compareSample = s;
					break;
				}
			}
		}
		
		Set<String> addedIds = new HashSet<String>();
		
		for(int i = 0; i < data.getNrRow(); i++) {
			ReporterData rdata = data.getRow(i);
			
			String id = rdata.getXref().getId();
			
			//First check if we can overwrite existing data
			if(compareSample != null && addedIds.contains(id)) {
				double oldValue = attr.getDoubleAttribute(id, compareSample.getName());
				double newValue = Double.parseDouble("" + rdata.getSampleData(compareSample));
				double diff = oldValue - newValue;
				if(options.keepMax ? diff > 0 : diff < 0) {
					continue; //Skip this row
				}
			}
			
			for(Sample s : samples) {
				Object value = rdata.getSampleData(s);
				if(s.getDataType() == Types.REAL) {
					try {
						double dvalue = Double.parseDouble(value.toString());
						attr.setAttribute(id, s.getName(), dvalue);
						addedIds.add(id);
					} catch(Exception e) {
						//Logger.log.error("Unable to parse double for column " + s.getName() + " and value " + value + "; datatype=" + s.getDataType());
					}
				} else {
					attr.setAttribute(rdata.getXref().getId(), s.getName(), value.toString());
				}
			}
		}
		
		//List the data columns
		for(Sample s : samples) {
			for(int i = 0; i < data.getNrRow(); i++) {
				ReporterData rdata = data.getRow(i);
				Object value = rdata.getSampleData(s);

				if(s.getDataType() == Types.REAL) {
					try {
						double dvalue = Double.parseDouble(value.toString());
						String id = rdata.getXref().getId();
						if(!addedIds.contains(id)) {
							attr.setAttribute(id, s.getName(), dvalue);
						} else {
							//First check if we can overwrite existing data
							if(compareSample != null) {
								double oldValue = attr.getDoubleAttribute(id, compareSample.getName());
							}
						}
					} catch(Exception e) {
						Logger.log.error("Unable to parse double for column " + s.getName() + " and value " + value + "; datatype=" + s.getDataType());
					}
				} else {
					attr.setAttribute(rdata.getXref().getId(), s.getName(), value.toString());
				}
			}
		}
	}
	
	public static void loadAttributes(SimpleGex data) throws IDMapperException {
		loadAttributes(data, null);
	}

	public static void loadZscores(StatisticsResult stats, String attrName) {
		CyAttributes attr = Cytoscape.getNodeAttributes();

		for(StatisticsPathwayResult pwr : stats.getPathwayResults()) {
			attr.setAttribute(pwr.getFile().getName(), attrName, pwr.getZScore());
		}
	}

	public static VisualStyle createZscoreVisualStyle(String visName, String source, String attrName, int maxZscore, int minNodeSize, int maxNodeSize) {
		VisualStyle vis = Cytoscape.getVisualMappingManager().getCalculatorCatalog().getVisualStyle(source);
		return createZscoreVisualStyle(visName, vis, attrName, maxZscore, minNodeSize, maxNodeSize);
	}

	public static VisualStyle createZscoreVisualStyle(String visName, VisualStyle source, String attrName, int maxZscore, int minNodeSize, int maxNodeSize) {
		VisualStyle vs = new VisualStyle(source);
		vs.setName(visName);
		NodeAppearanceCalculator nac = vs.getNodeAppearanceCalculator();
		ContinuousMapping cm = new ContinuousMapping(nac.getDefaultAppearance().get(
				VisualPropertyType.NODE_SIZE), ObjectMapping.NODE_MAPPING);
		BoundaryRangeValues min = new BoundaryRangeValues(minNodeSize, minNodeSize, minNodeSize);
		BoundaryRangeValues max = new BoundaryRangeValues(maxNodeSize, maxNodeSize, maxNodeSize);

		cm.addPoint(0, min);
		cm.addPoint(maxZscore, max);
		cm.setControllingAttributeName(attrName, Cytoscape.getCurrentNetwork(), true);
		nac.setCalculator(new BasicCalculator("z-score", cm, VisualPropertyType.NODE_SIZE));

		CalculatorCatalog cat = Cytoscape.getVisualMappingManager().getCalculatorCatalog();
		if(cat.getVisualStyle(vs.getName()) == null) {
			cat.addVisualStyle(vs);
		}
		return vs;
	}

	public static void saveLegend(VisualStyle vis, File imgFile) throws IOException {
		JPanel legendPanel = LegendDialog.generateLegendPanel(vis);

		JFrame f = new JFrame("Show remain invisible");
		f.setContentPane(legendPanel);
		f.pack();

		Dimension size = legendPanel.getSize();
		BufferedImage img = new BufferedImage(size.width,size.height,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = img.createGraphics();
		legendPanel.paint(g2);

		ImageIO.write(img, "png", imgFile);
	}
}
