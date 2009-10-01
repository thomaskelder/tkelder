package venn;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * A simple venn diagram where the set sizes are shown
 * as numbers in the corresponding areas.
 * @author thomas
 */
public class NumberVennDiagram extends VennDiagramTemplate {
	Font font = new Font("Arial", Font.PLAIN, 36);
	Color background = Color.WHITE;
	Color color = Color.BLACK;
	Color labelColor = Color.BLACK;
	
	public NumberVennDiagram(VennCounts data) {
		super(data);
	}
	
	public void setBackground(Color background) {
		this.background = background;
	}
	
	protected void paintBackground(Graphics g) {
		if(background != null) {
			Rectangle bounds = getBounds();
			g.setColor(background);
			g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
		}
	}
	
	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D)g.create();
		
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		paintBackground(g2d);
		
		super.paint(g);

		g2d.setFont(font);
		
		for(int i : getPartialShapeIndices()) {
			String label = getData().getUnionCountLabel(i);
			Rectangle sb = getPartialShape(i).getBounds();
			Rectangle2D tb = font.getStringBounds(label + "", g2d.getFontRenderContext());
			
			g2d.setColor(color);
			int x = (int)(sb.getCenterX() - tb.getWidth() / 2);
			int y = (int)(sb.getCenterY());
			g2d.setColor(labelColor);
			g2d.drawString(label, x, y);
		}
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
		final NumberVennDiagram venn = new NumberVennDiagram(data);
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
