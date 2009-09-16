package pvtools;

/**
 * Options for importing attributes using 
 * {@link PVToolsPlugin#loadAttributes(org.pathvisio.gex.SimpleGex, AttributeOptions)}
 */
public class AttributeOptions {
	String compareColumn;
	boolean keepMax;
	
	/**
	 * Set the column that will be use to compare duplicate reporters.
	 */
	public AttributeOptions compareColumn(String v) {
		compareColumn = v;
		return this;
	}
	
	/**
	 * Keep the maximum or minimum value in case of duplicate reporters?
	 * If true, then the maximum value will be kept, if false then the minimum
	 * value will be kept. The value of the column set with compareColumn will be used. 
	 */
	public AttributeOptions keepMax(boolean v) {
		keepMax = v;
		return this;
	}
}
