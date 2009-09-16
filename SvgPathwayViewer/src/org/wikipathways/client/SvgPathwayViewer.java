package org.wikipathways.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class SvgPathwayViewer implements EntryPoint {
	String svgRef;
	
	public void onModuleLoad() {
		initParameters();
		DockPanel mainPanel = new DockPanel();
		
		SvgBrowser svgBrowser = new SvgBrowser(svgRef);
		mainPanel.add(svgBrowser, DockPanel.CENTER);
		
		mainPanel.setSize("95%", "95%");
		RootPanel.get().add(mainPanel);
	}
	
	private void initParameters() {
		Dictionary dict = Dictionary.getDictionary("SvgPathwayViewer");
		svgRef = dict.get("svgRef");
	}
}
