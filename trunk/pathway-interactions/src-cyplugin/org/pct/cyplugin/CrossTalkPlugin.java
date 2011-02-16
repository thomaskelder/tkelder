package org.pct.cyplugin;

import giny.model.Node;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.help.UnsupportedOperationException;
import javax.swing.JMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.xml.parsers.ParserConfigurationException;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pathvisio.util.FileUtils;
import org.pathvisio.util.PathwayParser.ParseException;
import org.pct.PathwayCrossTalk;
import org.pct.PathwayInteractionDetails;
import org.pct.PathwayCrossTalk.Args;
import org.pct.model.AttributeKey;
import org.pct.model.Network;
import org.pct.util.ArgsParser;
import org.pct.util.GpmlUtils;
import org.pct.util.PathwayOverlap;
import org.pct.util.ArgsData.DIDMapper;
import org.pct.util.ArgsData.DNetworks;
import org.pct.util.ArgsData.DPathways;
import org.pct.util.ArgsData.DWeights;
import org.xml.sax.SAXException;

import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;
import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.init.CyInitParams;
import cytoscape.logger.CyLogger;
import cytoscape.logger.LogLevel;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.util.CytoscapeAction;
import cytoscape.view.CyMenus;
import cytoscape.view.CyNetworkView;
import cytoscape.view.CyNodeView;
import cytoscape.view.CytoscapeDesktop;
import cytoscape.view.cytopanels.CytoPanelImp;
import cytoscape.view.cytopanels.CytoPanelState;

public class CrossTalkPlugin extends CytoscapePlugin {
	static String PROP_PATHWAY_PATH = "org.pct.PathwayPath";
	static String PROP_ARGS = "org.pct.Args";
	
	JMenu pctMenu;
	PathVisioPanel ppanel;
	
	public CrossTalkPlugin() {
		try {
			Class.forName("org.bridgedb.rdb.IDMapperRdb");
		} catch(Exception e) {
			CyLogger.getLogger().warn("Couldn't load bridgedb driver", e);
		}
		CyInitParams cyPars = CytoscapeInit.getCyInitParams();
		cyPars.getArgs(); //Try to parse the pct args from this array...
		
		//Add a menu action to view details network for selected nodes
		CytoscapeDesktop desktop = Cytoscape.getDesktop();
		CyMenus menu = desktop.getCyMenus();
		JMenu pluginMenu = menu.getOperationsMenu();
		pctMenu = new JMenu("Pathway Crosstalk");
		pctMenu.add(new CytoscapeAction() {
			protected void initialize() {
				putValue(NAME, "Details for selected nodes");
				putValue(SHORT_DESCRIPTION, "Create details network for selected pathway nodes");
				super.initialize();
			}
			
			public void actionPerformed(ActionEvent e) {
				try {
					createDetailsNetwork();
				} catch(Exception ex) {
					CyLogger.getLogger().log(ex.getMessage(), LogLevel.LOG_ERROR, ex);
				}
			}
		});
		pluginMenu.add(pctMenu);
		
		addPathVisioPanel();
	}
	
	private Args getArgs(CyNetwork network) throws ArgumentValidationException {
		String args = Cytoscape.getNetworkAttributes().getStringAttribute(
				network.getIdentifier(), AttributeKey.Args.name()
		);
		if(args == null) {
			//Load from global properties
			args = (String)CytoscapeInit.getProperties().get(PROP_ARGS);
		}
		if(args == null) {
			throw new UnsupportedOperationException("Unable to find PathwayCrossTalk arguments for this network or in global property " + PROP_ARGS);
		}
		return ArgsParser.parse(splitArgs(args), PathwayCrossTalk.Args.class);
	}
	
	private void createDetailsNetwork() throws ArgumentValidationException, IDMapperException, ClassNotFoundException, FileNotFoundException, SAXException, ParserConfigurationException, IOException, ParseException {
		CyNetworkView view = Cytoscape.getCurrentNetworkView();
		
		List<CyNodeView> nodeViews = view.getSelectedNodes();
		
		Args pargs = getArgs(view.getNetwork());
		
		DIDMapper didm = new DIDMapper(pargs);
		DPathways dpws = new DPathways(pargs, didm);
		DNetworks<Xref, String> dnw = new DNetworks<Xref, String>(
				pargs, PathwayCrossTalk.defaultInteractionFactory, Network.xrefFactory, Network.defaultFactory, true
		);
		DWeights dw = new DWeights(pargs, didm);

		//Find the pathways for the selected nodes
		Map<String, Set<Xref>> pathways = new HashMap<String, Set<Xref>>();
		Collection<Node> selected = view.getNetwork().getSelectedNodes();
		for(Node n : selected) {
			String pid = Cytoscape.getNodeAttributes().getStringAttribute(
					n.getIdentifier(), AttributeKey.PathwayId.name()
			);
			if(pid == null) pid = n.getIdentifier();
			
			Set<Xref> p = dpws.getPathways().get(pid);
			if(p == null) {
				System.err.println(dpws.getPathways().keySet());
				throw new FileNotFoundException("Couldn't find pathway " + pid);
			}
			pathways.put(pid, p);
		}
		
		Network<Xref, String> dnetwork = PathwayInteractionDetails.createXrefInteractions(
				pathways, didm, dnw, dw
		);
		PathwayInteractionDetails.addMissingSymbols(dnetwork, 
				GpmlUtils.readSymbols(dpws.getPathwayFiles(), didm.getIDMapper(), didm.getDataSources()));
		
		File tmp = File.createTempFile("pct", ".xgmml");
		FileWriter out = new FileWriter(tmp);
		dnetwork.writeToXGMML(out);
		out.close();
		Cytoscape.createNetworkFromFile(tmp.toString(), true);
		
		Runtime.getRuntime().gc();
	}
	
