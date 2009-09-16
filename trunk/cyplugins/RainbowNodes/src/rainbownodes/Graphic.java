package rainbownodes;

import giny.view.NodeView;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.imageio.ImageIO;

import cytoscape.render.stateful.CustomGraphic;

/**
 * Graphics to draw over a Cytoscape node.
 * @author thomas
 */
public abstract class Graphic implements Serializable {
	private static final long serialVersionUID = -6528977249856158196L;
	private Gradient gradient;
	private List<String> attributes;
	
	private int scaleImg = 3; //For better resolution, scale the buffered image up
	
	public Graphic(List<String> attributes, Gradient gradient) {
		this.attributes = attributes;
		this.gradient = gradient;
	}
	
	protected Gradient getGradient() {
		return gradient;
	}
	
	protected List<String> getAttributes() {
		return attributes;
	}
	
	public void setScaleImg(int scaleImg) {
		this.scaleImg = scaleImg;
	}
	
	protected abstract Shape getShape(NodeView nv);
	protected abstract void paint(Graphics graphics, NodeView nv, Rectangle bounds);
	
	public CustomGraphic asCustomGraphic(NodeView nv) {
		Shape shape = getShape(nv);
		Rectangle bounds = shape.getBounds();
		BufferedImage img = new BufferedImage(
				bounds.width * scaleImg, bounds.height * scaleImg, 
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = (Graphics2D)img.getGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		paint(graphics, nv, new Rectangle(img.getWidth() - 1, img.getHeight() - 1));
		Paint imgPaint = new TexturePaint(img, bounds);

		return new CustomGraphic(
				shape, imgPaint, (byte) 0
		);
	}
	
	public void saveLegend(File imgFile, String title) throws IOException {
		BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
		paintLegend(img.createGraphics(), new Rectangle(0, 0, img.getWidth(), img.getHeight()), title);
		ImageIO.write(img, "png", imgFile);
	}
	
	protected void paintLegend(Graphics g, Rectangle bounds, String title) {
		int margin = 5;

		//Draw background
		g.setColor(Color.WHITE);
		g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

		Font f = new Font("sansserif", Font.PLAIN, 16);
		g.setFont(f);
		
		//Draw title
		Rectangle titleBounds = new Rectangle(
				bounds.x + margin, bounds.y + margin, 
				bounds.width, g.getFontMetrics().getHeight() + margin
		);
		g.setColor(Color.BLACK);
		g.drawString(title, titleBounds.x, titleBounds.y + titleBounds.height / 2);
		
		int gradientHeight = Math.min(50, (int)((bounds.height - titleBounds.height) * 0.3));
		Rectangle gradientBounds = new Rectangle(
				bounds.x + margin, titleBounds.y + titleBounds.height + margin, 
				bounds.width - margin, gradientHeight - margin);
		gradient.paintLegend(g, gradientBounds);
		
		g.setColor(Color.BLACK);
		int baseline = gradientBounds.y + gradientBounds.height + margin;
		int lineHeight = g.getFontMetrics().getHeight();
		int sampleNum = getAttributes().size();
		
		for (int i = 0; i < sampleNum; i++) {
			int base = baseline + lineHeight - g.getFontMetrics().getDescent();
									
			String label = (i + 1) + ": " + attributes.get(i);
			g.drawString (label, bounds.x + margin, i * lineHeight + base);
		}
	}
}