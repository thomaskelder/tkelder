package org.wikipathways.stats.db;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.pathvisio.util.PathwayParser.ParseException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class PathwayInfo {
	WPDatabase db;
	String pathwayId;
	int pageId;
	int revision;
	
	boolean dirty;

	String gpml;
	PathwayParser parsedGpml;
	
	public PathwayInfo(WPDatabase db, String pathwayId, int pageId, int revision) {
		this.db = db;
		this.pathwayId = pathwayId;
		this.pageId = pageId;
		this.revision = revision;
		markDirty();
	}
	
	public int getPageId() {
		return pageId;
	}
	
	public String getPathwayId() {
		return pathwayId;
	}
	
	private void markDirty() {
		gpml = null;
		parsedGpml = null;
	}

	public void setRevision(int revision) {
		this.revision = revision;
	}
	
	public boolean isDeleted() throws SQLException {
		String gpml = getGpml();
		return gpml != null && gpml.startsWith(PREFIX_DELETED);
	}
	
	public boolean isRedirect() throws SQLException {
		String gpml = getGpml();
		return gpml != null && gpml.startsWith(PREFIX_REDIRECT);
	}
	
	public String getSpecies() throws SQLException, ParseException, SAXException, IOException {
		PathwayParser parsedGpml = getParsedGpml();
		if(parsedGpml != null) {
			return parsedGpml.getOrganism();
		} else {
			return null;
		}
	}
	
	public String getGpml() throws SQLException {
		if(gpml == null) {
			PreparedStatement pst = db.getPst(pstGetGpml);
			pst.setInt(1, revision);
			ResultSet r = pst.executeQuery();
			if(r.next()) {
				gpml = r.getString(1);
			} else {
				gpml = null;
			}
			r.close();
		}
		return gpml;
	}
	
	public PathwayParser getParsedGpml() throws SQLException, ParseException, SAXException, IOException {
		String gpml = getGpml();
		if(parsedGpml == null && gpml != null && !gpml.equals("") && !isDeleted() && !isRedirect()) {
			XMLReader xmlReader = XMLReaderFactory.createXMLReader();
			parsedGpml = new PathwayParser(new StringReader(gpml), xmlReader);
		}
		return parsedGpml;
	}
	
	private int findClosestRevision(String ts) throws SQLException {
		PreparedStatement pst = db.getPst(pstClosestRevision);
		pst.setString(1, ts);
		pst.setInt(2, pageId);
		ResultSet r = pst.executeQuery();
		r.next();
		int rev = r.getInt(1);
		r.close();
		return rev;
	}
	
	public static Set<PathwayInfo> getSnapshot(WPDatabase db, Date date) throws SQLException {
		//Get a snapshot of all pathways at the given date
		Set<PathwayInfo> pathways = new HashSet<PathwayInfo>();
		
		PreparedStatement pst = db.getPst(pstPathwaySnapshot);
		String ts = WPDatabase.dateToTimestamp(date);
		pst.setString(1, ts);
		ResultSet r = pst.executeQuery();
		while(r.next()) {
			PathwayInfo i = new PathwayInfo(db, r.getString(2), r.getInt(1), 0);
			i.setRevision(i.findClosestRevision(ts));
			if(!i.isDeleted() && !i.isRedirect()) pathways.add(i);
		}
		r.close();
		return pathways;
	}
	
	public static Set<String> getSpecies(WPDatabase db) throws SQLException {
		Set<String> species = new HashSet<String>();
		
		PreparedStatement pst = db.getPst(pstAllSpecies);
		ResultSet r = pst.executeQuery();
		while(r.next()) {
			species.add(r.getString(1));
		}
		r.close();
		return species;
	}
	
	public boolean equals(Object obj) {
		if(obj == null) return false;
		if(!(obj instanceof PathwayInfo)) return false;
		PathwayInfo pwi = (PathwayInfo)obj;
		return pathwayId.equals(pwi) && revision == pwi.revision;
	}
	
	public int hashCode() {
		return (pathwayId + revision).hashCode();
	}
	
	public String toString() {
		return pathwayId + "@" + revision;
	}

	static final String pstAllSpecies = 
		"SELECT DISTINCT(tag_text) FROM tag WHERE tag_name = 'cache-organism'";
	
	static final String pstPathwaySnapshot = 
		"SELECT DISTINCT(r.rev_page) AS page_id, p.page_title as page_title " +
		"FROM revision AS r JOIN page AS p " +
		"ON r.rev_page = p.page_id " +
		"WHERE r.rev_timestamp <= ? " +
		"AND p.page_is_redirect = 0 " +
		"AND p.page_namespace = " + WPDatabase.NS_PATHWAY;
	
	static final String pstClosestRevision = 
		"SELECT MAX(rev_id) FROM revision " +
		"WHERE rev_timestamp <= ? AND rev_page = ?";
	
	static final String pstGetGpml = 
		"SELECT t.old_text FROM text AS t JOIN revision AS r " +
		"WHERE r.rev_id = ? AND t.old_id = r.rev_text_id";
	
	static final String PREFIX_DELETED = "{{deleted";
	static final String PREFIX_REDIRECT = "#REDIRECT";
	
	static class PathwayParser extends DefaultHandler {
		String organism;
		
		public PathwayParser(Reader in, XMLReader xmlReader) throws IOException, SAXException {
			xmlReader.setContentHandler(this);
			xmlReader.setEntityResolver(this);
			xmlReader.parse(new InputSource(in));
		}
		
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			
			if(localName.equals("Pathway")) {
				organism = attributes.getValue("Organism");
			}
		}
		
		public String getOrganism() {
			return organism;
		}
	}
}
