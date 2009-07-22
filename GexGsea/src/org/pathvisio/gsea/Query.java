package org.pathvisio.gsea;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.pathvisio.debug.Logger;
import org.pathvisio.gex.Sample;
import org.pathvisio.gex.SimpleGex;

import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.DefaultDataset;

public class Query {
	private static final String F_ENSID = "ensId";
	private static final String F_ID = "id";
	private static final String F_CODE = "code";
	private static final String F_GROUPID = "groupId";
	private static final String F_SAMPLEID = "idSample";
	private static final String F_DATA = "data";
	
	private Connection con;
	
	private PreparedStatement pstData;
	private PreparedStatement pstDataSample;
	
	public Query(Connection con) throws SQLException {
		this.con = con;
		pstData = con.prepareStatement(
				"SELECT data FROM expression WHERE ensId = ? AND idSample = ?"
		);
		pstDataSample = con.prepareStatement(
				"SELECT data, idSample FROM expression WHERE ensId = ? AND id = ? AND code = ? AND groupId = ?"
		);
	}
	
	/**
	 * Get the data for all numeric samples
	 */
	public static Dataset getDataUnique(SimpleGex gex) throws SQLException {
		Query q = new Query(gex.getCon());
		return q.getDataUnique(gex.getDbName(), gex.getSamples(Types.REAL));
	}
	
	/**
	 * Get the data for the given samples as a matrix, averaging duplicate gene ids
	 */
	public Dataset getDataUnique(String name, List<Sample> samples) throws SQLException {
		Logger.log.info("Start querying dataset");
		
		Logger.log.info("Creating rows");
		Set<String> rows = new HashSet<String>();
		
		ResultSet r = con.createStatement().executeQuery(
			"SELECT ensId FROM expression"
		);
		
		while(r.next()) {
			rows.add(r.getString(F_ENSID));
		}
		
		Matrix m = new Matrix(rows.size(), samples.size());
		
		HashMap<Integer, Integer> sample2index = new HashMap<Integer, Integer>();
		List<String> colNames = new ArrayList<String>();
		
		for(int i = 0; i < samples.size(); i++) {
			sample2index.put(samples.get(i).getId(), i);
			colNames.add(samples.get(i).getName());
		}
		
		Logger.log.info("Fetching sample data");
		
		List<String> rowNames = new ArrayList<String>();
		
		int i = 0;
		for(String row : rows) {
			if(i % 100 == 0) Logger.log.info("\tFetching row " + i + " out of " + rows.size());

			for(int sid : sample2index.keySet()) {
				pstData.setString(1, row);
				pstData.setInt(2, sid);
				//Fetch and average data for this sample
				r = pstData.executeQuery();
				int nr = 0;
				float avg = 0;
				
				while(r.next()) {
					nr++;
					float d = r.getFloat(F_DATA);
					if(Float.isNaN(d)) {
						Logger.log.info("NaN found for " + row + ", " + sid);
					} else {
						avg += r.getFloat(F_DATA);
					}
				}
				
				avg /= nr;
				m.setElement(i, sample2index.get(sid), avg);
			}
			rowNames.add(row);
			i++;
		}

		Dataset d = new DefaultDataset(
			name, 
			m, 
			rowNames.toArray(new String[rowNames.size()]), 
			colNames.toArray(new String[colNames.size()]), 
			true //TODO: find out what this parameter means!
		);
		
		Logger.log.info("Done querying dataset");
		return d;
	}
	
	/**
	 * Get the dataset for the given samples, without averaging duplicate gene ids
	 */
	public Dataset getDataNonUnique(String name, List<Sample> samples) throws SQLException {
		Logger.log.info("Start querying dataset");
		
		Logger.log.info("Creating rows");
		Map<String, String[]> rows = new HashMap<String, String[]>();
		
		ResultSet r = con.createStatement().executeQuery(
			"SELECT ensId, id, code, groupId FROM expression"
		);
		
		while(r.next()) {
			String[] row = new String[4];
			row[0] = r.getString(F_ENSID);
			row[1] = r.getString(F_ID);
			row[2] = r.getString(F_CODE);
			row[3] = r.getString(F_GROUPID);
			rows.put(Arrays.toString(row), row);
		}
		
		Matrix m = new Matrix(rows.size(), samples.size());
		
		HashMap<Integer, Integer> sample2index = new HashMap<Integer, Integer>();
		List<String> colNames = new ArrayList<String>();
		
		for(int i = 0; i < samples.size(); i++) {
			sample2index.put(samples.get(i).getId(), i);
			colNames.add(samples.get(i).getName());
		}
		
		Logger.log.info("Fetching sample data");
		
		List<String> rowNames = new ArrayList<String>();
		
		int i = 0;
		for(String[] row : rows.values()) {
			if(i % 100 == 0) Logger.log.info("\tFetching row " + i + " out of " + rows.size());
			
			pstDataSample.setString(1, row[0]);
			pstDataSample.setString(2, row[1]);
			pstDataSample.setString(3, row[2]);
			pstDataSample.setInt(4, Integer.parseInt(row[3]));
			r = pstDataSample.executeQuery();
			
			while(r.next())	{
				int sid = r.getInt(F_SAMPLEID);
				if(sample2index.containsKey(sid)) {
					float value = r.getFloat(F_DATA);
					m.setElement(i, sample2index.get(sid), value);
				}
			}
			rowNames.add(row[0]);
			i++;
		}

		Dataset d = new DefaultDataset(
			name, 
			m, 
			rowNames.toArray(new String[rowNames.size()]), 
			colNames.toArray(new String[colNames.size()]), 
			true //TODO: find out what this parameter means!
		);
		
		Logger.log.info("Done querying dataset");
		return d;
	}
	
}
