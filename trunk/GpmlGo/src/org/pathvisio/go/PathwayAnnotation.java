package org.pathvisio.go;

public class PathwayAnnotation implements GOAnnotation {
	static final int ROUND_TO = 3;
	String id;
	double score;
	
	public PathwayAnnotation(String id, double score) {
		this.id = id;
		this.score = score;
	}
	
	public String getId() {
		return id;
	}
	
	public double getScore() {
		return score;
	}
	
	public String getEvidence() {
		int dec = (int)Math.pow(10, ROUND_TO);
		return "" + (double)(Math.round(score * dec)) / dec;
	}
}
