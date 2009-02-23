package org.its;

import java.awt.Rectangle;

/**
 * Information about a specific tiler, such as the
 * number of zoom levels and the size.
 * @author thomas
 */
public class TilerInfo {
	private Rectangle bounds;
	private double[] resolutions;
	
	public TilerInfo(double[] resolutions, Rectangle bounds) {
		this.bounds = bounds;
		this.resolutions = resolutions;
	}
	
	public double[] getResolutions() {
		return resolutions;
	}
	
	public Rectangle getBounds() {
		return bounds;
	}
}
