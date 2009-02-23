package org.pathvisio.go;

import org.pathvisio.model.DataSource;
import org.pathvisio.model.Xref;

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
