package pps.interaction;

import java.util.HashMap;
import java.util.Map;

public class Node {
	String id;
	
	private Node(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
	public boolean equals(Object o) {
		if(o instanceof Node) {
			return id.equals(((Node)o).id);
		}
		return false;
	}
	
	public int hashCode() {
		return id.hashCode();
	}
	
	private static Map<String, Node> instances = new HashMap<String, Node>();
	
	public static Node getInstance(String id) {
		if(id == null) throw new IllegalArgumentException("id can't be null");
		
		Node n = instances.get(id);
		if(n == null) {
			instances.put(id, n = new Node(id));
		}
		return n;
	}
}
