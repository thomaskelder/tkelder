package gcharts;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.visualization.colorset.Criterion;
import org.pathvisio.visualization.colorset.Criterion.CriterionException;

import com.googlecode.charts4j.Color;
import com.googlecode.charts4j.DataEncoding;
import com.googlecode.charts4j.Fills;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.LegendPosition;
import com.googlecode.charts4j.VennDiagram;

public class GChartsGexVenn {
	private static final String[] defaultColors = new String[] { "FF9955", "7FC659", "75A4FB" };
	
	SimpleGex data;
	
	Map<String, Set<String>> matches;
	Map<String, String> colors;
	Map<String, String> labels;
	
	public GChartsGexVenn(SimpleGex data) {
		this.data = data;
	}
	
	public void calculateMatches(String[] criteria) throws IDMapperException, CriterionException {
		matches = new HashMap<String, Set<String>>();
		
		for(String expr : criteria) {
			Criterion c = new Criterion();
			c.setExpression(expr, data.getSampleNames());
			
			Set<String> match = new HashSet<String>();
			matches.put(expr, match);
			
			int maxRow = data.getNrRow();
			for(int i = 0; i < maxRow; i++) {
				Map<String, Object> sdata = data.getRow(i).getByName();
				Xref xref = data.getRow(i).getXref();
				if(c.evaluate(sdata)) match.add(xref.getId());
			}
		}
	}
	
	public void calculateZScoreMatches(Map<String, Map<String, Double>> zscores, double min, double max) {
		matches = new HashMap<String, Set<String>>();
		
		for(String crit : zscores.keySet()) {
			Map<String, Double> critScores = zscores.get(crit);
			Set<String> pathways = new HashSet<String>();
			for(String pw : critScores.keySet()) {
				if(critScores.get(pw) >= min && critScores.get(pw) <= max) {
					pathways.add(pw);
				}
			}
			matches.put(crit, pathways);
		}
	}
	
	public void setColors(String[] criteria, String[] clrs) {
		colors = new HashMap<String, String>();
		for(int i = 0; i < criteria.length; i++) colors.put(criteria[i], clrs[i]);
	}
	
	public void setLabels(String[] criteria, String[] lbls) {
		labels = new HashMap<String, String>();
		for(int i = 0; i < criteria.length; i++) labels.put(criteria[i], lbls[i]);
	}
	
	public static <E> BufferedImage createDiagram(String title, 
			Set<E> match1, Set<E> match2, Set<E> match3, 
			String l1, String l2, String l3) throws MalformedURLException, IOException {
		int max = Math.max(match1.size(), match2.size());
		max = Math.max(max, match3.size());
		return createDiagram(title, match1, match2, match3, l1, l2, l3, 
				defaultColors[0], defaultColors[1], defaultColors[2], max);
	}

	private static <E> BufferedImage createDiagram(String title, 
			Set<E> match1, Set<E> match2, Set<E> match3, 
			String l1, String l2, String l3,
			String c1, String c2, String c3,
			int relativeToMax) throws MalformedURLException, IOException {
		if(match3 == null) match3 = new HashSet<E>();
		
		double n1 = match1.size();
		double n2 = match2.size();
		double n3 = match3 != null ? match3.size() : 0;
		double ntotal = n1 + n2 + n3;
		double max = relativeToMax;
		
		HashSet<E> match123 = new HashSet<E>(match1);
		match123.retainAll(match2);
		match123.retainAll(match3);
		HashSet<E> match12 = new HashSet<E>(match1);
		match12.retainAll(match2);
		match12.removeAll(match123);
		HashSet<E> match23 = new HashSet<E>(match2);
		match23.retainAll(match3);
		match23.removeAll(match123);
		HashSet<E> match13 = new HashSet<E>(match1);
		match13.retainAll(match3);
		match13.removeAll(match123);

		VennDiagram chart = GCharts.newVennDiagram(
				(n1 / max) * 100,
				(n2 / max) * 100,
				(n3 / max) * 100,
				(match12.size() / (n1 + n2)) * 100,
				(match13.size() / (n1 + n3)) * 100,
				(match23.size() / (n2 + n3)) * 100,
				(match123.size() / ntotal) * 100
		);
		
		if(title != null) chart.setTitle(title);
        chart.setSize(600, 500);
        chart.setMargins(5, 5, 5, 5);
        chart.setBackgroundFill(Fills.newSolidFill(Color.WHITE));
        chart.setLegendPosition(LegendPosition.RIGHT);
        chart.setDataEncoding(DataEncoding.TEXT);
        
        String url = chart.toURLString();
        //Add legend manually, since we want more than 3 labels
        //Add default labels
        url += "&chdl=";
        String labels = "";
        labels += "a: " + l1 + " (" + (int)n1 + ")";
        labels += "|b: " + l2 + " (" + (int)n2 + ")";
        if(match3.size() > 0) labels += "|c: " + l3 + " (" + (int)n3 + ")";
        //Add labels for intersect areas
        labels += "|Area counts:";
        labels += "|a: " + (match1.size() - match12.size() - match13.size() - match123.size()); //a
        labels += "|b: " + (match2.size() - match12.size() - match23.size() - match123.size()); //b
        if(match3.size() > 0) labels += "|c: " + (match3.size() - match13.size() - match23.size() - match123.size()); //c
        if(match3.size() > 0)labels += "|abc: " + match123.size(); //abc
        labels += "|ab: " + match12.size(); //ab
        if(match3.size() > 0) labels += "|ac: " + match13.size(); //ac
        if(match3.size() > 0) labels += "|bc: " + match23.size(); //bc
        labels += "|total: " + (int)ntotal;
        url += URLEncoder.encode(labels, "UTF-8");
        //Add colors
        String white = ",FFFFFF";
        url += "&chco=";
        String colors = "";
        colors += c1; //Orange
        colors += "," + c2; //Green
        if(match3.size() > 0) colors += "," + c3; //Blue
        colors += white + white + white + white + white + (match3.size() > 0 ? white + white + white + white : "");
        url += URLEncoder.encode(colors, "UTF-8");
        System.out.println(url);
        //Add the chart to an image
        return ImageIO.read(new URL(url));
	}
	
	public BufferedImage createDiagram(String title, String c1, String c2, String c3) throws IDMapperException, CriterionException, MalformedURLException, IOException {
		int max = 0;
		for(Set<String> m : matches.values()) max = Math.max(max, m.size());
		
		String clr1 = defaultColors[0]; 
		String clr2 = defaultColors[1]; 
		String clr3 = defaultColors[2];
		if(colors != null) {
			clr1 = colors.get(c1);
			clr2 = colors.get(c2);
			clr3 = colors.get(c3);
		}

		return createDiagram(
				title,
				matches.get(c1), matches.get(c2), matches.get(c3),
				labels == null ? c1 : labels.get(c1), 
				labels == null ? c2 : labels.get(c2), 
				labels == null ? c3 : labels.get(c3), 
				clr1, clr2, clr3, max
		);
	}
	
	public void writeMatchTxt(Writer out) throws IOException {
		for(String crit : matches.keySet()) {
			for(String m : matches.get(crit)) {
				out.append(m);
				out.append("\t");
				out.append(labels == null ? crit : labels.get(crit));
				out.append("\n");
			}
		}
	}
}
