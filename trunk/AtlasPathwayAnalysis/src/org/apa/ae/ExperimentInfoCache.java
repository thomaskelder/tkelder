package org.apa.ae;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.pathvisio.util.FileUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class ExperimentInfoCache {
	private static final String URL_BASE = "http://www.ebi.ac.uk/microarray-as/ae/xml/experiments/";

	XMLReader parser;
	File cacheDir;

	public ExperimentInfoCache(File cacheDir) {
		this.cacheDir = cacheDir;
		cacheDir.mkdirs();
	}

	public ExperimentInfo get(String id) throws SAXException, MalformedURLException, IOException {
		if(parser == null) {
			parser = XMLReaderFactory.createXMLReader();
		}
		
		File cacheFile = new File(cacheDir, id);
		if(!cacheFile.exists()) {
			//Save the cache
			saveUrlToFile(cacheFile, URL_BASE + id);
		}
		
		ExperimentInfo info = new ExperimentInfo();
		parser.setContentHandler(new ExperimentInfoParser(info));
		parser.parse(cacheFile.getAbsolutePath());
		info.setAccession(id);
		return info;
	}

	private void saveUrlToFile(File saveFile,String location) throws MalformedURLException, IOException{
		FileUtils.downloadFile(new URL(location), saveFile);

	}
	
	private static class ExperimentInfoParser extends DefaultHandler {
		enum Element { ORGANISM, NAME }
		
		Element current;
		ExperimentInfo info;
		public ExperimentInfoParser(ExperimentInfo info) {
			this.info = info;
		}
		
		public void startElement(String nsURI, String strippedName, String tagName,
				Attributes attributes) throws SAXException {
			if("species".equals(tagName)) current = Element.ORGANISM;
			if(info.getName() == null && "name".equals(tagName)) current = Element.NAME;
		}
		
		public void endElement(String arg0, String arg1, String arg2)
				throws SAXException {
			current = null;
		}
		
		public void characters(char[] chars, int start, int length)
				throws SAXException {
			if(current != null) {
				switch(current) {
				case ORGANISM: info.setOrganism(new String(chars, start, length));
				case NAME: info.setName(new String(chars, start, length));
				}
			}
		}
	}
}
