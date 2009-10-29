package org.wikipathways.stats.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class User {
	int id;
	String name;
	String fullName;
	
	public User(int id, String name, String fullName) {
		this.id = id;
		this.name = name;
		this.fullName = fullName;
	}
	
	public String getFullName() {
		return fullName;
	}
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public int getEditCount(WPDatabase db) throws SQLException {
		PreparedStatement pst = db.getPst(pstGetUserActive);
		pst.setInt(1, id);
		pst.setInt(2, WPDatabase.NS_PATHWAY);
		ResultSet r = pst.executeQuery();
		r.next();
		int count = r.getInt(1);
		r.close();
		return count;
	}
	
	public static Collection<User> getSnapshot(WPDatabase db, Date from, Date to) throws SQLException, ParseException {
		Set<User> users = new HashSet<User>();
		
		//Select all users that have registered after from date
		PreparedStatement pst = db.getPst(pstGetUsersFrom);
		String ts = WPDatabase.dateToTimestamp(from);
		pst.setString(1, ts);
		ResultSet r = pst.executeQuery();
		while(r.next()) {
			users.add(new User(r.getInt(1), r.getString(2), r.getString(3)));
		}
		r.close();
		
		return users;
	}
	
	static final String pstGetUsersFrom = 
		"SELECT user_id, user_name, user_real_name FROM user WHERE user_registration <= ?";
	
	static final String pstGetUserActive = 
		"SELECT COUNT(r.rev_id) FROM revision AS r JOIN page AS p " +
		"WHERE r.rev_user = ? AND p.page_namespace = ?";
}
