package org.pathvisio.go.gui;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.pathvisio.go.GOAnnotations;
import org.pathvisio.go.GOTerm;
import org.pathvisio.go.GOTree;

public class GOExplorer extends JFrame {
	GOTreePanel treePanel;
	GOInfoPanel infoPanel;
	
	public GOExplorer() {
		super("GO Exporer");
		treePanel = new GOTreePanel();
		infoPanel = new GOInfoPanel();
		
		treePanel.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				Object s = treePanel.getLastSelectedPathComponent();
				if(s != null) infoPanel.setTerm((GOTerm)s);
			}
		});
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, infoPanel), BorderLayout.CENTER);
	}
	
	public void setGO(GOTree tree, GOAnnotations annotations) {
		treePanel.setGoTree(tree);
		infoPanel.setAnnotations(tree, annotations);
	}
}
