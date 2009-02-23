package org.pathvisio.gsea;

import java.awt.Container;
import java.awt.Dimension;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JFrame;

import org.pathvisio.Engine;
import org.pathvisio.data.DBConnDerby;
import org.pathvisio.data.DBConnector;
import org.pathvisio.data.Gdb;
import org.pathvisio.data.SimpleGdb;
import org.pathvisio.data.SimpleGex;
import org.pathvisio.debug.Logger;

public class Gui {
	public static void main(String[] args) {
		Engine.init();
		
		String dirName = null;
		String gdbName = null;
		String gexName = null;
		
		for(int i = 0; i < args.length - 1; i++) 
		{
			if("-p".equals(args[i])) 
			{
				dirName = args[i + 1];
				i++;
			}
			else if ("-d".equals(args[i]))
			{
				gexName = args[i + 1];
				i++;
			}
			else if ("-g".equals(args[i])) 
			{
				gdbName = args[i + 1];
				i++;
			}
			else
			{
				Logger.log.warn("Unable to parse argument: " + args[i]);
			}
		}
		
		Gdb gdb = null;
		SimpleGex gex = null;
		File pathwayDir = null;
		
		DBConnector dbconn = new DBConnDerby();

		if(gdbName != null) {
			try {
				gdb = new SimpleGdb(gdbName, dbconn, DBConnector.PROP_NONE);
				gex = new SimpleGex(gexName, false, dbconn);
			} catch(Exception e) {
				Logger.log.error("Unable to connect to database", e);
			}
		}
		if(gexName != null) {
			try {
				gex = new SimpleGex(gexName, false, dbconn);
			} catch(Exception e) {
				Logger.log.error("Unable to connect to database", e);
			}
		}
		
		if(dirName != null) {
			pathwayDir = new File(dirName);
		}
		
		JFrame frame = new JFrame("GSEA");
		frame.setMinimumSize(new Dimension(300, 500));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		Container content = frame.getContentPane();
		
		content.add(new ConfigPanel(gdb, gex, pathwayDir));
		
		frame.pack();
		frame.setVisible(true);
	}
}
