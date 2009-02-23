package org.its.util;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class Xml {
	public static Document newDocument() throws ParserConfigurationException {
        return getDocBuilder().newDocument();
	}
	
	public static String toString(Document doc) throws TransformerException {
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();

        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        return sw.toString();
	}
	
	public static Document readSvg(File f) throws SAXException, IOException, ParserConfigurationException {
	    String parser = XMLResourceDescriptor.getXMLParserClassName();
	    SAXSVGDocumentFactory svgFac = new SAXSVGDocumentFactory(parser);
	    return svgFac.createDocument(f.toURI().toString());
	}
	
	private static DocumentBuilder docBuilder;
	
	private static DocumentBuilder getDocBuilder() throws ParserConfigurationException {
	    if(docBuilder == null) {
	    	docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	    }
	    return docBuilder;
	}
}
