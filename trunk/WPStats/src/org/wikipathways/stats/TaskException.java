package org.wikipathways.stats;

public class TaskException extends Exception {
	public TaskException(Throwable e) {
		super(e);
	}
	
	public TaskException(String msg, Throwable e) {
		super(msg, e);
	}
}
