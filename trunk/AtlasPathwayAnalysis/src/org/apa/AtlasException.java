package org.apa;

public class AtlasException extends Exception {
	private static final long serialVersionUID = 704642931994553568L;

	public AtlasException(Exception source) {
		super(source.getMessage(), source);
	}
}
