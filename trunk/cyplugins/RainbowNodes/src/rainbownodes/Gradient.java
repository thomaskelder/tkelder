package rainbownodes;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A Color gradient.
 * @author thomas
 */
public class Gradient implements Serializable {
	private static final long serialVersionUID = -1533851816647153152L;
	SortedMap<Double, Color> pointColors;

	public Gradient() {
		pointColors = new TreeMap<Double, Color>();
	}

	public void addPoint(double value, Color c) {
		pointColors.put(value, c);
	}

	public Gradient point(double value, Color c) {
		addPoint(value, c);
		return this;
	}

	public void removePoint(double value) {
		pointColors.remove(value);
	}

	public double getMin() {
		return pointColors.firstKey();
	}

	public double getMax() {
		return pointColors.lastKey();
	}

	public Color calculate(double value) {
		double valueStart = getMin();
		double valueEnd = getMax();
		Color colorStart = null;
		Color colorEnd = null;

		//Easy cases
		if(value <= getMin()) return pointColors.get(getMin());
		if(value >= getMax()) return pointColors.get(getMax());

		//Find what colors the value is in between
		//Keys are sorted!
		List<Double> values = new ArrayList<Double>(pointColors.keySet());
		for(int i = 0; i < values.size() - 1; i++) {
			double d = values.get(i);
			double dnext = values.get(i + 1);
			if(value > d && value < dnext)
			{
				valueStart = d;
				colorStart = pointColors.get(d);
				valueEnd = dnext;
				colorEnd = pointColors.get(dnext);
				break;
			}
		}

		if(colorStart == null || colorEnd == null) return null; //Check if the values/colors are found

		// Interpolate to find the color belonging to the given value
		double alpha = (value - valueStart) / (valueEnd - valueStart);
		double red = colorStart.getRed() + alpha*(colorEnd.getRed() - colorStart.getRed());
		double green = colorStart.getGreen() + alpha*(colorEnd.getGreen() - colorStart.getGreen());
		double blue = colorStart.getBlue() + alpha*(colorEnd.getBlue() - colorStart.getBlue());

		return new Color((int)red, (int)green, (int)blue);
	}

	public void paintLegend(Graphics graphics, Rectangle bounds) {
		Graphics2D g = (Graphics2D)graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		for(int i = 0; i < bounds.width; i++) {
			Color c = calculate(getMin() + (double)i * (getMax() - getMin())/ bounds.width);
			g.setColor(c);
			g.fillRect(bounds.x + i, bounds.y, 1, bounds.height);
		}

		g.setColor(Color.BLACK);
		int margin = 30; //Border spacing
		int x = bounds.x + margin / 2;
		int w = bounds.width - margin;
		for(double dv : pointColors.keySet()) {
			String value = "" + dv;
			Rectangle2D fb = g.getFontMetrics().getStringBounds(value, g);
			g.drawString(
					value, 
					x - (int)(fb.getWidth() / 2), 
					bounds.y + bounds.height / 2 + (int)(fb.getHeight() / 2)
			);
			x += w / (pointColors.size() - 1);
		}
	}
}
