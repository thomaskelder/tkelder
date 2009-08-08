package venn;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * A venn diagram where set counts are indicated
 * by little colored balls.
 * Visualization idea from http://www.neoformix.com/2008/TwitterVenn.html
 * @author thomas
 */
public class BallVennDiagram extends VennDiagramTemplate {
	int ballDiameter; //Initial ball diameter (will be scaled upon resizing)
	double spacing; //Factor to prevent balls from being too close to each other
	Color background = Color.WHITE;
	Random random; //Random generator for ball noise
	
	Font legendFont = new Font(Font.SANS_SERIF, Font.BOLD, 26);
	Font legendHeaderFont = new Font(Font.SANS_SERIF, Font.BOLD, 26);
	Color legendColor = Color.DARK_GRAY;
	String legendTitle;
	
	Map<Shape, Double> partialSurfaces;
	Map<Shape, Double> ballCounts;
	Map<Integer, Color> colors;
	Map<Shape, Set<Ellipse2D>> balls;
	
	public BallVennDiagram(VennCounts data) {
		super(data);
		refresh();
		setDefaultColors();
	}

	protected void init() {
		super.init();
		ballDiameter = 20;
		spacing = 1;
		random = new Random();
	}
	
	protected void refresh() {
		super.refresh();
		initSurfaces();
		initBallCounts();
		addBalls();
	}

	private Color getColor(Shape s) {
		return colors.get(getIndex(s));
	}

	public void setBallColor(int i, Color color) {
		colors.put(i, color);
	}
	
	private void setDefaultColors() {
		int alpha = 200;
		colors = new HashMap<Integer, Color>();
		Color[] defaultColors = new Color[] {
				new Color(54, 211, 255, alpha),
				new Color(174, 93, 178, alpha),
				new Color(102, 204, 0, alpha),
				new Color(255, 110, 165, alpha),
				new Color(255, 240, 110, alpha),
				new Color(255, 153, 0, alpha),
				new Color(182, 182, 182, alpha),
		};
		colors.put(getData().getUnionIndex(0), defaultColors[0]);
		colors.put(getData().getUnionIndex(1), defaultColors[1]);
		colors.put(getData().getUnionIndex(2), defaultColors[2]);
		colors.put(getData().getUnionIndex(0,1), defaultColors[3]);
		colors.put(getData().getUnionIndex(0,2), defaultColors[4]);
		colors.put(getData().getUnionIndex(1,2), defaultColors[5]);
		colors.put(getData().getUnionIndex(0,1,2), defaultColors[6]);
	}

	/**
	 * Estimates the maximum number of balls that will fit in this shape
	 */
	private double getMaxBallCount(Shape s) {
		//Divide by rectangular surface of balls
		double bsurface = ballDiameter * ballDiameter * spacing;
		return getSurface(s) / bsurface;
	}

	/**
	 * returns ball count for this shape.
	 * @param s
	 * @return
	 */
	private double getBallCount(Shape s) {
		return ballCounts.get(s);
	}

	private double getSurface(Shape s) {
		return partialSurfaces.get(s);
	}

