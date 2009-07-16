package pvtools;

import java.io.File;
import java.sql.Types;
import java.util.Collection;

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

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.data.CyAttributesUtils.AttributeType;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.visual.NodeAppearanceCalculator;
import cytoscape.visual.VisualPropertyType;
import cytoscape.visual.VisualStyle;
import cytoscape.visual.calculators.BasicCalculator;
import cytoscape.visual.mappings.BoundaryRangeValues;
import cytoscape.visual.mappings.ContinuousMapping;
import cytoscape.visual.mappings.ObjectMapping;

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
	
	public static void loadAttributes(SimpleGex data) throws IDMapperException {
		CyAttributes attr = Cytoscape.getNodeAttributes();
		
		//List the data columns
		Collection<Sample> samples = data.getSamples().values();
		for(Sample s : samples) {
			for(int i = 0; i < data.getMaxRow(); i++) {
				ReporterData rdata = data.getRow(i);
				Object value = rdata.getSampleData(s);
				
				if(s.getDataType() == Types.REAL) {
					try {
					double dvalue = Double.parseDouble(value.toString());
					attr.setAttribute(rdata.getXref().getId(), s.getName(), dvalue);
					} catch(Exception e) {
						Logger.log.error("Unable to parse double for column " + s.getName() + " and value " + value + "; datatype=" + s.getDataType());
					}
				} else {
					attr.setAttribute(rdata.getXref().getId(), s.getName(), value.toString());
				}
			}
		}
	}
	
	public static void loadZscores(StatisticsResult stats, String attrName) {
		CyAttributes attr = Cytoscape.getNodeAttributes();
		
		for(StatisticsPathwayResult pwr : stats.getPathwayResults()) {
			attr.setAttribute(pwr.getFile().getName(), attrName, pwr.getZScore());
		}
	}
	
	public static void setZscoreVisualStyle(VisualStyle source, String attrName, int maxZscore, int minNodeSize, int maxNodeSize) {
		VisualStyle vs = new VisualStyle(source);
		vs.setName("z-score-" + attrName);
		NodeAppearanceCalculator nac = vs.getNodeAppearanceCalculator();
		ContinuousMapping cm = new ContinuousMapping(nac.getDefaultAppearance().get(
				VisualPropertyType.NODE_SIZE), ObjectMapping.NODE_MAPPING);
		BoundaryRangeValues min = new BoundaryRangeValues(minNodeSize, minNodeSize, minNodeSize);
		BoundaryRangeValues max = new BoundaryRangeValues(maxNodeSize, maxNodeSize, maxNodeSize);
		
		cm.addPoint(0, min);
		cm.addPoint(maxZscore, max);
		cm.setControllingAttributeName(attrName, Cytoscape.getCurrentNetwork(), true);
		nac.setCalculator(new BasicCalculator("z-score", cm, VisualPropertyType.NODE_SIZE));
		
		Cytoscape.getVisualMappingManager().getCalculatorCatalog().addVisualStyle(vs);
		Cytoscape.getVisualMappingManager().setVisualStyle(vs);
	}
	
	public static void test(CyNetwork network, String attr1, String attr2, String newAttr) {
		CyAttributes attr = Cytoscape.getNodeAttributes();
		for(Object no : Cytoscape.getCyNodesList()) {
			String id = ((CyNode)no).getIdentifier();
			if(attr.hasAttribute(id, attr1) && attr.hasAttribute(id, attr2) &&
					attr.getType(attr1) == CyAttributes.TYPE_FLOATING &&
					attr.getType(attr2) == CyAttributes.TYPE_FLOATING) {
				double d1 = attr.getDoubleAttribute(id, attr1);
				double d2 = attr.getDoubleAttribute(id, attr2);
				attr.setAttribute(id, newAttr, Math.log(d2/d1)/Math.log(2));
			}
		}
	}
}
