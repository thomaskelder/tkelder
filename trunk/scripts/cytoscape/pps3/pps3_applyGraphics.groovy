import rainbownodes.*;
import org.pathvisio.visualization.colorset.*;
import cytoscape.*;
import cytoscape.view.*;
import cytoscape.layout.*;
import cytoscape.data.CyAttributes;
import java.awt.Color;
import PPSGlobals;

for(CyNetworkView view in Cytoscape.getNetworkViewMap().values()) {
	String s = view.getTitle();
	//Create the graphics
	Gradient gradient = null;
	if(s.equals("overall")) {
	    gradient = new Gradient()
	            .point(0, Color.WHITE)
	            .point(1E6, Color.RED);
	} else {
	    gradient = new Gradient()
	            .point(-4, Color.GREEN)
	            .point(4, Color.RED);
	}
	
	List<String> visAttr = PPSGlobals.getVisAttr(s);
	Graphic barGraph = new BarGraphic(
	        visAttr, gradient
	);
	
	//Apply the graphics
	GraphicsManager.getInstance().addGraphic(barGraph, view);
	
	//Save a legend
	barGraph.saveLegend(new File(PPSGlobals.outPath, "legend_" + s + ".png"), "Legend for " + s);
}