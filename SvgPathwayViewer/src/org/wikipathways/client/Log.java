package org.wikipathways.client;

public class Log {
	public static native void firebug(String msg) /*-{
		if($wnd.console) {
			$wnd.console.log("GWT: " + msg);
		}
}-*/;
}
