import rainbownodes.*;
import org.pathvisio.visualization.colorset.*;
import cytoscape.*;
import cytoscape.view.*;
import cytoscape.layout.*;
import cytoscape.data.CyAttributes;
import java.awt.Color;
import PPSGlobals;

for(CyNetworkView view in Cytoscape.getNetworkViewMap().values()) {
	String s = view.getNetwork().getTitle();
	if(s.contains("all significant")) {
		println("Skipping " + s);
		continue;
	}
	//Determine the tissue
	String tissue = null;
	for(t in PPSGlobals.tissues) {
		println(t);
		println(s);
		println("----");
		if(s.contains(t)) {
			tissue = t;
		}
	}
	
	//Create the graphics
	Gradient gradient = new Gradient()
	            .point(-2, Color.GREEN)
	            .point(2, Color.RED);
	
	List<String> visAttr = new ArrayList<String>();
	for(vt in PPSGlobals.visAttrTmp) {
		visAttr.add(vt.replace("TIS", tissue));
	}
	
	Graphic barGraph = new BarGraphic(
	        visAttr, gradient
	);
	
	//Apply the graphics
	GraphicsManager.getInstance().addGraphic(barGraph, view);
	
	//Save a legend
	barGraph.saveLegend(new File(PPSGlobals.outPath, "legend_" + s + ".png"), "Legend for " + s);
}