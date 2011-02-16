package pvtools;

import java.util.ArrayList;
import java.util.List;

/**
 * Options for importing attributes using 
 * {@link PVToolsPlugin#loadAttributes(org.pathvisio.gex.SimpleGex, AttributeOptions)}
 */
public class AttributeOptions {
	List<String> excludeCols = new ArrayList<String>();
	
	/**
	 * Specify the columns that will be excluded from the attribute import.
	 */
	public AttributeOptions excludeCols(List<String> v) {
		excludeCols = v;
		return this;
	}
}
