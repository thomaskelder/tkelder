package org.pathvisio.go;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class GOTree implements Cloneable {
	Map<String, GOTerm> goTerms = new HashMap<String, GOTerm>();
	Map<String, Set<GOTerm>> parents = new HashMap<String, Set<GOTerm>>();
	Map<String, Set<GOTerm>> children = new HashMap<String, Set<GOTerm>>();
	Map<String, Set<GOTerm>> bySynonym = new HashMap<String, Set<GOTerm>>();
	
	public GOTree(Collection<GOTerm> terms) {
		buildTree(terms);
	}
	
	private void buildTree(Collection<GOTerm> terms) {
		for(GOTerm term : terms) { //Add terms
			goTerms.put(term.getId(), term);
			for(String syn : term.getSynonyms()) {
				Set<GOTerm> synSet = bySynonym.get(syn);
				if(synSet == null) bySynonym.put(syn, synSet = new HashSet<GOTerm>());
				synSet.add(term);
			}
		}
		for(GOTerm term : terms) {
			Set<GOTerm> termParents = new HashSet<GOTerm>();
			for(String pid : term.getParents()) {
				termParents.add(getTerm(pid));
				Set<GOTerm> sisters = children.get(pid);
				if(sisters == null) {
					children.put(pid, sisters = new HashSet<GOTerm>());
				}
				sisters.add(term);
			}
			parents.put(term.getId(), termParents);
		}
	}
	
	public GOTerm getTerm(String id) {
		return goTerms.get(id);
	}
	
	/**
	 * Find GO terms by name (including synonyms).
	 */
	public Set<GOTerm> findTermsByName(String name) {
		Set<GOTerm> result = bySynonym.get(name);
		if(result == null) result = new HashSet<GOTerm>();
		return result;
	}
	
	public Set<GOTerm> getChildren(String id) {
		Set<GOTerm> c = children.get(id);
		return c == null ? new HashSet<GOTerm>() : c;
	}
	
	public Set<GOTerm> getParents(String id) {
		Set<GOTerm> p = parents.get(id);
		return p == null ? new HashSet<GOTerm>() : p;
	}
	
	public Set<GOTerm> getRecursiveParents(String id) {
		Set<GOTerm> parents = new HashSet<GOTerm>();
		
		for(GOTerm p : getParents(id)) {
			parents.addAll(getRecursiveParents(p.getId()));
			parents.add(p);
		}
		
		return parents;
	}
	
	public Collection<GOTerm> getTerms() {
		return goTerms.values();
	}
	
	/**
	 * Get the annotations from the given go term and
	 * all it's children.
	 */
	public <K extends GOAnnotation> Set<K> getRecursiveAnnotations(GOTerm term, GOAnnotations<K> annotations) {
		Set<K> recursive = new HashSet<K>();
		recursive.addAll(annotations.getAnnotations(term));
		for(GOTerm child : getChildren(term.getId())) {
			recursive.addAll(getRecursiveAnnotations(child, annotations));
		}
		return recursive;
	}
	
	/**
	 * Returns a clone of the tree structure, excluding annotations.
	 * @return
	 */
	public GOTree cloneTree() {
		Set<GOTerm> terms = new HashSet<GOTerm>();
		for(GOTerm t : goTerms.values()) {
			GOTerm n = new GOTerm(t.getId(), t.getName());
			n.setObsolete(t.isObsolete());
			n.setParents(t.getParents());
		}
		return new GOTree(terms);
	}
}
