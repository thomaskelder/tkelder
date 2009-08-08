package org.pathvisio.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

import org.bridgedb.IDMapperException;
import org.bridgedb.rdb.IDMapperRdb;
import org.pathvisio.gex.ReporterData;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.visualization.colorset.Criterion;
import org.pathvisio.visualization.colorset.Criterion.CriterionException;

public class GexUtil {
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
