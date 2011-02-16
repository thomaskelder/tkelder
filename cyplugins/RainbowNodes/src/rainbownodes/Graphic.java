package rainbownodes;

import giny.view.NodeView;

import java.awt.Color;
import java.awt.Dimension;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import cytoscape.render.stateful.CustomGraphic;

/**
 * Graphics to draw over a Cytoscape node.
 * @author thomas
 */
public abstract class Graphic implements Serializable {
	private static final long serialVersionUID = -6528977249856158196L;
	private ColorMapper mapper;
	private List<String> attributes;
	private Map<NodeView, CustomGraphic> customGraphics = new HashMap<NodeView, CustomGraphic>();
	
	private int scaleImg = 3; //For better resolution, scale the buffered image up
	
	public Graphic(List<String> attributes, ColorMapper mapper) {
		this.attributes = attributes;
		this.mapper = mapper;
	}
	
	protected ColorMapper getMapper() {
		return mapper;
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
		CustomGraphic cg = customGraphics.get(nv);
		if(cg == null) {
			Shape shape = getShape(nv);
			Rectangle bounds = shape.getBounds();
			BufferedImage img = new BufferedImage(
					bounds.width * scaleImg, bounds.height * scaleImg, 
					BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = (Graphics2D)img.getGraphics();
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			paint(graphics, nv, new Rectangle(img.getWidth() - 1, img.getHeight() - 1));
			Paint imgPaint = new TexturePaint(img, bounds);
	
			cg = new CustomGraphic(
					shape, imgPaint, (byte) 0
			);
			customGraphics.put(nv, cg);
		}
		return cg;
	}

	public void saveLegend(File imgFile, String title) throws IOException {
		JPanel legend = createLegend();
		Dimension size = legend.getPreferredSize();
		
		BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.createGraphics();
		legend.paintAll(g);
		g.dispose();
		ImageIO.write(image, "jpeg", imgFile);
		image.flush();
	}
	
	protected JPanel createLegend() {
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("fill:pref:grow"));
		builder.setDefaultDialogBorder();
		for(String a : attributes) {
			JLabel l = new JLabel("- " + a);
			l.setOpaque(true);
			l.setBackground(Color.WHITE);
			builder.append(l);
		}
		builder.append(mapper.createLegend());
		JPanel panel = builder.getPanel();
		panel.setBackground(Color.WHITE);
		panel.setOpaque(true);
		return panel;
	}
}