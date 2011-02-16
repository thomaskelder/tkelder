package org.wikipathways.stats.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Webservice {
	public static int getCounts(WPDatabase db, Date start, Date end) throws SQLException {
		PreparedStatement pst = db.getPst(pstCount);

		pst.setString(1, WPDatabase.dateToTimestamp(start));
		pst.setString(2, WPDatabase.dateToTimestamp(end));
		ResultSet r = pst.executeQuery();
		r.next();
		int count = r.getInt(1);
		r.close();
		return count;
	}

	public static int getCountsForIp(WPDatabase db, Date start, Date end, String ip) throws SQLException {
		PreparedStatement pst = db.getPst(pstIpCount);
		pst.setString(1, ip);
		pst.setString(2, WPDatabase.dateToTimestamp(start));
		pst.setString(3, WPDatabase.dateToTimestamp(end));
		ResultSet r = pst.executeQuery();
		r.next();
		int count = r.getInt(1);
		r.close();
		return count;
	}
	
	public static Map<String, Integer> getCountsPerIp(WPDatabase db, Date start, Date end) throws SQLException {
		Map<String, Integer> counts = new LinkedHashMap<String, Integer>();

		PreparedStatement pst = db.getPst(pstIpFreq);
		
		pst.setString(1, WPDatabase.dateToTimestamp(start));
		pst.setString(2, WPDatabase.dateToTimestamp(end));
		
		ResultSet r = pst.executeQuery();
		while(r.next()) {
			String ip = r.getString(1);
			int count = r.getInt(2);
			counts.put(ip, count);
		}
		r.close();
		
		return counts;
	}

	public static Map<String, Integer> getCountsPerOperation(WPDatabase db, Date start, Date end) throws SQLException {
		Map<String, Integer> counts = new TreeMap<String, Integer>();

		String[] operations = new String[] {
				"createPathway",
				"findInteractions",
				"findPathwaysByLiterature",
				"findPathwaysByText",
				"findPathwaysByXref",
				"getColoredPathway",
				"getCurationTagsByName",
				"getCurationTagHistory",
				"getCurationTags",
				"getPathway",
				"getPathwayAs",
				"getPathwayHistory",
				"getPathwayInfo",
				"getRecentChanges",
				"getXrefList",
				"listOrganisms",
				"listPathways",
				"login",
				"removeCurationTag",
				"saveCurationTag",
				"updatePathway",
		};
		
		PreparedStatement pst = db.getPst(pstOperationFreq);
		
		pst.setString(1, WPDatabase.dateToTimestamp(start));
		pst.setString(2, WPDatabase.dateToTimestamp(end));
		
		ResultSet r = pst.executeQuery();
		while(r.next()) {
			String command = r.getString(1);
			int count = r.getInt(2);
			
			//Match the right operation
			boolean match = false;
			for(String operation : operations) {
				if(command.toLowerCase().contains(operation.toLowerCase())) {
					if(counts.containsKey(operation)) {
						counts.put(operation, counts.get(operation) + count);
					} else {
						counts.put(operation, count);
					}
					match = true;
				}
			}
			if(!match) counts.put("other (WSDL or incorrect request)", count);
		}
		r.close();
		
		return counts;
	}
	
	public static int getCountsForOperation(WPDatabase db, Date start, Date end, String op) throws SQLException {
		PreparedStatement pst = db.getPst(pstOperationCount);
		pst.setString(1, op);
		pst.setString(2, WPDatabase.dateToTimestamp(start));
		pst.setString(3, WPDatabase.dateToTimestamp(end));
		ResultSet r = pst.executeQuery();
		r.next();
		int count = r.getInt(1);
		r.close();
		return count;
	}

	public static Set<String> getIps(WPDatabase db, Date start, Date end) throws SQLException {
		Set<String> ips = new HashSet<String>();

		String tsStart = WPDatabase.dateToTimestamp(start);
		String tsEnd = WPDatabase.dateToTimestamp(end);

		PreparedStatement pst = db.getPst(pstIps);
		pst.setString(1, tsStart);
		pst.setString(2, tsEnd);
		ResultSet r = pst.executeQuery();
		while(r.next()) {
			ips.add(r.getString(1));
		}
		r.close();

		return ips;
	}

	static final String pstCount = 
		"SELECT count(ip) FROM webservice_log WHERE request_timestamp >= ? AND request_timestamp < ?";
	
	static final String pstIps = 
		"SELECT distinct(ip) FROM webservice_log WHERE request_timestamp >= ? AND request_timestamp < ?";

	static final String pstIpCount = 
		"SELECT count(ip) FROM webservice_log WHERE ip = ? AND request_timestamp >= ? AND request_timestamp < ?";
	
	static final String pstIpFreq = 
		"SELECT ip, count(ip) FROM webservice_log WHERE request_timestamp >= ? AND request_timestamp < ? " +
		"GROUP BY ip ORDER BY count(ip) DESC";
	
	static final String pstOperationFreq = 
		"SELECT operation, count(operation) FROM webservice_log WHERE request_timestamp >= ? AND request_timestamp < ? " +
		"GROUP BY operation ORDER BY count(operation) DESC";
	
	static final String pstOperationCount = 
		"SELECT count(operation) FROM webservice_log WHERE operation LIKE ? AND request_timestamp >= ? AND request_timestamp < ?";
	
	
}
