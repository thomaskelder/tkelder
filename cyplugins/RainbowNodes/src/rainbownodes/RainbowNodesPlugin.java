package rainbownodes;

import giny.view.NodeView;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.view.CyNetworkView;

public class RainbowNodesPlugin extends CytoscapePlugin {
	public RainbowNodesPlugin() {
		//Testing, set from code
		//fromCode();
	}
	
	void fromCode() {
		//Create the graphics
		List<String> attributes = Arrays.asList(new String[] {
			"number1",
			"number2",
			"number3"
		});
		Graphic barGraph = new BarGraphic(
				attributes, new Gradient()
					.point(-1, Color.GREEN)
					.point(0, Color.YELLOW)
					.point(1, Color.RED)
		);
		
		Graphic pieGraph = new PieGraphic(
				attributes, new Gradient()
				.point(-1, Color.BLUE)
				.point(1, Color.YELLOW)	
		);
		
		//Create a test networkview
		CyNetwork network = Cytoscape.createNetwork("test");
		CyNode n1 = Cytoscape.getCyNode("test1", true);
		CyNode n2 = Cytoscape.getCyNode("test2", true);
		network.addNode(n1);
		network.addNode(n2);
		
		CyAttributes attr = Cytoscape.getNodeAttributes();
		attr.setAttribute(n1.getIdentifier(), "number1", 0.5);
		attr.setAttribute(n1.getIdentifier(), "number2", -0.5);
		attr.setAttribute(n1.getIdentifier(), "number3", 0.8);
		attr.setAttribute(n2.getIdentifier(), "number1", -0.7);
		attr.setAttribute(n2.getIdentifier(), "number2", 1d);
		attr.setAttribute(n2.getIdentifier(), "number3", -1d);

		CyNetworkView view = Cytoscape.createNetworkView(network);
		NodeView nv1 = view.getNodeView(n1.getRootGraphIndex());
		nv1.setXPosition(50);
		nv1.setYPosition(50);
		NodeView nv2 = view.getNodeView(n2.getRootGraphIndex());
		nv2.setXPosition(100);
		nv2.setYPosition(100);
		view.redrawGraph(true, true);
		view.fitContent();
		
		//Apply the graphics
		GraphicsManager.getInstance().addGraphic(pieGraph, view);
		GraphicsManager.getInstance().addGraphic(barGraph, view);
		
		//Save a legend
		try {
			barGraph.saveLegend(new File("legendtest.png"), "Test legend...");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
