package venn;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Basic structure for a venn diagram.
 * Draws 3 circles and set labels.
 * @author thomas
 */
class VennDiagramTemplate {
	VennCounts data;
	
	Rectangle bounds = new Rectangle(0, 0, 1200, 1200);
	double overlap = 0.8; //Overlap between the big circles
	int strokeSize = 3;
	Color strokeColor = Color.DARK_GRAY;
	int margin = 5;
	int labelMargin = 40;
	int titleMargin = 40;
	Font labelFont = new Font(Font.SANS_SERIF, Font.BOLD, 30);
	
	String title;
	Font titleFont = new Font(Font.SANS_SERIF, Font.BOLD, 30);
	
	Map<Integer, Area> partialAreas;
	Map<Shape, Integer> indexByShape;
	
	String[] labels;
	Color[] labelColors;
	
	public VennDiagramTemplate(VennCounts data) {
		this.data = data;
		init();
		refresh();
	}

	protected void init() {
		
	}
	
	protected void refresh() {
		initShapes();
	}

	public void setLabels(String...labels) {
		if(labels.length != data.getNrSets()) {
			throw new IllegalArgumentException(
					"Number of labels should be equal to number sets"
			);
		}
		if(this.labels == null || !this.labels.equals(labels)) {
			this.labels = labels;
			refresh();
		}
	}
	
	public void setTitle(String title) {
		if(this.title == null || !this.title.equals(title)) {
			this.title = title;
			refresh();
		}
	}
	
	protected void setLabelColors(Color...colors) {
		this.labelColors = colors;
	}
	
	private int getVennMargin() {
		return labels == null ? margin : margin + labelMargin;
	}
	
	private int getTitleMargin() {
		return title == null ? 0 : titleMargin;
	}
	
	public VennCounts getData() {
		return data;
	}
	
	public Rectangle getBounds() {
		return bounds;
	}
	
	public void setBounds(Rectangle bounds) {
		if(!this.bounds.equals(bounds)) {
			this.bounds = bounds;
			refresh();
		}
	}

	public int getIndex(Shape s) {
		return indexByShape.get(s);
	}
	
	public Area getPartialShape(int index){
		return partialAreas.get(index);
	}

	public Collection<Integer> getPartialShapeIndices() {
		return partialAreas.keySet();
	}
	
	public Collection<Area> getPartialShapes() {
		return partialAreas.values();
	}
	
	private void setPartialShape(Area union, int...index) {
		int i = data.getUnionIndex(index);
		partialAreas.put(i, union);
		indexByShape.put(union, i);
	}

	private void initShapes() {
		partialAreas = new HashMap<Integer, Area>();
		indexByShape = new HashMap<Shape, Integer>();

		int totalMargin = margin + getVennMargin();
		Rectangle mbounds = new Rectangle(
			bounds.x + totalMargin, bounds.y + totalMargin + getTitleMargin(),
			bounds.width - 2*totalMargin, bounds.height - 2*totalMargin - getTitleMargin()
		);
		double rx = mbounds.width / (4 - overlap);
		double ry = mbounds.height / (4 - overlap);
		Point2D[] positions = new Point2D[3];
		double shiftx = rx * 2 - rx * overlap;
		double a = shiftx / 2;
		double shifty = Math.sqrt(3*a*a) * (ry / rx);
		//Center if only 2 sets
		double x = mbounds.x;
		double y = mbounds.y;
		if(data.getNrSets() == 2) {
			y = mbounds.height / 2 - ry;
		}
		
		positions[0] = new Point2D.Double(x, y);
		positions[1] = new Point2D.Double(x + shiftx, y);
		positions[2] = new Point2D.Double(x + shiftx / 2, y + shifty);
		
		List<Area> fullAreas = new ArrayList<Area>();
		for(int i = 0; i < data.getNrSets(); i++) {
			Point2D p = positions[i];
			Ellipse2D circle = new Ellipse2D.Double(p.getX(), p.getY(), rx * 2, ry * 2);
			fullAreas.add(new Area(circle));
		}

		Area totalOverlap = null;
		if(data.getNrSets() > 2) {
			for(int i = 0; i < data.getNrSets(); i++) {
				if(totalOverlap == null) {
					totalOverlap = new Area(fullAreas.get(i));
				} else {
					totalOverlap.intersect(fullAreas.get(i));
				}
			}
			setPartialShape(totalOverlap, 0, 1, 2);
		}
		
		//Find overlapping areas
		for(int i = 0; i < data.getNrSets(); i++) {
			for(int j = i + 1; j < data.getNrSets(); j++) {
				Area match = new Area(fullAreas.get(i));
				if(totalOverlap != null) match.subtract(totalOverlap);
				match.intersect(fullAreas.get(j));
				setPartialShape(match, i, j);
			}
		}
		//Find non-overlapping areas
		for(int i = 0; i < data.getNrSets(); i++) {
			Area unique = new Area(fullAreas.get(i));
			if(totalOverlap != null) unique.subtract(totalOverlap);
			for(int j = 0; j < data.getNrSets(); j++) {
				if(j != i) unique.subtract(getPartialShape(data.getUnionIndex(i, j)));
			}
			setPartialShape(unique, i);
		}
	}

	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D)g.create();
		g2d.setClip(null);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		BasicStroke stroke = new BasicStroke(strokeSize);
		g2d.setStroke(stroke);
		for(Area a : partialAreas.values()) {
			//Draw the circle outline
			g2d.setColor(strokeColor);
			g2d.draw(a);
		}
		
		//Draw title
		if(title != null) {
			Rectangle2D titleBounds = titleFont.getStringBounds(title, g2d.getFontRenderContext());
			g2d.setFont(titleFont);
			g2d.drawString(
					title,
					(int)(bounds.getMaxX() / 2 - titleBounds.getWidth() / 2),
					(int)(bounds.y + titleBounds.getHeight())
			);
		}
		
		//Draw labels
		if(labels != null) {
			g2d.setFont(labelFont);
			if(labelColors != null) g2d.setColor(labelColors[0]);
			//label 1, top left
			Rectangle2D lb = labelFont.getStringBounds(labels[0], g2d.getFontRenderContext());
			int x = margin;
			int y = (int)(bounds.x + margin + lb.getHeight() + getTitleMargin());
			g2d.drawString(labels[0], x, y);
			
			//label 2, top right
			if(labelColors != null) g2d.setColor(labelColors[1]);
			lb = labelFont.getStringBounds(labels[1], g2d.getFontRenderContext());
			x = (int)(bounds.getMaxX() - margin - lb.getWidth());
			y = (int)(bounds.x + margin + lb.getHeight() + getTitleMargin());
			g2d.drawString(labels[1], x, y);
			
			//label 3, bottom center
			if(data.getNrSets() == 3) {
				if(labelColors != null) g2d.setColor(labelColors[2]);
				lb = labelFont.getStringBounds(labels[2], g2d.getFontRenderContext());
				x = (int)(bounds.x + bounds.width / 2 - lb.getWidth() / 2);
				y = (int)(bounds.getMaxY() - margin);
				g2d.drawString(labels[2], x, y);
			}
		}
	}
	
	public void saveImage(File imgFile, String formatName) throws IOException {
		BufferedImage img = new BufferedImage(
				(int)bounds.getWidth(), 
				(int)bounds.getHeight(), 
				BufferedImage.TYPE_INT_ARGB
		);
		paint(img.createGraphics());
		ImageIO.write(img, formatName, imgFile);
	}
}
