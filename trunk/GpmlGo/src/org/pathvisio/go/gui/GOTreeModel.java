package org.pathvisio.go.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.pathvisio.go.GOTerm;
import org.pathvisio.go.GOTree;

public class GOTreeModel implements TreeModel {
	private GOTerm root = new GOTerm("GO", "GO");
	
	GOTree tree;
	List<GOTerm> topTerms = new ArrayList<GOTerm>();
	
	public GOTreeModel(GOTree tree) {
		this.tree = tree;
		
		//Find top terms
		for(GOTerm t : tree.getTerms()) {
			if(tree.getParents(t.getId()).size() == 0) {
				topTerms.add(t);
			}
		}
	}
	
	public void addTreeModelListener(TreeModelListener l) {}
	
	public Object getChild(Object parent, int index) {
		if(parent == root) {
			return topTerms.get(index);
		} else {
			return getSortedChildren(parent).get(index);
		}
	}
	
	public int getChildCount(Object parent) {
		if(parent == root) {
			return topTerms.size();
		} else {
			GOTerm t = (GOTerm)parent;
			return tree.getChildren(t.getId()).size();
		}
	}
	
	public int getIndexOfChild(Object parent, Object child) {
		if(parent == root) {
			return topTerms.indexOf(child);
		} else {
			return getSortedChildren(parent).indexOf(child);
		}
	}
	
	private List<GOTerm> getSortedChildren(Object t) {
		ArrayList<GOTerm> children = new ArrayList<GOTerm>(tree.getChildren(((GOTerm)t).getId()));
		Collections.sort(children);
		return children;
	}
	
	public Object getRoot() {
		return root;
	}
	
	public boolean isLeaf(Object node) {
		return tree.getChildren(((GOTerm)node).getId()).size() == 0 && node != root;
	}
	
	public void removeTreeModelListener(TreeModelListener l) {
	}
	
	public void valueForPathChanged(TreePath path, Object newValue) {
	}
}
