package rainbownodes;

import giny.view.NodeView;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;

public class PieGraphic<E> extends Graphic {
	public PieGraphic(List<String> attributes, ColorMapper mapper) {
		super(attributes, mapper);
	}

	protected Shape getShape(NodeView nv) {
		double w = nv.getWidth();
		double h = nv.getHeight();
		return new Ellipse2D.Double(
				-w/2, -h/2, w - 1, h - 1
		);
	}

	protected void paint(Graphics graphics, NodeView nv, Rectangle bounds) {
		int nrParts = getAttributes().size();

		//If one of the attributes is a list, also create a part for each list item
		List<String> attributes = getAttributes();
		CyAttributes cyAttr = Cytoscape.getNodeAttributes();
		for(String attr : attributes) {
			if(cyAttr.getType(attr) == CyAttributes.TYPE_SIMPLE_LIST) {
				List values = cyAttr.getListAttribute(nv.getNode().getIdentifier(), attr);
				if(values.size() > 0) nrParts += values.size() - 1;
			}
		}
		int angle = 360 / nrParts;

		int part = 0;
		for(String attr : attributes) {
			List<Object> values = new ArrayList<Object>();
			if(cyAttr.getType(attr) == CyAttributes.TYPE_SIMPLE_LIST) {
				List vs = cyAttr.getListAttribute(nv.getNode().getIdentifier(), attr);
				for(Object v : vs) {
					values.add(v);
				}
			} else {
				values.add(cyAttr.getAttribute(nv.getNode().getIdentifier(), attr));
			}

			for(int j = 0; j < values.size(); j++) {
				Object value = values.get(j);
				Color color = getMapper().calculate(value);
				System.err.println(j + ": Color " + color + " for value " + value);
				graphics.setColor(color);

				graphics.fillArc(bounds.x, bounds.y, bounds.width, bounds.height, 
						angle * part, angle);
				part++;
			}
		}
	}
}
