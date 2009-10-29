package org.wikipathways.stats.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class WPDatabase {
	String server;
	String database;
	String user;
	String pass;
	
	Connection con;
	
	public WPDatabase(String server, String database, String user, String pass) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		this.server = server;
		this.database = database;
		this.user = user;
		this.pass = pass;
		connect();
	}
	
	public Connection getConnection() {
		return con;
	}
	
	private void connect() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		String url = "jdbc:mysql://" + server + "/" + database;
        Class.forName ("com.mysql.jdbc.Driver").newInstance ();
        con = DriverManager.getConnection (url, user, pass);
        con.setReadOnly(true);
	}
	
	public void closePsts() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		con.close();
		connect();
	}
	
	public Date getWpStart() {
		Calendar c = Calendar.getInstance();
		c.set(2007, 3, 1);
		return c.getTime();
	}
	
	Map<String, PreparedStatement> statements = new HashMap<String, PreparedStatement>();
	
	public PreparedStatement getPst(String sql) throws SQLException {
		PreparedStatement pst = statements.get(con);
		if(pst == null) {
			statements.put(sql, pst = con.prepareStatement(sql));
		}
		return pst;
	}
	
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat ("yyyyMMddHHmmss");
	
	public static String dateToTimestamp(Date date) {
		// turn Date into expected timestamp format, in GMT:
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format(date);
	}
	
	public static Date timestampToDate(String ts) throws ParseException {
		// turn Date into expected timestamp format, in GMT:
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.parse(ts);
	}
	
	public static final int NS_PATHWAY = 102;
}
