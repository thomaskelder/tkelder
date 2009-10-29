package org.pathvisio.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.rdb.IDMapperRdb;
import org.pathvisio.gex.ReporterData;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.visualization.colorset.Criterion;
import org.pathvisio.visualization.colorset.Criterion.CriterionException;

public class GexUtil {
	public static Set<Xref> extractSignificant(SimpleGex data, String crit) throws CriterionException, IDMapperException {
		Criterion c = new Criterion();
		c.setExpression(crit, data.getSampleNames());
		
		Set<Xref> match = new HashSet<Xref>();
		
		int maxRow = data.getNrRow();
		for(int i = 0; i < maxRow; i++) {
			Map<String, Object> sdata = data.getRow(i).getByName();
			Xref xref = data.getRow(i).getXref();
			if(c.evaluate(sdata)) match.add(xref);
		}
		
		return match;
	}
	
	public static Set<Xref> listXrefs(SimpleGex data) throws CriterionException, IDMapperException {
		Set<Xref> match = new HashSet<Xref>();
		
		int maxRow = data.getNrRow();
		for(int i = 0; i < maxRow; i++) {
			Xref xref = data.getRow(i).getXref();
			match.add(xref);
		}
		
		return match;
	}
	
	public static void exportGenesMatchingCriterion(File file, String crit, SimpleGex data, IDMapperRdb idMapper) throws CriterionException, IOException, IDMapperException {
		Writer out = new BufferedWriter(new FileWriter(file));
		
		//Write header
		out.append("id");
		if(idMapper != null) {
			out.append("\tname");
		}
		Criterion c = new Criterion();
		c.setExpression(crit, data.getSampleNames());
		for(int i = 0; i < data.getNrRow(); i++) {
			ReporterData row = data.getRow(i);
			Map<String, Object> sdata = row.getByName();
			if(c.evaluate(sdata)) {
				//Matches criterion: write to file
				out.append("\n");
				out.append(row.getXref().getId());
				if(idMapper != null) {
					Set<String> attr = idMapper.getAttributes(row.getXref(), "Symbol");
					String symbol = "";
					if(attr.size() > 0) symbol = attr.iterator().next();
					out.append("\t" + symbol);
				}
			}
		}
		
		out.close();
	}
}
