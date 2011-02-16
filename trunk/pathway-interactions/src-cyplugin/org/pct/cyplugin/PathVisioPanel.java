package org.pct.cyplugin;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pathvisio.Engine;
import org.pathvisio.debug.Logger;
import org.pathvisio.gui.swing.CommonActions;
import org.pathvisio.gui.swing.MainPanel;
import org.pathvisio.gui.swing.PvDesktop;
import org.pathvisio.gui.swing.SwingEngine;
import org.pathvisio.gui.swing.SwingEngine.Browser;
import org.pathvisio.model.ObjectType;
import org.pathvisio.model.PathwayElement;
import org.pathvisio.plugins.gex.GexPlugin.SelectGexAction;
import org.pathvisio.preferences.PreferenceManager;
import org.pathvisio.view.Graphics;
import org.pathvisio.view.MIMShapes;
import org.pathvisio.view.VPathway;

import components.ButtonTabComponent;

import edu.stanford.ejalbert.BrowserLauncher;

public class PathVisioPanel extends JPanel {
	JTabbedPane tabbedPane;
	Map<File, PathwayPanel> panels = new HashMap<File, PathwayPanel>();
	
	public PathVisioPanel() {
		PreferenceManager.init();
		MIMShapes.registerShapes();
		
		tabbedPane = new JTabbedPane();
		setLayout(new BorderLayout());
		add(tabbedPane, BorderLayout.CENTER);
	}
	
	public void highlightXref(Xref x, IDMapper idm) throws IDMapperException {
		Set<Xref> mapped = new HashSet<Xref>();
		mapped.add(x);
		if(idm != null) mapped.addAll(idm.mapID(x));
		for(PathwayPanel pp : panels.values()) {
			VPathway vp = pp.pvDesktop.getSwingEngine().getEngine().getActiveVPathway();
			vp.resetHighlight();
			
			Graphics firstFound = null;
			for(PathwayElement pe : vp.getPathwayModel().getDataObjects()) {
				if(pe.getObjectType() == ObjectType.DATANODE) {
					Xref dx = pe.getXref();
					if(dx != null && mapped.contains(dx)) {
						Graphics g = vp.getPathwayElementView(pe);
						if(firstFound == null) firstFound = g;
						g.highlight();
					}
				}
			}
			if(firstFound != null) {
				JScrollPane sp = pp.pvDesktop.getSwingEngine().getApplicationPanel().getScrollPane();
				sp.getVerticalScrollBar().setValue(0);
				sp.getHorizontalScrollBar().setValue(0);
				vp.getWrapper().scrollTo(firstFound.getVBounds().getBounds());
			}
		}
	}
	
	public void showPathway(final File f) {
		if(!panels.containsKey(f)) {
			PathwayPanel pp = new PathwayPanel();
			panels.put(f, pp);
			tabbedPane.addTab(f.getName(), pp);
			
			ButtonTabComponent tab = new ButtonTabComponent(tabbedPane);
			tab.getButton().addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					//Remove the pathway from the map
					panels.get(f).dispose();
					panels.remove(f);
				}
			});
			tabbedPane.setTabComponentAt(
					tabbedPane.indexOfComponent(pp), tab);
			pp.init(f);
			validate();
		}
		tabbedPane.setSelectedComponent(panels.get(f));
	}
	
	class PathwayPanel extends JPanel {
		PvDesktop pvDesktop;
		
		void dispose() {
			pvDesktop.getSwingEngine().getGdbManager().removeGdbEventListener(pvDesktop);
			pvDesktop.getSwingEngine().getEngine().removeApplicationEventListener(pvDesktop);
			pvDesktop.getVisualizationManager().removeListener(pvDesktop);
			pvDesktop.getVisualizationManager().dispose();
			pvDesktop.getSwingEngine().getEngine().dispose();
			pvDesktop.getSwingEngine().dispose();
		}
		
		void init(File pathwayFile) {
			Engine engine = new Engine();
			SwingEngine swingEngine = new SwingEngine(engine);
			swingEngine.setUrlBrowser(new Browser() {
				public void openUrl(URL url) {
					try {
						BrowserLauncher b = new BrowserLauncher(null);
						b.openURLinBrowser(url.toString());
					} catch (Exception ex) {
						Logger.log.error ("Couldn't open url '" + url + "'", ex);
					}
				}
			});
			pvDesktop = new PvDesktop (swingEngine);
			swingEngine.getGdbManager().initPreferred();

			Set<Action> hideActions = new HashSet<Action>();
			CommonActions actions = swingEngine.getActions();
			hideActions.add(actions.copyAction);
			hideActions.add(actions.pasteAction);
			hideActions.add(actions.importAction);
			hideActions.add(actions.saveAction);
			hideActions.add(actions.saveAsAction);
			hideActions.add(actions.standaloneSaveAction);
			hideActions.add(actions.standaloneSaveAsAction);
			hideActions.add(actions.undoAction);
			for(Action[] aa : actions.newElementActions) for(Action a : aa) hideActions.add(a);
			for(Action a : actions.layoutActions) hideActions.add(a);
			for(Action a : actions.newInteractionActions) hideActions.add(a);
			for(Action a : actions.newMIMInteractionActions) hideActions.add(a);
			for(Action a : actions.newRLInteractionActions) hideActions.add(a);
			
			MainPanel mainPanel = new MainPanel(swingEngine, hideActions);
			mainPanel.createAndShowGUI();

			mainPanel.getToolBar().add(new SelectGexAction(pvDesktop));
			
			setLayout(new BorderLayout());
			add(mainPanel, BorderLayout.CENTER);
			
			swingEngine.setFrame((JFrame) SwingUtilities.getRoot(PathVisioPanel.this));
			swingEngine.setApplicationPanel(mainPanel);

			pvDesktop.initPlugins(Arrays.asList(new String[] {
					"org.pathvisio.visualization.plugins.VisualizationPlugin",
					"org.pathvisio.plugins.gex.GexPlugin"
			}));
			
			swingEngine.openPathway(pathwayFile);
			swingEngine.getEngine().getActiveVPathway().setEditMode(false);
			pvDesktop.loadGexCache();
			
			//Hack to hide the sidepanel by default
			BasicSplitPaneUI ui = (BasicSplitPaneUI)mainPanel.getSplitPane().getUI();
			BasicSplitPaneDivider divider = ui.getDivider();
			JButton button = (JButton)divider.getComponent(1);
			button.doClick();
		}
	}
}
