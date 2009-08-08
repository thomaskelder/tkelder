package org.pathvisio.go.venn;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.go.GOAnnotation;
import org.pathvisio.go.GOAnnotations;
import org.pathvisio.go.GOTerm;
import org.pathvisio.go.GOTree;
import org.pathvisio.go.XrefAnnotation;
import org.pathvisio.visualization.colorset.Criterion;
import org.pathvisio.visualization.colorset.Criterion.CriterionException;

public class GoVenn {
	/**
	 * Write mappings to the given GO terms as a VennMaster input file.
	 * This file contains a line for each annotation-term association.
	 * @param terms
	 * @param out
	 * @throws IOException 
	 */
	public static void writeVennMappings(GOTree tree, GOAnnotations<?> annot, GOTerm[] terms, Writer out) throws IOException {
		for(GOTerm term : terms) {
			for(GOAnnotation termAnnot : tree.getRecursiveAnnotations(term, annot)) {
				out.append(termAnnot.getId());
				out.append("\t");
				out.append(term.getName() + "(" + term.getId() + ")");
				out.append("\n");
			}
		}
	}
	
	public static List<Set<Xref>> getAnnotatedSets(GOTree tree, GOAnnotations<XrefAnnotation> annot, Set<Xref> xrefs, GOTerm[]...terms) {
		List<Set<Xref>> sets = new ArrayList<Set<Xref>>();
		for(GOTerm[] termGroup : terms) {
			Set<XrefAnnotation> a = new HashSet<XrefAnnotation>();
			for(GOTerm t : termGroup) a.addAll(tree.getRecursiveAnnotations(t, annot));
			Set<Xref> match = new HashSet<Xref>(a);
			match.retainAll(xrefs);
			sets.add(match);
		}
		return sets;
	}
	
	public static List<Set<Xref>> getAnnotatedSets(GOTree tree, GOAnnotations<XrefAnnotation> annot, Set<Xref> xrefs, GOTerm...terms) {
		GOTerm[][] t = new GOTerm[terms.length][1];
		for(int i = 0; i < t.length; i++) t[i][0] = terms[i];
		return getAnnotatedSets(tree, annot, xrefs, t);
	}
	
	public static List<Set<Xref>> getAnnotatedSetsByCriterion(SimpleGex data, String crit, GOTree tree, GOAnnotations<XrefAnnotation> annot, GOTerm...terms) throws IDMapperException, CriterionException {
		GOTerm[][] t = new GOTerm[terms.length][1];
		for(int i = 0; i < t.length; i++) t[i][0] = terms[i];
		return getAnnotatedSetsByCriterion(data, crit, tree, annot, t);
	}
	
	public static List<Set<Xref>> getAnnotatedSetsByCriterion(SimpleGex data, String crit, GOTree tree, GOAnnotations<XrefAnnotation> annot, GOTerm[]...terms) throws IDMapperException, CriterionException {
		Criterion c = new Criterion();
		c.setExpression(crit, data.getSampleNames());
		
		Set<Xref> match = new HashSet<Xref>();
		
		int maxRow = data.getNrRow();
		for(int i = 0; i < maxRow; i++) {
			Map<String, Object> sdata = data.getRow(i).getByName();
			Xref xref = data.getRow(i).getXref();
			if(c.evaluate(sdata)) match.add(xref);
		}
		
		return getAnnotatedSets(tree, annot, match, terms);
	}
}
