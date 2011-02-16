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
	
	public BarGraphic(List<String> attributes, ColorMapper mapper) {
		super(attributes, mapper);
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
			Object value = cyAttr.getAttribute(nv.getNode().getIdentifier(), attr);
			Color color = getMapper().calculate(value);
			graphics.setColor(color);
			
			int baseline = bounds.y + scaledHeight / 2;
			
			int height = scaledHeight;
			
			if(getMapper() instanceof Gradient) {
				double dvalue = Gradient.parseValue(value);
				double range = ((Gradient)getMapper()).getMax() - ((Gradient)getMapper()).getMin();
				height = (int)((scaledHeight / range) * dvalue);
			}
			if(value != null) {
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
