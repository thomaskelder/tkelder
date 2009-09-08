package rainbownodes;

import giny.view.NodeView;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.List;

import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;

public class BarGraphic extends Graphic {
	private int barWidth = 8;
	private int totalHeight = 25;
	
	public BarGraphic(List<String> attributes, Gradient gradient) {
		super(attributes, gradient);
		setScaleImg(1);
	}
	
	public void setBarWidth(int barWidth) {
		this.barWidth = barWidth;
	}
	
	public void setTotalHeight(int totalHeight) {
		this.totalHeight = totalHeight;
	}
	
	protected Shape getShape(NodeView nv) {
		int w = barWidth * getAttributes().size();
		int h = totalHeight;
		int x = -w/2;
		int y = (int)nv.getHeight() / 2;
		return new Rectangle(x, y, w, h);
	}
	
	protected void paint(Graphics graphics, NodeView nv, Rectangle bounds) {
		List<String> attributes = getAttributes();
		
		int scaledHeight = bounds.height;
		int scaledWidth = bounds.width / getAttributes().size();
		
		CyAttributes cyAttr = Cytoscape.getNodeAttributes();
		for(int i = 0; i < attributes.size(); i++) {
			String attr = attributes.get(i);
			double value = 0;
			try {
				Object obj = cyAttr.getAttribute(nv.getNode().getIdentifier(), attr);
				value = Double.parseDouble(obj == null ? "" : obj.toString());
			} catch(NumberFormatException e) {
				System.err.println(e.getMessage());
				return;
			}
			Color color = getGradient().calculate(value);
			graphics.setColor(color);
			
			int baseline = bounds.y + scaledHeight / 2;
			
			double range = getGradient().getMax() - getGradient().getMin();
			int height = (int)((scaledHeight / range) * value);
			if(value > 0) {
				graphics.fillRect(bounds.x + i * scaledWidth, baseline - height, scaledWidth, height);
			} else {
				graphics.fillRect(bounds.x + i * scaledWidth, baseline + 1, scaledWidth, -height);
			}
			
			//Draw base line
			graphics.setColor(Color.BLACK);
			graphics.drawLine(bounds.x, baseline, scaledWidth * getAttributes().size(), baseline);
		}
	}
}
