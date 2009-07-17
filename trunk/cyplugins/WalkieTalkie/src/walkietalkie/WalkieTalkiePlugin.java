package walkietalkie;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

import org.pathvisio.visualization.colorset.Criterion.CriterionException;

import walkietalkie.WalkieTalkie.PathwayInfo;
import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.visual.CalculatorCatalog;

/**
 * Cytoscape plugin entry point for WalkieTalkie scripts.
 * @author thomas
 */
public class WalkieTalkiePlugin extends CytoscapePlugin {
	private static void registerVisualStyle() {
		CalculatorCatalog cat = Cytoscape.getVisualMappingManager().getCalculatorCatalog();
		if(cat.getVisualStyle(WalkieTalkieVisualStyle.NAME) == null) {
			//Register default visual style
			cat.addVisualStyle(new WalkieTalkieVisualStyle(Cytoscape.getVisualMappingManager().getVisualStyle()));
		}
	}
	
	public static CyNetwork loadSif(WalkieTalkie wt, Collection<PathwayInfo> filterPathways, boolean createView) throws IOException, CriterionException {
		//Write sif to temporary file
		File tmp = File.createTempFile("tmpnetwork", ".sif");
		Writer out = new FileWriter(tmp);
		wt.writeSif(out, filterPathways);
		out.close();
		
		//Load sif into Cytoscape
		CyNetwork network = Cytoscape.createNetworkFromFile(tmp + "", createView);
		
		return network;
	}
	
	public static CyNetwork loadSif(WalkieTalkie wt, boolean createView) throws IOException, CriterionException {
		return loadSif(wt, null, createView);
	}
	
	public static void loadAttributes(WalkieTalkie wt) throws IOException {
		//Write attributes to temporary file
		File tmpLabel = File.createTempFile("label", ".txt");
		Writer out = new BufferedWriter(new FileWriter(tmpLabel));
		wt.writeLabelAttributes(out);
		out.close();
		
		File tmpType = File.createTempFile("type", ".txt");
		out = new BufferedWriter(new FileWriter(tmpType));
		wt.writeTypeAttributes(out);
		out.close();
		
		String[] attrFiles = new String[] { "" + tmpLabel, "" + tmpType };
		Cytoscape.loadAttributes(attrFiles, new String[0]);
		
		registerVisualStyle();
	}
}