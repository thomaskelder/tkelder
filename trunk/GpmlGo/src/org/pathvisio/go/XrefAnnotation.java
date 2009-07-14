package org.pathvisio.go;

import org.bridgedb.DataSource;
import org.bridgedb.Xref;

public class XrefAnnotation extends Xref implements GOAnnotation {
	private String evidence;
	
	public XrefAnnotation(String id, DataSource ds, String evidence) {
		super(id, ds);
		this.evidence = evidence;
	}
	
	public String getEvidence() {
		return evidence;
	}
}
