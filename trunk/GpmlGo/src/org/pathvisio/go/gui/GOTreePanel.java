package org.pathvisio.go.gui;

import javax.swing.JTree;

import org.pathvisio.go.GOTree;

public class GOTreePanel extends JTree {
	public void setGoTree(GOTree goTree) {
		setModel(new GOTreeModel(goTree));
	}
}
