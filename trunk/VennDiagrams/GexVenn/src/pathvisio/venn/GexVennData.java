package pathvisio.venn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.visualization.colorset.Criterion;
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
			Criterion c = new Criterion();
			c.setExpression(expr, data.getSampleNames());
			
			Set<Xref> match = new HashSet<Xref>();
			matches.add(match);
			
			int maxRow = data.getNrRow();
			for(int i = 0; i < maxRow; i++) {
				Map<String, Object> sdata = data.getRow(i).getByName();
				Xref xref = data.getRow(i).getXref();
				if(c.evaluate(sdata)) match.add(xref);
			}
		}
		
		VennData<Xref> vdata = new VennData<Xref>(matches);
		return vdata;
	}
}
