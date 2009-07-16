package rainbownodes;

import java.util.Iterator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import cytoscape.render.stateful.CustomGraphic;
import cytoscape.view.CyNetworkView;
import ding.view.DNodeView;

public class GraphicsManager {
	private GraphicsManager() {
		registeredGraphics = new ArrayListMultimap<String, Graphic>();
	}
	
	private static GraphicsManager instance;
	
	public static GraphicsManager getInstance() {
		if(instance == null) instance = new GraphicsManager();
		return instance;
	}
	
	private Multimap<String, Graphic> registeredGraphics;
	private Graphic defaultGraphic;
	
	/**
	 * Default graphics to apply to all new network views.
	 * @param graphics
	 */
	public void setDefaultGraphic(Graphic graphic) {
		defaultGraphic = graphic;
	}
	
	public void addGraphic(Graphic graphic, CyNetworkView view) {
		registeredGraphics.put(view.getIdentifier(), graphic);
		
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
		registeredGraphics.remove(view.getIdentifier(), graphic);
		
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
		for(Graphic g : registeredGraphics.get(view.getIdentifier())) {
			removeGraphic(g, view);
		}
	}
}
