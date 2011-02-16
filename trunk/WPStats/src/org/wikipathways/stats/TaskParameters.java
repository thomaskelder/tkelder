package org.wikipathways.stats;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TaskParameters {
	Map<String, String> values = new HashMap<String, String>();
	
	public TaskParameters() {
		setDefaults();
	}
	
	public void setDefaults() {
		register(OUT_PATH, "/home/thomas/code/googlerepo/WPStats/output");
		register(GRAPH_WIDTH, "800");
		register(GRAPH_HEIGHT, "600");
		register(BRIDGE_PATH, "/home/thomas/data/bridgedb/");
	}
	
	public void register(String name, String value) {
		values.put(name, value);
	}
	
	public String getString(String name) { 
		return values.get(name);
	}
	
	public File getFile(String name) {
		return new File(values.get(name));
	}
	
	public int getInt(String name) {
		return Integer.parseInt(values.get(name));
	}
	
	public static final String OUT_PATH = "out";
	public static final String GRAPH_WIDTH = "graph.width";
	public static final String GRAPH_HEIGHT = "graph.height";
	public static final String START = "start";
	public static final String END = "end";
	public static final String BRIDGE_PATH = "bridge.path";
	
}
