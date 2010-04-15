package org.wikipathways.stats.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
		Map<String, Integer> counts = new HashMap<String, Integer>();

		PreparedStatement pst = db.getPst(pstIpCount);

		Set<String> ips = getIps(db, start, end);
		int i = 0;
		for(String ip : ips) {
			System.out.println("Querying " + ip + " (" + i++ + " / " + ips.size() + ")");
			counts.put(ip, getCountsForIp(db, start, end, ip));
		}

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
		for(String op : operations) {
			int c = getCountsForOperation(db, start, end, "%" + op + "%");
			counts.put(op, c);
			System.out.println("Processing operation " + op + " (" + c + ")");
		}

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
	
	static final String pstOperationCount = 
		"SELECT count(operation) FROM webservice_log WHERE operation LIKE ? AND request_timestamp >= ? AND request_timestamp < ?";
	
	
}
