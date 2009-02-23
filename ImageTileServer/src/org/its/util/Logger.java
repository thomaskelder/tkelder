package org.its.util;

import java.io.PrintStream;

public class Logger {
	public static Logger log = new Logger();
	
	private PrintStream out = System.err;
	
	public void setStream(PrintStream out) {
		this.out = out;
	}
	
	public void trace(String msg) {
		out.println("[TRACE " + System.currentTimeMillis() + "] " + msg);
	}
	
	public void error(String msg) {
		out.println("[ERROR " + System.currentTimeMillis() + "] " + msg);
	}
	
	public void error(Throwable e) {
		out.println("[ERROR " + System.currentTimeMillis() + "] " + e.getMessage());
		e.printStackTrace();
	}
}
