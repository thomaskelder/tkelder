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
import java.util.Map;

import org.pathvisio.debug.Logger;
import org.pathvisio.go.mapper.ScoreMatrix;

public class GOAnnotations {
	HashMap<GOTerm, Map<String, GOAnnotation>> annotations = new HashMap<GOTerm, Map<String, GOAnnotation>>();
	
	public GOAnnotation getAnnotation(GOTerm term, String id) {
		Map<String, GOAnnotation> a = annotations.get(term);
		if(a != null) return a.get(id);
		return null;
	}
	
	public Collection<GOAnnotation> getAnnotations(GOTerm term) {
		Map<String, GOAnnotation> a = annotations.get(term);
		return a == null ? new HashSet<GOAnnotation>() : a.values();
	}
	
	public void addAnnotation(GOTerm term, GOAnnotation annotation) {
		Map<String, GOAnnotation> values = annotations.get(term);
		if(values == null) {
			annotations.put(term, values = new HashMap<String, GOAnnotation>());
		}
		values.put(annotation.getId(), annotation);
	}
	
	public static GOAnnotations read(File annotFile, GOTree tree, GOAnnotationFactory f) throws IOException {
		GOAnnotations annotations = new GOAnnotations();
		
		BufferedReader in = new BufferedReader(new FileReader(annotFile));
		String line;
		while((line = in.readLine()) != null) {
			String[] cols = line.split("\t", 3);
			String id = cols[0];
			String go = cols[1];
			String ev = cols[2];
			GOAnnotation annot = f.createAnnotation(id, ev);
			GOTerm term = tree.getTerm(go);
			if(term != null) {
				Logger.log.trace("Annotating " + term.getId() + " with " + annot);
				annotations.addAnnotation(term, annot);
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
