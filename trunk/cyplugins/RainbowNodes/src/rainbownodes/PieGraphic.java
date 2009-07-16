package rainbownodes;

import giny.view.NodeView;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.List;

import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;

public class PieGraphic extends Graphic {
	public PieGraphic(List<String> attributes, Gradient gradient) {
		super(attributes, gradient);
	}

	protected Shape getShape(NodeView nv) {
		double w = nv.getWidth();
		double h = nv.getHeight();
		return new Ellipse2D.Double(
				-w/2, -h/2, w - 1, h - 1
		);
	}

	protected void paint(Graphics graphics, NodeView nv, Rectangle bounds) {
		int angle = 360 / getAttributes().size();
		
		CyAttributes cyAttr = Cytoscape.getNodeAttributes();
		
		List<String> attributes = getAttributes();
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
			
			graphics.fillArc(bounds.x, bounds.y, bounds.width, bounds.height, 
					angle * i, angle);

			graphics.setColor(Color.BLACK);
			graphics.drawArc(bounds.x, bounds.y, bounds.width, bounds.height, 
					angle * i, angle);
			
		}
	}	 
}
