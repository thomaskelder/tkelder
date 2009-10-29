package pathvisio.venn;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.utils.GexUtil;
import org.pathvisio.visualization.colorset.Criterion.CriterionException;

import venn.VennData;

/**
 * Create VennData instance from a PathVisio expression
 * dataset and several criteria
 * @author thomas
 */
public class GexVennData {
	public static VennData<Xref> create(SimpleGex data, String...criteria) throws IDMapperException, CriterionException {
		if(criteria.length < 2 || criteria.length > 3) {
			throw new IllegalArgumentException("Number of criteria must be 2 or 3");
		}
		
		List<Set<Xref>> matches = new ArrayList<Set<Xref>>();
		
		for(String expr : criteria) {
			matches.add(GexUtil.extractSignificant(data, expr));
		}
		
		VennData<Xref> vdata = new VennData<Xref>(matches);
		return vdata;
	}
}
