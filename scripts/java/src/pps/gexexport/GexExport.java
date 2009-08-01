package pps.gexexport;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.rdb.IDMapperRdb;
import org.pathvisio.debug.Logger;
import org.pathvisio.gex.ReporterData;
import org.pathvisio.gex.SimpleGex;

/**
 * Export a gex file as a data matrix where the reporter ids
 * are converted to an identifier system of choice.
 * @author thomas
 */
public class GexExport {
	private GexExport() {
	}
	
	public static void exportDelimited(
			SimpleGex gex, Writer out, String delimiter, 
			IDMapperRdb gdb, DataSource dataSource, List<String> sampleNames) throws IDMapperException, IOException {
		
		if(sampleNames == null) sampleNames = gex.getSampleNames();
		//Write headers
		out.append("id");
		for(String sn : sampleNames) {
			out.append(delimiter);
			out.append(sn);
		}
		out.append("\n");
		
		int maxRow = gex.getNrRow();
		
		for(int i = 0; i < maxRow; i++) {
			Logger.log.trace("Row " + i + " out of " + maxRow);
			ReporterData row = gex.getRow(i);
			Map<String, Object> sampleData = row.getByName();
			
			StringBuilder dataLineBld = new StringBuilder();
			for(String sn : sampleNames) {
				dataLineBld.append(sampleData.get(sn));
				dataLineBld.append(delimiter);
			}
			String dataLine = dataLineBld.toString();
			
			Xref reporter = row.getXref();
			for(Xref xref : gdb.getCrossRefs(reporter, dataSource)) {
				out.append(xref.getId());
				out.append(delimiter);
				out.append(dataLine);
				out.append("\n");
			}
		}
	}
}