	private void addPathVisioPanel() {
		ppanel = new PathVisioPanel();
		
		final CytoPanelImp cyPanel = (CytoPanelImp) Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST);
		cyPanel.add("PathVisio", ppanel);
		
		//Add menu action / shortcut to open pathway for selected node(s)
		pctMenu.add(new CytoscapeAction() {
			protected void initialize() {
				super.initialize();
				putValue(NAME, "Open pathways in PathVisio");
				putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("ctrl alt P"));
			}
			
			public void actionPerformed(ActionEvent e) {
				try {
					if(cyPanel.getState() == CytoPanelState.HIDE) {
						cyPanel.setState(CytoPanelState.DOCK);
					}
					openPathwayForSelectedNodes(Cytoscape.getCurrentNetwork());
				} catch(Exception ex) {
					CyLogger.getLogger().log(ex.getMessage(), LogLevel.LOG_ERROR, ex);
					ex.printStackTrace();
				}
			}
		});
		pctMenu.add(new CytoscapeAction() {
			protected void initialize() {
				super.initialize();
				putValue(NAME, "Highligh xref in pathway");
				putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("ctrl alt H"));
			}
			
			public void actionPerformed(ActionEvent e) {
				try {
					if(cyPanel.getState() == CytoPanelState.HIDE) {
						cyPanel.setState(CytoPanelState.DOCK);
					}
					showSelectedNodeInPathway(Cytoscape.getCurrentNetwork());
				} catch(Exception ex) {
					CyLogger.getLogger().log(ex.getMessage(), LogLevel.LOG_ERROR, ex);
					ex.printStackTrace();
				}
			}
		});
	}
	
	private void showSelectedNodeInPathway(CyNetwork network) throws ArgumentValidationException, IDMapperException, ClassNotFoundException {
		Args pargs = getArgs(network);
		DIDMapper didm = new DIDMapper(pargs);
		
		Collection<Node> selected = network.getSelectedNodes();
		if(selected != null && selected.size() > 0) {
			Node n = selected.iterator().next();
			String id = Cytoscape.getNodeAttributes().getStringAttribute(n.getIdentifier(), AttributeKey.XrefId.name());
			DataSource ds = DataSource.getByFullName(
					Cytoscape.getNodeAttributes().getStringAttribute(n.getIdentifier(), AttributeKey.XrefDatasource.name())
			);
			if(id == null || ds == null) throw new IllegalArgumentException(
				"Unable to find xref for node, please set " + AttributeKey.XrefId.name() + " and " + AttributeKey.XrefDatasource.name() + " attributes"	
			);
			Xref x = new Xref(id, ds);
			ppanel.highlightXref(x, didm.getIDMapper());
		}
	}
	
	private void openPathwayForSelectedNodes(CyNetwork network) throws FileNotFoundException, ArgumentValidationException, IDMapperException, ClassNotFoundException {
		String path = CytoscapeInit.getProperties().getProperty(PROP_PATHWAY_PATH);
		if(path == null) path = ".";
		
		Set<File> files = new TreeSet<File>();
		Collection<Node> selected = network.getSelectedNodes();
		System.out.println("Opening pathways for nodes: " + selected);
		for(Node n : selected) {
			String pid = Cytoscape.getNodeAttributes().getStringAttribute(
					n.getIdentifier(), AttributeKey.PathwayId.name()
			);
			if(pid == null) pid = n.getIdentifier();
			
			//Try to find the pathway file
			List<File> allFiles = FileUtils.getFiles(new File(path), true);
			
			String[] names = pid.split("\\" + PathwayOverlap.MERGE_SEP);
			for(String nm : names) {
				for(String s : nm.split("\\" + PathwayInteractionDetails.PATHWAY_ID_SEP)) {
					boolean found = false;
					for(File f : allFiles) {
						if(s.equals(f.getName())) {
							files.add(f);
							found = true;
							break;
						}
					}
					if(!found) throw new FileNotFoundException("Couldn't find pathway " + s);
				}
			}
		}
		
		for(File f : files) {
			ppanel.showPathway(f);
		}
	}
	
	private static String[] splitArgs(String args) {
		String currArg = "";
		boolean currQuote = false;
		List<String> argsList = new ArrayList<String>();
		
		for(int i = 0; i < args.length(); i++) {
			char newChar = args.charAt(i);
			if(isQuote(newChar)) currQuote = !currQuote; //Start/end of a quoted sequence
			
			if(newChar == ' ' && !currQuote) {
				//End of a sequence
				argsList.add(currArg);
				currArg = "";
			} else {
				//Continue sequence
				if(!isQuote(newChar)) currArg += newChar;
			}
		}
		argsList.add(currArg);
		System.err.println("Parsed args: " + argsList);
		return argsList.toArray(new String[argsList.size()]);
	}
	
	private static boolean isQuote(char c) { return c == '"' || c == '\''; }
}
