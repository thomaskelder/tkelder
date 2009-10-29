package org.apa.report;

import java.io.File;

import org.apa.AtlasException;
import org.hibernate.Session;

public abstract class Report {
	private Session session;
	
	public Report(Session session) {
		this.session = session;
		session.beginTransaction();
	}
	
	protected Session getSession() {
		return session;
	}
	
	public abstract void saveReport(File outPath, String prefix) throws AtlasException;
}