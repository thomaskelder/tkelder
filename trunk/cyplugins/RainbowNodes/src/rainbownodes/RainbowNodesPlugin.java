package rainbownodes;

import giny.view.NodeView;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.swing.SwingConstants;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.data.CyAttributes;
import cytoscape.logger.CyLogger;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.view.CyNetworkView;
import cytoscape.view.CytoscapeDesktop;
import cytoscape.view.cytopanels.CytoPanelImp;

public class RainbowNodesPlugin extends CytoscapePlugin {
	private GraphicsManager graphicsManager;
	private LegendPanel legend;
	
	private static RainbowNodesPlugin instance;
	
	public RainbowNodesPlugin() {
		Cytoscape.getSwingPropertyChangeSupport().addPropertyChangeListener(this);
		graphicsManager = new GraphicsManager();
		instance = this;

		//Testing, set from code
		if(CytoscapeInit.getCyInitParams().getProps().get("test") != null) {
			fromCode();
		}
	}
	
	public void activateLegend() {
		if(legend == null) {
			legend = new LegendPanel(this);
			CytoPanelImp cyPanel = (CytoPanelImp) Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST);
			cyPanel.add("RainbowNodes legend", legend);
			
			Cytoscape.getDesktop().getSwingPropertyChangeSupport().addPropertyChangeListener(CytoscapeDesktop.NETWORK_VIEW_FOCUSED,
					new PropertyChangeListener() {
						public void propertyChange(PropertyChangeEvent e) {
							legend.reset();
						}
					}
			);
		}
	}
	
	public GraphicsManager getGraphicsManager() {
		return graphicsManager;
	}
	
	public static RainbowNodesPlugin getInstance() {
		return instance;
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

		CyNetworkView view = Cytoscape.createNetworkView(network, "test");
		NodeView nv1 = view.getNodeView(n1.getRootGraphIndex());
		nv1.setXPosition(50);
		nv1.setYPosition(50);
		NodeView nv2 = view.getNodeView(n2.getRootGraphIndex());
		nv2.setXPosition(100);
		nv2.setYPosition(100);
		nv2.setBorderWidth(10);
		view.redrawGraph(true, true);
		view.fitContent();

		//Apply the graphics
		getGraphicsManager().addGraphic(pieGraph, view);
		getGraphicsManager().addGraphic(barGraph, view);

		System.err.println(getGraphicsManager().getRegisteredGraphics());
		//Save a legend
		try {
			barGraph.saveLegend(new File("legendtest.png"), "Test legend...");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void saveSessionStateFiles(List<File> pFileList) {
		String tmpDir = System.getProperty("java.io.tmpdir");
		File file = new File(tmpDir, RainbowNodesPlugin.class.getName());

		try {
			graphicsManager.save(file);
			pFileList.add(file);
		} catch(Exception e) {
			e.printStackTrace();
			CyLogger.getLogger().error("Unable to save RainbowNodesPlugin state", e);
		}
	}

	public void restoreSessionState(List<File> pStateFileList) {
		File file = null;
		for(File f : pStateFileList) {
			if(f.getName().endsWith(RainbowNodesPlugin.class.getName())) {
				file = f;
			}
		}
		try {
			if(file != null) {
				graphicsManager = GraphicsManager.load(file);
				System.err.println(graphicsManager.getRegisteredGraphics());
			}
		} catch(Exception e) {
			e.printStackTrace();
			CyLogger.getLogger().error("Unable to restore RainbowNodesPlugin state", e);
		}
	}
}
