package org.pathvisio.go.gui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.pathvisio.go.GOAnnotation;
import org.pathvisio.go.GOAnnotations;
import org.pathvisio.go.GOTerm;
import org.pathvisio.go.GOTree;

public class GOInfoPanel extends JPanel {
	JEditorPane text;
	GOAnnotations annotations;
	GOTree tree;
	
	public GOInfoPanel() {
		text = new JEditorPane();
		text.setContentType( "text/html" );
		text.setEditable( false );
		setLayout(new BorderLayout());
		add(new JScrollPane(text), BorderLayout.CENTER);
	}
	
	public void setAnnotations(GOTree tree, GOAnnotations annotations) {
		this.tree = tree;
		this.annotations = annotations;
	}
	
	public void setTerm(GOTerm term) {
		String html = "<H1>" + term.getName() + "</H1>";
		html += "<H2>" + term.getId() + "</H2>";
		
		//Get the annotations for this term
		Comparator<GOAnnotation> evidenceComp = new Comparator<GOAnnotation>() {
			public int compare(GOAnnotation o1, GOAnnotation o2) {
				return o2.getEvidence().compareTo(o1.getEvidence());
			}
		};
		
		html += "<H3>Annotations</H3><UL>";
		List<GOAnnotation> myAnnot = new ArrayList<GOAnnotation>(annotations.getAnnotations(term));
		Collections.sort(myAnnot, evidenceComp);
		
		for(GOAnnotation a : myAnnot) {
			html += "<LI>" + a.getId() + "; <B>" + a.getEvidence() + "</B>";
		}
		html += "</UL>";
		//Get the annotations for the children
		html += "<H3>Inherited annotations</H3><UL>";
		List<GOAnnotation> childAnnot = new ArrayList<GOAnnotation>();
		
		for(GOAnnotation a : tree.getRecursiveAnnotations(term, annotations)) {
			if(!myAnnot.contains(a)) {
				childAnnot.add(a);
			}
		}
		Collections.sort(childAnnot, evidenceComp);
		for(GOAnnotation a : childAnnot) {
			html += "<LI>" + a.getId() + "; <B>" + a.getEvidence() + "</B>";
		}
		html += "</UL>";
		
		text.setText(html);
		text.setCaretPosition(0);
	}
}
