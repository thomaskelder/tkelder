package rainbownodes;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import cytoscape.Cytoscape;
import cytoscape.render.stateful.CustomGraphic;
import cytoscape.view.CyNetworkView;
import ding.view.DNodeView;

public class GraphicsManager implements Serializable {
	private static final long serialVersionUID = -4706400045763907067L;

	GraphicsManager() {
		registeredGraphics = new ArrayListMultimap<String, Graphic>();
	}
	
	static GraphicsManager load(File file) throws IOException, ClassNotFoundException {
		InputStream in = new FileInputStream(file);
		XStream xstream = new XStream(new DomDriver());
		GraphicsManager graphicsMgr = (GraphicsManager)xstream.fromXML(in);
		in.close();
		Set<String> viewIds = new HashSet<String>(graphicsMgr.registeredGraphics.keySet());
		for(String viewId : viewIds) {
			CyNetworkView view = Cytoscape.getNetworkView(viewId);
			Collection<Graphic> graphics = new ArrayList<Graphic>(graphicsMgr.registeredGraphics.get(viewId));
			for(Graphic g : graphics) {
				graphicsMgr.addGraphic(g, view);
			}
		}
		return graphicsMgr;
	}
	
	GraphicsManager cloneWithTitles() {
		GraphicsManager clone = new GraphicsManager();
		for(String viewId : registeredGraphics.keySet()) {
			CyNetworkView view = Cytoscape.getNetworkView(viewId);
			for(Graphic g : registeredGraphics.get(viewId)) {
				clone.registeredGraphics.put(view.getNetwork().getTitle(), g);
			}
		}
		return clone;
	}
	
	void save(File file) throws IOException {
		//Because network ids are changed to the title after saving (why?!), we need
		//to rebuild the map based on titles. Therefore we call cloneWithTitles instead
		//of passing this.
		OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		XStream xstream = new XStream(new DomDriver());
		xstream.toXML(cloneWithTitles(), out);
		out.close();
	}
	
	/**
	 * @deprecated use RainbowNodesPlugin#getGraphicsManager instead
	 */
	public static GraphicsManager getInstance() {
		return RainbowNodesPlugin.getInstance().getGraphicsManager();
	}
	
	private Multimap<String, Graphic> registeredGraphics;
	
	Multimap<String, Graphic> getRegisteredGraphics() {
		return registeredGraphics;
	}
	
	void setRegisteredGraphics(
			Multimap<String, Graphic> registeredGraphics) {
		this.registeredGraphics = registeredGraphics;
	}
	
	public void addGraphic(Graphic graphic, CyNetworkView view) {
		registeredGraphics.put(view.getNetwork().getIdentifier(), graphic);
		
		//Add the custom graphics for each node view
		Iterator<DNodeView> it = view.getNodeViewsIterator();
		while(it.hasNext()) {
			DNodeView nv = it.next();
			CustomGraphic cg = graphic.asCustomGraphic(nv);
			nv.addCustomGraphic(cg);
		}
		view.redrawGraph(true, true);
	}

	public void removeGraphic(Graphic graphic, CyNetworkView view) {
		registeredGraphics.remove(view.getNetwork().getIdentifier(), graphic);
		
		//Remove the custom graphics for each node view
		Iterator<DNodeView> it = view.getNodeViewsIterator();
		while(it.hasNext()) {
			DNodeView nv = it.next();
			CustomGraphic cg = graphic.asCustomGraphic(nv);
			nv.removeCustomGraphic(cg);
		}
		view.redrawGraph(true, true);
	}
	
	/**
	 * Removes all associated Graphic objects from the view.
	 * @param view
	 */
	public void reset(CyNetworkView view) {
		for(Graphic g : registeredGraphics.get(view.getNetwork().getIdentifier())) {
			removeGraphic(g, view);
		}
	}
}