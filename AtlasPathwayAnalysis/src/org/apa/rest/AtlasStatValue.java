package org.apa.rest;

public class AtlasStatValue {
	public enum Direction { UP, DOWN }
	
	private Direction expression;
	private double pvalue;
	private double tstat;
	
	public Direction getExpression() {
		return expression;
	}
	public void setExpression(Direction expression) {
		this.expression = expression;
	}
	public double getPvalue() {
		return pvalue;
	}
	public void setPvalue(double pvalue) {
		this.pvalue = pvalue;
	}
	public double getTstat() {
		return tstat;
	}
	public void setTstat(double tstat) {
		this.tstat = tstat;
	}
	
}
