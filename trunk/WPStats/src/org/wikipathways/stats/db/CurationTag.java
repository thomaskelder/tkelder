package org.wikipathways.stats.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class CurationTag {
	String type;
	int pageId;
	
	public CurationTag(String type, int pageId) {
		if(type.startsWith(CURATION_PREFIX)) {
			type = type.substring(CURATION_PREFIX.length());
		}
		this.type = type;
		this.pageId = pageId;
	}
	
	public String getType() {
		return type;
	}
	
	public int getPageId() {
		return pageId;
	}
	
	public boolean equals(Object obj) {
		if(obj == null) return false;
		if(!(obj instanceof CurationTag)) return false;
		CurationTag t = (CurationTag)obj;
		return type.equals(t.type) && pageId == t.pageId;
	}
	
	public int hashCode() {
		return (type + pageId).hashCode();
	}
	
	public static Set<String> getTagTypes(WPDatabase db, Date date) throws SQLException {
		Set<String> types = new HashSet<String>();
		PreparedStatement pst = db.getPst(pstTagTypes);
		pst.setString(1, WPDatabase.dateToTimestamp(date));
		ResultSet r = pst.executeQuery();
		while(r.next()) {
			types.add(r.getString(1).substring(CURATION_PREFIX.length()));
		}
		return types;
	}
	
	public static Collection<CurationTag> getSnapshot(WPDatabase db, Date date) throws SQLException, ParseException {
		//Select all curation tags from history that have "create" before given date
		Set<CurationTag> tags = new HashSet<CurationTag>();
		
		PreparedStatement pst = db.getPst(pstTagsFromHistory);
		String ts = WPDatabase.dateToTimestamp(date);
		pst.setString(1, ts);
		ResultSet r = pst.executeQuery();
		while(r.next()) {
			tags.add(new CurationTag(r.getString(1), r.getInt(2)));
		}
		r.close();
		
		Set<CurationTag> remove = new HashSet<CurationTag>();
		
		for(CurationTag tag : tags) {
			//For each curation tag, find:
			//- the latest create before date
			//- the latest delete before date
			//Compare, if !delete or create > delete then exists
			String latestDelete = null;
			PreparedStatement pstD = db.getPst(pstLatestActionBefore);
			pstD.setString(1, CURATION_PREFIX + tag.type);
			pstD.setInt(2, tag.pageId);
			pstD.setString(3, "create");
			pstD.setString(4, ts);
			r = pstD.executeQuery();
			if(r.next()) latestDelete = r.getString(1);
			r.close();
			
			if(latestDelete == null) continue;
			
			String latestCreate = null;
			PreparedStatement pstC = db.getPst(pstLatestActionBefore);
			pstC.setString(1, CURATION_PREFIX + tag.type);
			pstC.setInt(2, tag.pageId);
			pstC.setString(3, "create");
			pstC.setString(4, ts);
			r = pstC.executeQuery();
			if(r.next()) latestCreate = r.getString(1);
			r.close();
			
			Date dCreate = WPDatabase.timestampToDate(latestCreate);
			Date dDelete = WPDatabase.timestampToDate(latestDelete);
			if(dDelete.after(dCreate)) remove.add(tag); //Tag was deleted
		}
		tags.removeAll(remove);
		return tags;
	}
	
	static final String CURATION_PREFIX = "Curation:";
	
	static final String pstLatestActionBefore = 
		"SELECT time FROM tag_history " +
		"WHERE tag_name = ? AND page_id = ? " +
		"AND action = ? AND time <= ? " +
		"ORDER BY time DESC";
	
	static final String pstTagsFromHistory = 
		"SELECT tag_name, page_id FROM tag_history " +
		"WHERE tag_name LIKE '" + CURATION_PREFIX + "%' " +
		"AND action = 'create' " +
		"AND time <= ?";
	
	static final String pstTagTypes = 
		"SELECT distinct(tag_name) FROM tag_history " +
		"WHERE tag_name LIKE '" + CURATION_PREFIX + "%' " +
		"AND time <= ?";
}
