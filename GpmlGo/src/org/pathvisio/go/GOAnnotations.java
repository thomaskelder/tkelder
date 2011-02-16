package org.pathvisio.go;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.pathvisio.data.XrefWithSymbol;
import org.pathvisio.debug.Logger;
import org.pathvisio.go.mapper.ScoreMatrix;
import org.pathvisio.util.PathwayParser;
import org.pathvisio.util.PathwayParser.ParseException;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class GOAnnotations<K extends GOAnnotation> {
	Map<GOTerm, Map<String, K>> annotations = new HashMap<GOTerm, Map<String, K>>();
	Multimap<String, GOTerm> annotation2terms = new HashMultimap<String, GOTerm>();
	
	public K getAnnotation(GOTerm term, String id) {
		Map<String, K> a = annotations.get(term);
		if(a != null) return a.get(id);
		return null;
	}
	
	public Collection<K> getAnnotations(GOTerm term) {
		Map<String, K> a = annotations.get(term);
		return a == null ? new HashSet<K>() : a.values();
	}
	
	public Collection<GOTerm> getTerms(K annotation) {
		return getTerms(annotation.getId());
	}
	
	public Collection<GOTerm> getTerms(String annotationId) {
		return annotation2terms.get(annotationId);
	}
	
	public void addAnnotation(GOTerm term, K annotation) {
		Map<String, K> values = annotations.get(term);
		if(values == null) {
			annotations.put(term, values = new HashMap<String, K>());
		}
		values.put(annotation.getId(), annotation);
		
		annotation2terms.put(annotation.getId(), term);
	}
	
	public static <K extends GOAnnotation> GOAnnotations<K> fromIDMapper(IDMapper idm, DataSource ds, GOTree tree, GOAnnotationFactory<K> f) throws IOException, IDMapperException {
		GOAnnotations<K> annotations = new GOAnnotations<K>();
		
		for(GOTerm t : tree.getTerms()) {
			String id = t.getId();
			Xref gox = new Xref(id, BioDataSource.GENE_ONTOLOGY);
			for(Xref x : idm.mapID(gox, ds)) {
				for(K a : f.createAnnotations(x.getId(), "")) {
					annotations.addAnnotation(t, a);
				}
			}
		}
		return annotations;
	}
	
	public static <K extends GOAnnotation> GOAnnotations<K> read(File annotFile, GOTree tree, GOAnnotationFactory<K> f) throws IOException {
		GOAnnotations<K> annotations = new GOAnnotations<K>();
		
		BufferedReader in = new BufferedReader(new FileReader(annotFile));
		String line;
		while((line = in.readLine()) != null) {
			String[] cols = line.split("\t", 3);
			String id = cols[0];
			String go = cols[1];
			String ev = cols[2];
			Collection<K> annot = f.createAnnotations(id, ev);
			GOTerm term = tree.getTerm(go);
			if(term != null) {
				Logger.log.trace("Annotating " + term.getId() + " with " + annot);
				for(K a : annot) annotations.addAnnotation(term, a);
			} else {
				Logger.log.warn("GO Term '" + go + "' not found!");
			}
		}
		return annotations;
	}
	
	public void write(File annotFile) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(annotFile));
		for(GOTerm term : annotations.keySet()) {
			for(GOAnnotation a : getAnnotations(term)) {
				out.write(a.getId() + "\t" + term.getId() + "\t" + a.getEvidence() + "\n");
			}
		}
		out.close();
	}
	
	public void writeXrefs(File outFile, IDMapper gdb, DataSource ds) throws ParseException, IOException, IDMapperException, SAXException {
		XMLReader xmlReader = XMLReaderFactory.createXMLReader();
		Map<GOTerm, Set<String>> xrefMap = new HashMap<GOTerm, Set<String>>();
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
		for(GOTerm term : annotations.keySet()) {
			for(GOAnnotation a : getAnnotations(term)) {
				Logger.log.info("Looking up xrefs for term " + term.getId() + ", pathway " + a.getId());
				File pwFile = new File(a.getId());
				if(!pwFile.exists()) {
					throw new IllegalArgumentException("The mappings file doesn't contain valid file names, " +
							"please use mappings exported with the -useFileName parameter set to true."
					);
				}
				PathwayParser parser = new PathwayParser(pwFile, xmlReader);
				List<XrefWithSymbol> genes =  parser.getGenes();
				
				for(XrefWithSymbol g : genes) {
					for(Xref x : gdb.mapID(g.asXref(), ds)) {
						Set<String> xrefs = xrefMap.get(term);
						if(xrefs == null) xrefMap.put(term, xrefs = new HashSet<String>());
						xrefs.add(x.getId());
					}
				}
			}
		}
		for(GOTerm t : xrefMap.keySet()) {
			for(String id : xrefMap.get(t)) {
				out.write(id + "\t" + t.getId() + "\n");
			}
		}
		out.close();
	}
	
	public ScoreMatrix<String> createEvidenceMatrix(String defaultValue) {
		ScoreMatrix<String> m = new ScoreMatrix<String>();
		m.setDefault(defaultValue);
		for(GOTerm t : annotations.keySet()) {
			for(GOAnnotation a : getAnnotations(t)) {
				m.setScore(t.getName() + "(" + t.getId() + ")", a.getId(), a.getEvidence());
			}
		}
		return m;
	}
}
