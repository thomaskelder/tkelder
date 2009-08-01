package pathvisio.venn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import venn.VennData;

/**
 * Create VennData instance from a PathVisio expression
 * dataset and several criteria
 * @author thomas
 */
public class ZScoreVennData {
	public static VennData<String> create(double threshold, Map<String, Double>...zscores) {
		List<Set<String>> sets = new ArrayList<Set<String>>();
		
		for(Map<String, Double> r : zscores) {
			Set<String> pathways = new HashSet<String>();
			for(String pw : r.keySet()) {
				if(r.get(pw) >= threshold) {
					pathways.add(pw);
				}
			}
			sets.add(pathways);
		}
		
		return new VennData<String>(sets);
	}
}
