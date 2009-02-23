package org.pathvisio.go.mapper;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ScoreMatrix<K> {
	Map<String, Map<String, K>> values = new HashMap<String, Map<String, K>>();
	K defValue;
	
	public void setDefault(K value) {
		defValue = value;
	}
	
	public void setScore(String row, String col, K value) {
		Map<String, K> r = values.get(row);
		if(r == null) {
			values.put(row, r = new HashMap<String, K>());
		}
		r.put(col, value);
	}
	
	public K getScore(String row, String col) {
		K v = defValue;
		Map<String, K> r = values.get(row);
		if(r != null) {
			v = r.get(col);
		}
		return v;
	}
	
	public void write(Writer out) throws IOException {
		Set<String> rows = new TreeSet<String>(values.keySet());
		//Collect all columns and assign an index
		Set<String> colSet = new HashSet<String>();
		for(String r : rows) {
			colSet.addAll(values.get(r).keySet());
		}
		List<String> cols = new ArrayList<String>();
		for(String c : colSet) cols.add(c);
		
		//Print header
		for(String c : cols) {
			out.append("\t" + c);
		}
		out.append("\n");
		
		//Print values
		for(String r : rows) {
			String line = r;
			
			String[] colv = new String[cols.size()];
			Arrays.fill(colv, defValue.toString());
			
			Map<String, K> rowv = values.get(r);
			for(String c : rowv.keySet()) {
				colv[cols.indexOf(c)] = getScore(r, c).toString();
			}
			
			for(String cv : colv) {
				line +=  "\t" + cv;
			}
			out.append(line + "\n");
		}
	}
}
