package venn;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class GradientVennDiagram extends NumberVennDiagram {
	Color lowColor;
	double lowValue;
	Color highColor;
	double highValue;
	
	public GradientVennDiagram(VennCounts data) {
		super(data);
		lowColor = Color.WHITE;
		highColor = new Color(150, 150, 255);
		lowValue = 0;
		highValue = 0;
		for(int i : getPartialShapeIndices()) {
			highValue = Math.max(highValue, getData().getUnionCount(i));
		}
	}
	
	public GradientVennDiagram(VennCounts data, double lowValue, Color lowColor, double highValue, Color highColor) {
		super(data);
		this.lowColor = lowColor;
		this.lowValue = lowValue;
		this.highColor = highColor;
		this.highValue = highValue;
	}
	
	public void setGradient(double highValue, Color highColor) {
		this.highColor = highColor;
		this.highValue = highValue;
	}
	
	public void paint(Graphics g) {
		//Fill the union shapes with a color depending on the gradient
		paintBackground(g);
		setBackground(null);
		
		Graphics2D g2d = (Graphics2D)g.create();
		
		for(int i : getPartialShapeIndices()) {
			Shape s = getPartialShape(i);
			int count = getData().getUnionCount(i);
			g2d.setColor(calculateColor(count));
			g2d.fill(s);
		}
		
		super.paint(g);
	}
	
	private Color calculateColor(double value) {
		//Easy cases
		if(value <= lowValue) return lowColor;
		if(value >= highValue) return highColor;
		
		// Otherwise, interpolate
		double alpha = (value - lowValue) / (highValue - lowValue);
		double red = lowColor.getRed() + alpha*(highColor.getRed() - lowColor.getRed());
		double green = lowColor.getGreen() + alpha*(highColor.getGreen() - lowColor.getGreen());
		double blue = lowColor.getBlue() + alpha*(highColor.getBlue() - lowColor.getBlue());
		return new Color((int)red, (int)green, (int)blue);
	}
	
	public static void main(String[] args) {
		//Test
		Set<Integer> set1 = new HashSet<Integer>();
		Set<Integer> set2 = new HashSet<Integer>();
		Set<Integer> set3 = new HashSet<Integer>();
		for(int i = 0; i < 1000; i++) {
			set1.add((int)(Math.random() * 1000 - 500));
			set2.add((int)(Math.random() * 2000 - 1500));
			set3.add((int)(Math.random() * 5000));
		}
		List<Set<Integer>> sets = new ArrayList<Set<Integer>>();
		sets.add(set1); sets.add(set2); sets.add(set3);
		VennData<Integer> data = new VennData<Integer>(sets);
		final GradientVennDiagram venn = new GradientVennDiagram(data);
		venn.setLabels("set 1" , "set 2", "set 3");
		venn.setTitle("Test...");
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setSize(venn.getBounds().width, venn.getBounds().height);

		f.add(new JPanel() {
			protected void paintComponent(Graphics g) {
				venn.setBounds(new Rectangle(0, 0, getSize().width, getSize().height));
				venn.paint((Graphics2D)g);
			}
		}, BorderLayout.CENTER);

		f.setVisible(true);
	}
}
