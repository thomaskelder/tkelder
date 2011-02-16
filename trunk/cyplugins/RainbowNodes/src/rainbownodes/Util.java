package rainbownodes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import cytoscape.data.CyAttributes;
import cytoscape.data.CyAttributesUtils;

public class Util {
	/**
	 * Utility method to collect all unique values from an attribute
	 */
	public static Set<Object> collectAttributeValues(String name, CyAttributes attributes) {
		Set<Object> values = new HashSet<Object>();
		Map amap = CyAttributesUtils.getAttribute(name, attributes);
		for(Object value : amap.values()) {
			if(value instanceof List) {
				for(Object lv : (List)value) {
					values.add(lv);
				}
			} else {
				values.add(value);
			}
		}
		return values;
	}
	
//	public static Set<Object> collectNodeAttributeValues(String name, CyNetwork network) {
//		Set<Object> values = new HashSet<Object>();
//		for(String n : network.getNode
//		for(Object value : amap.values()) {
//			if(value instanceof List) {
//				for(Object lv : (List)value) {
//					values.add(lv);
//				}
//			} else {
//				values.add(value);
//			}
//		}
//		return values;
//	}
	
	public static void splitAttribute(String name, String newName, String sep, CyAttributes attributes) {
		Map<String, Object> amap = CyAttributesUtils.getAttribute(name, attributes);
		
		for(Entry<String, Object> e : amap.entrySet()) {
			if(e.getValue() != null) {
				List<String> list = Arrays.asList(e.getValue().toString().split(sep));
				attributes.setListAttribute(e.getKey(), newName, list);
			}
		}
	}
}
