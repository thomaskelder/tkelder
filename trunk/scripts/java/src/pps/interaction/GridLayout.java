package pps.interaction;

import java.util.Collection;

import org.pathvisio.model.PathwayElement;

/**
 * GridLayout for GPML elements.
 * @author thomas
 */
public class GridLayout {
	private double spacing = 5 * 15;
	private int width;
	
	public GridLayout(int width) {
		this.width = width;
	}
	
	public void layout(Collection<PathwayElement> elements, double startX, double startY) {
		double nx = 1;
		double cx = startX;
		double cy = startY;
		
		for(PathwayElement elm : elements) {
			elm.setMLeft(cx);
			elm.setMTop(cy);
			
			if(nx >= width) {
				cx = startX;
				cy += spacing + elm.getMHeight();
				nx = 1;
			} else {
				cx += spacing + elm.getMWidth();
				nx++;
			}
		}
	}
}
