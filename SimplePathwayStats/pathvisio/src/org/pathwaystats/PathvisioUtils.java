package org.pathwaystats;

import java.io.File;
import java.util.Collection;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pathvisio.data.XrefWithSymbol;
import org.pathvisio.gex.ReporterData;
import org.pathvisio.gex.Sample;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.util.PathwayParser;
import org.pathvisio.util.PathwayParser.ParseException;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class PathvisioUtils {
	public static Sample findSample(SimpleGex data, String name) throws IDMapperException {
		for(Sample s : data.getSamples().values()) {
			if(name.equals(s.getName())) return s;
		}
		return null;
	}
	
	public static Multimap<File, Xref> readPathwaySets(Collection<File> gpmlFiles, IDMapper idMapper, DataSource targetDs) throws SAXException, ParseException, IDMapperException {
		XMLReader xmlReader = XMLReaderFactory.createXMLReader();
		
		Multimap<File, Xref> sets = new HashMultimap<File, Xref>();
		for(File f : gpmlFiles) {
			PathwayParser pp = new PathwayParser(f, xmlReader);
			for(XrefWithSymbol xs : pp.getGenes()) {
				Xref x = xs.asXref();
				if(targetDs.equals(x.getDataSource())) {
					sets.put(f, x);
				} else {
					sets.putAll(f, idMapper.mapID(x, targetDs));
				}
			}
		}
		return sets;
	}
	
	public static Multimap<Xref, Double> asMap(SimpleGex data, Sample sample, IDMapper idMapper, DataSource targetDs) throws IDMapperException {
		Multimap<Xref, Double> mapData = new ArrayListMultimap<Xref, Double>();
		
		for(int i = 0; i < data.getNrRow(); i++) {
			ReporterData rd = data.getRow(i);
			Double value = (Double)rd.getSampleData(sample);
			Xref rx = rd.getXref();
			if(targetDs.equals(rx.getDataSource())) {
				mapData.put(rx, value);
			} else {
				for(Xref x : idMapper.mapID(rx, targetDs)) {
					mapData.put(x, value);
				}
			}
		}
		
		return mapData;
	}
}