	private BufferedImage createBufferedImage(Shape s) {
		//Rasterize the shape
		Rectangle bounds = getBounds();
		BufferedImage img = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D)img.getGraphics();
		g.setColor(Color.RED);
		g.setStroke(new BasicStroke(0));
		g.fill(s);
		return img;
	}

	private double calculateSurface(BufferedImage img) {
		//Count pixels
		int surface = 0;
		for(int x = 0; x < img.getWidth(); x++) {
			for(int y = 0; y < img.getHeight(); y++) {
				if(img.getRGB(x, y) != 0) surface++;
			}
		}
		return surface;
	}

	private void initBallCounts() {
		ballCounts = new HashMap<Shape, Double>();

		//Calculate ratios of total ball count with maximal ball count
		//Find maximum ratio, this is the shape we should normalize
		//the other shapes to

		double maxRatio = Double.MIN_VALUE;

		for(int i : getPartialShapeIndices()) {
			Shape shape = getPartialShape(i);
			int size = getData().getUnionCount(i);

			double ratio = size / getMaxBallCount(shape);
			maxRatio = Math.max(maxRatio, ratio);
		}

		//Now set the real ball count
		for(Integer i : getPartialShapeIndices()) {
			Shape shape = getPartialShape(i);
			double count = getData().getUnionCount(i);
			if(maxRatio > 1) {
				count = count / maxRatio;
			}
			ballCounts.put(shape, count);
		}
	}

	private void initSurfaces() {
		partialSurfaces = new HashMap<Shape, Double>();
		for(Area a : getPartialShapes()) {
			BufferedImage img = createBufferedImage(a);
			partialSurfaces.put(a, calculateSurface(img));
		}
	}

	private void addBalls() {
		balls = new HashMap<Shape, Set<Ellipse2D>>();

		for(Area a : getPartialShapes()) {
			addBalls(a);
		}
	}

	private void addBalls(Shape s) {
		Rectangle b = s.getBounds();
		int rw = b.width / ballDiameter;
		int rh = b.height / ballDiameter;

		Set<Point> visited = new HashSet<Point>();
		Set<Ellipse2D> ballSet = new HashSet<Ellipse2D>();

		//Use the pixels from the buffered image
		int attempts = 0;
		while(visited.size() < getBallCount(s) && attempts < 1000) {
			Point p = randomPoint(rw, rh);
			if(!visited.contains(p)) {
				Ellipse2D ball = new Ellipse2D.Double(
						b.x + p.x * ballDiameter, b.y + p.y * ballDiameter, ballDiameter, ballDiameter
				);
				//Make sure the ball fits in the shape
				if(s.contains(ball.getBounds())) {
					visited.add(p);
					//Add some noise
					double nf = 0.5;
					double rx = (nf*Math.random() - nf/2) * ballDiameter;
					double ry = (nf*Math.random() - nf/2) * ballDiameter;
					Ellipse2D noisyBall = new Ellipse2D.Double(
							ball.getX() + rx, ball.getY() + ry, ball.getWidth(), ball.getHeight()
					);
					ballSet.add(noisyBall);
				}
			}
			attempts++;
		}
		balls.put(s, ballSet);
	}

	private Point randomPoint(int mx, int my) {
		int x = random.nextInt(mx);
		int y = random.nextInt(my);
		return new Point(x, y);
	}

	public void paint(Graphics g) {
		setLabelColors(
				colors.get(data.getUnionIndex(0)),
				colors.get(data.getUnionIndex(1)),
				colors.get(data.getUnionIndex(2))
		);
		
		Graphics2D g2d = (Graphics2D)g.create();
		
		Rectangle bounds = getBounds();

		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setColor(background);
		g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

		for(Area a : getPartialShapes()) {
			g2d.setClip(a);
			//Draw the balls
			g2d.setColor(getColor(a));
			for(Shape b : balls.get(a)) {
				g2d.fill(b);
			}
		}
		
		super.paint(g);
		
		//Paint a legend
		g2d.setClip(null);
		g2d.setColor(legendColor);
		Rectangle2D headerBounds = new Rectangle2D.Double();
		//Find maximum width for legend bounds
		int hwidth = 0;
		for(int i : partialAreas.keySet()) {
			double nw = legendFont.getStringBounds(data.getUnionCountLabel(i), g2d.getFontRenderContext()).getWidth();
			hwidth = (int)Math.max(hwidth, nw);
		}
		if(legendTitle != null) {
			headerBounds = legendHeaderFont.getStringBounds(legendTitle, g2d.getFontRenderContext());
			if(hwidth > headerBounds.getWidth()) {
				headerBounds = new Rectangle2D.Double(0, 0, hwidth, headerBounds.getHeight());
			}
		} else {
			headerBounds = new Rectangle2D.Double(
				0, 0,
				hwidth, 0
			);
		}
		int vspacing = 3;
		int hspacing = 5;
		int textHeight = g2d.getFontMetrics(legendFont).getHeight();
		textHeight = Math.max(textHeight, ballDiameter);
		textHeight += vspacing;
		
		int height = (int)(textHeight * partialAreas.size() + headerBounds.getHeight() + 2*vspacing);
		int width = (int)(headerBounds.getWidth() + ballDiameter) + 2*hspacing;
		Rectangle lbounds = new Rectangle(
				(int)bounds.getMaxX() - margin - width,
				(int)bounds.getMaxY() - margin - height,
				width, height
		);
		
		//Paint header
		int y = lbounds.y + (int)headerBounds.getHeight() + vspacing;
		if(legendTitle != null) {
			g2d.setFont(legendHeaderFont);
			g2d.drawString(legendTitle, lbounds.x + hspacing, y - (int)headerBounds.getHeight() / 2);
		}
		//Paint balls + numbers
		g2d.setFont(legendFont);
		int x = lbounds.x + hspacing;
		paintLegendEntry(g2d, x, y, hspacing, 0);
		y += textHeight;
		paintLegendEntry(g2d, x, y, hspacing, 1);
		y += textHeight;
		if(data.getNrSets() == 3) {
			paintLegendEntry(g2d, x, y, hspacing, 2);
			y += textHeight;
		}
		paintLegendEntry(g2d, x, y, hspacing, 0, 1);
		y += textHeight;
		if(data.getNrSets() == 3) {
			paintLegendEntry(g2d, x, y, hspacing, 0, 2);
			y += textHeight;
			paintLegendEntry(g2d, x, y, hspacing, 1, 2);
			y += textHeight;
			paintLegendEntry(g2d, x, y, hspacing, 0, 1, 2);
			y += textHeight;
		}
	}
	
	private void paintLegendEntry(Graphics2D g, int x, int y, int hspacing, int...index) {
		int i = data.getUnionIndex(index);
		String nr = data.getUnionCountLabel(i);
		g.setColor(getColor(getPartialShape(i)));
		
		g.fillOval(x, y, ballDiameter, ballDiameter);
		x += ballDiameter + hspacing;
		
		Rectangle2D b = legendFont.getStringBounds(nr, g.getFontRenderContext());
		g.drawString(nr, x, y - ballDiameter / 2 + (int)b.getHeight());
	}
	
	public static void main(String[] args) {
		//Test
		Set<Integer> set1 = new HashSet<Integer>();
		Set<Integer> set2 = new HashSet<Integer>();
		Set<Integer> set3 = new HashSet<Integer>();
		for(int i = 0; i < 1000; i++) {
			set1.add((int)(Math.random() * 1000 - 500));
			set2.add((int)(Math.random() * 2000 - 1500));
			set3.add((int)(Math.random() * 5000));
		}
		List<Set<Integer>> sets = new ArrayList<Set<Integer>>();
		sets.add(set1); sets.add(set2); sets.add(set3);
		VennData<Integer> data = new VennData<Integer>(sets);
		final BallVennDiagram venn = new BallVennDiagram(data);
		venn.setLabels("set 1" , "set 2", "set 3");
		venn.setTitle("Test...");
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setSize(venn.getBounds().width, venn.getBounds().height);

		f.add(new JPanel() {
			protected void paintComponent(Graphics g) {
				venn.setBounds(new Rectangle(0, 0, getSize().width, getSize().height));
				venn.paint((Graphics2D)g);
			}
		}, BorderLayout.CENTER);
		f.setVisible(true);
	}
}
