package pps.interaction;

public class Edge {
	Node node1;
	Node node2;
	String type;
	
	public Edge(Node n1, Node n2, String type) {
		this.node1 = n1;
		this.node2 = n2;
		this.type = type;
	}

	public Node getNode1() {
		return node1;
	}
	
	public Node getNode2() {
		return node2;
	}
	
	public String getType() {
		return type;
	}
	
	public int hashCode() {
		return ("" + node1 + node2 + type).hashCode();
	}
	
	public boolean equals(Object o) {
		return ("" + node1 + node2 + type).equals(o);
	}
}
