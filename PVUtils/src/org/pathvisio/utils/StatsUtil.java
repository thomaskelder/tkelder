package org.pathvisio.utils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pathvisio.debug.Logger;
import org.pathvisio.gex.GexManager;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.plugins.statistics.Column;
import org.pathvisio.plugins.statistics.SetZScoreCalculator;
import org.pathvisio.plugins.statistics.StatisticsPathwayResult;
import org.pathvisio.plugins.statistics.StatisticsResult;
import org.pathvisio.plugins.statistics.ZScoreCalculator;
import org.pathvisio.visualization.colorset.Criterion;

public class StatsUtil {
	/**
	 * Write detailed results for total (including n, r) and append with up/down and difference columns.
	 * @throws IOException 
	 */
	public static void writeDetailed(StatisticsResult total, StatisticsResult up, StatisticsResult down, File file) throws IOException {
		Writer out = new BufferedWriter(new FileWriter(file));
		Column[] saveColumns = new Column[] {
				Column.PATHWAY_NAME, Column.FILE_NAME, Column.R, 
				Column.N, Column.TOTAL, Column.PCT, Column.ZSCORE, 
		};
		
		out.append("Statistics results for " + new Date() + "\n");
		out.append("Dataset: " + total.getGex().getDbName() + "\n");
		out.append("Pathway directory: " + total.getPathwayDir() + "\n");
		out.append("Gene database: " + total.getIDMapper() + "\n");
		out.append("Rows in data (N): " + total.getBigN() + "\n");
		out.append("Rows meeting criterion (R): " + total.getBigR() + "\n");
		out.append("Criterion: " + total.getCriterion().getExpression() + "\n");
		out.append("Criterion up: " + up.getCriterion().getExpression() + "\n");
		out.append("Criterion down: " + down.getCriterion().getExpression() + "\n");
		out.append("\n");
		
		boolean first = true;
		for(Column c : saveColumns) {
			if(!first)  out.append("\t");
			first = false;
			out.append(c.getTitle());
		}
		out.append("\tzup");
		out.append("\tzdown");
		out.append("\tzup-zdown");
		out.append("\n");
		
		Map<File, StatisticsPathwayResult[]> updownResults = extractResults(new StatisticsResult[] { up, down }, new FilterZScoreOptions());
		
		
		for(StatisticsPathwayResult tr : total.getPathwayResults()) {
			first = true;
			for (Column col : saveColumns) {
				if(!first) out.append("\t");
				first = false;
				out.append(tr.getProperty(col));
			}
			//Append the up/down and difference cols
			String szup = updownResults.get(tr.getFile())[0].getProperty(Column.ZSCORE);
			String szdown = updownResults.get(tr.getFile())[1].getProperty(Column.ZSCORE);
			out.append("\t");
			out.append(szup);
			out.append("\t");
			out.append(szdown);
			out.append("\t");
			out.append(Double.parseDouble(szup) - Double.parseDouble(szdown) + "");
			out.append("\n");
		}
	}
	
	/**
	 * Write summarized results (z-scores only) and append each column with difference between up/down.
	 */
	public static void writeSummary(StatisticsResult[] total, StatisticsResult[] up, StatisticsResult[] down, String[] headers, File file, FilterZScoreOptions options) throws IOException {
		Writer out = new BufferedWriter(new FileWriter(file));
		out.append("Pathway\tFile");
		for(String h : headers) {
			out.append("\t");
			out.append(h);
			out.append("\t");
			out.append(h + "_updowndiff");
		}

		Map<File, StatisticsPathwayResult[]> totalResults = extractResults(total, options);
		Map<File, StatisticsPathwayResult[]> upResults = extractResults(up, options);
		Map<File, StatisticsPathwayResult[]> downResults = extractResults(down, options);

		for(File f : totalResults.keySet()) {
			StatisticsPathwayResult[] frTotal = totalResults.get(f);
			StatisticsPathwayResult[] frUp = upResults.get(f);
			StatisticsPathwayResult[] frDown = downResults.get(f);
			if(!Double.isNaN(options.threshold)) {
				boolean include = false;
				for(int i = 0; i < frUp.length; i++) {
					double z = Double.parseDouble(frTotal[i].getProperty(Column.ZSCORE));
					if(Double.isInfinite(options.threshold) || z >= options.threshold) {
						include = true;
						break;
					}
				}
				if(!include) continue;
			}
			out.append("\n");
			out.append(frTotal[0].getProperty(Column.PATHWAY_NAME));
			out.append("\t");
			out.append(f.getName());
			for(int i = 0; i < frTotal.length; i++) {
				double ztotal = Double.parseDouble(frTotal[i].getProperty(Column.ZSCORE));
				double zup = Double.parseDouble(frUp[i].getProperty(Column.ZSCORE));
				double zdown = Double.parseDouble(frDown[i].getProperty(Column.ZSCORE));
				if(Double.isNaN(zup)) zup = 0;
				if(Double.isNaN(zdown)) zdown = 0;
				double zdiff = zup - zdown;
				out.append("\t" + ztotal);
				out.append("\t" + zdiff);
			}
		}
		out.close();
	}
	
	/**
	 * Write summarized results, sign and optionally digitalize z-scores (depending on options).
	 */
	public static void writeCategorized(StatisticsResult[] total, StatisticsResult[] up, StatisticsResult[] down, String[] headers, File file, FilterZScoreOptions options) throws IOException {
		if(Double.isNaN(options.threshold)) {
			throw new IllegalArgumentException("You should set a valid options.threshold for categorization.");
		}
		Writer out = new BufferedWriter(new FileWriter(file));
		out.append("Pathway\tFile");
		for(String h : headers) {
			out.append("\t");
			out.append(h);
		}

		Map<File, StatisticsPathwayResult[]> totalResults = extractResults(total, options);
		Map<File, StatisticsPathwayResult[]> upResults = extractResults(up, options);
		Map<File, StatisticsPathwayResult[]> downResults = extractResults(down, options);

		for(File f : totalResults.keySet()) {
			StatisticsPathwayResult[] frTotal = totalResults.get(f);
			StatisticsPathwayResult[] frUp = upResults.get(f);
			StatisticsPathwayResult[] frDown = downResults.get(f);
			boolean include = false;
			for(int i = 0; i < frTotal.length; i++) {
				double z = Double.parseDouble(frTotal[i].getProperty(Column.ZSCORE));
				if(z >= options.threshold) {
					include = true;
					break;
				}
			}
			if(!include) continue;
			out.append("\n");
			out.append(frTotal[0].getProperty(Column.PATHWAY_NAME));
			out.append("\t");
			out.append(f.getName());
			for(int i = 0; i < frTotal.length; i++) {
				out.append("\t");
				double ztotal = Double.parseDouble(frTotal[i].getProperty(Column.ZSCORE));
				if(ztotal < 0 || Double.isNaN(ztotal)) ztotal = 0; //Cutoff negative z-scores
				double zup = Double.parseDouble(frUp[i].getProperty(Column.ZSCORE));
				if(Double.isNaN(zup)) zup = Double.MIN_VALUE;
				double zdown = Double.parseDouble(frDown[i].getProperty(Column.ZSCORE));
				if(Double.isNaN(zdown)) zdown = Double.MIN_VALUE;
				double cat = ztotal;
				double zdiff = zup - zdown;
				
				if(zdiff < 0) cat = -ztotal;
				if (Math.abs(zdiff) < options.minDifferenceForSign*ztotal){
					Logger.log.info("Unable to determine sign for " + frTotal[i].getFile());
					cat = Double.NaN; //Unable to determine sign
				}
				if(options.digitalize) {
					if(ztotal >= options.threshold) {
						cat = cat > 0 ? 1 : 0;
					} else {
						cat = 0;
					}
				}
				out.append(cat + "");
			}
		}
		out.close();
	}
	
	/**
	 * Combine results for up and down in single file and give
	 * all z-scores for down a negative sign.
	 * @deprecated: this method doesn't take into account pathways of which the direction is 
	 * undetermined (where the difference between z-up and z-down is not large).
	 */
	public static void writeSigned(StatisticsResult[] up, StatisticsResult[] down, String[] headers, File file, FilterZScoreOptions options) throws IOException {
		Writer out = new BufferedWriter(new FileWriter(file));
		out.append("Pathway\tFile");
		for(String h : headers) {
			out.append("\t");
			out.append(h);
		}
		
		Map<File, StatisticsPathwayResult[]> upResults = extractResults(up, options);
		Map<File, StatisticsPathwayResult[]> downResults = extractResults(down, options);
		
		for(File f : downResults.keySet()) {
			StatisticsPathwayResult[] frUp = upResults.get(f);
			StatisticsPathwayResult[] frDown = downResults.get(f);
			if(!Double.isNaN(options.threshold)) {
				boolean include = false;
				for(int i = 0; i < frUp.length; i++) {
					double zup = Double.parseDouble(frUp[i].getProperty(Column.ZSCORE));
					double zdown = Double.parseDouble(frDown[i].getProperty(Column.ZSCORE));
					double z = Math.max(zup, zdown);
					if(Double.isInfinite(options.threshold) || z >= options.threshold) {
						include = true;
						break;
					}
				}
				if(!include) continue;
			}
			out.append("\n");
			out.append(frUp[0].getProperty(Column.PATHWAY_NAME));
			out.append("\t");
			out.append(f.getName());
			for(int i = 0; i < frUp.length; i++) {
				out.append("\t");
				double zup = Double.parseDouble(frUp[i].getProperty(Column.ZSCORE));
				if(options.negativeToZero && zup < 0) zup = 0;
				double zdown = Double.parseDouble(frDown[i].getProperty(Column.ZSCORE));
				if(options.negativeToZero && zdown < 0) zdown = 0;
				//out.append(fr[i].getProperty(Column.ZSCORE));
				double z = Math.max(zup, zdown);
				if(z == zdown) z = -z;
				out.append(z + "");
			}
		}
		out.close();
	}
	
	/**
	 * Write multiple z-scores for multiple criteria to a single file.
	 */
	public static void write(StatisticsResult[] results, String[] headers, File file, FilterZScoreOptions options) throws IOException {
		Writer out = new BufferedWriter(new FileWriter(file));
		out.append("Pathway\tFile");
		for(String h : headers) {
			out.append("\t");
			out.append(h);
		}
		
		Map<File, StatisticsPathwayResult[]> pathwayResults = extractResults(results, options);
		
		for(File f : pathwayResults.keySet()) {
			StatisticsPathwayResult[] fr = pathwayResults.get(f);
			if(!Double.isNaN(options.threshold)) {
				boolean include = false;
				for(int i = 0; i < fr.length; i++) {
					double z = Double.parseDouble(fr[i].getProperty(Column.ZSCORE));
					if(Double.isInfinite(options.threshold) || z >= options.threshold) {
						include = true;
						break;
					}
				}
				if(!include) continue;
			}
			out.append("\n");
			out.append(fr[0].getProperty(Column.PATHWAY_NAME));
			out.append("\t");
			out.append(f.getName());
			for(int i = 0; i < results.length; i++) {
				out.append("\t");
				double z = Double.parseDouble(fr[i].getProperty(Column.ZSCORE));
				if(options.negativeToZero && z < 0) z = 0;
				out.append(z + "");
			}
		}
		
		out.close();
	}
	
	public static StatisticsResult calculateZscores(String expr, File pwDir, GexManager gexMgr, IDMapper gdb) throws IDMapperException {
		Criterion crit = new Criterion();
		crit.setExpression(expr, gexMgr.getCurrentGex().getSampleNames());
		ZScoreCalculator calc = new ZScoreCalculator(crit, pwDir, gexMgr.getCachedData(), gdb, null);
		return calc.calculateAlternative();
	}
	
	/**
	 * @deprecated Use with GexManager instead for better performance (uses cache)
	 */
	public static StatisticsResult calculateZscores(String expr, File pwDir, SimpleGex gex, IDMapper gdb) throws IDMapperException {
		GexManager gexMgr = new GexManager();
		gexMgr.setCurrentGex(gex);
		return calculateZscores(expr, pwDir, gexMgr, gdb);
	}
	
	public static StatisticsResult calculateZscoresFromSet(Set<Xref> positive, File pwDir, SimpleGex gex, IDMapper gdb) throws IDMapperException {
		SetZScoreCalculator calc = new SetZScoreCalculator(positive, pwDir, gex, gdb, null);
		return calc.calculateAlternative();
	}
	
	public static Map<String, Double> resultsToMap(StatisticsResult result, Column col, FilterZScoreOptions options) {
		Map<String, Double> scores = new HashMap<String, Double>();
		for(StatisticsPathwayResult pr : result.getPathwayResults()) {
			scores.put(pr.getFile().toString(), Double.parseDouble(pr.getProperty(col)));
		}
		return scores;
	}
	
	public static Map<String, Double> parseZScoreResults(File file, Column col) throws IOException {
		int skip = 8; //Skip first 8 lines

		BufferedReader in = new BufferedReader(new FileReader(file));
		Map<String, Double> zscores = new HashMap<String, Double>();

		for(int i = 0; i < skip; i++) in.readLine();
		//Read header
		String[] header = in.readLine().split("\t");
		int valueCol = -1;
		for(int i = 0; i < header.length; i++) if(header[i].equals(col.getTitle())) {
			valueCol = i; break;
		}
		String line = "";
		while((line = in.readLine()) != null) {
			String[] cols = line.split("\t");
			zscores.put(cols[0], Double.parseDouble(cols[valueCol]));
		}
		in.close();
		return zscores;
	}
	
	private static Map<File, StatisticsPathwayResult[]> extractResults(StatisticsResult[] results, FilterZScoreOptions options) {
		Map<File, StatisticsPathwayResult[]> resultsMap = new HashMap<File, StatisticsPathwayResult[]>();
		for(StatisticsPathwayResult r : results[0].getPathwayResults()) {
			int n = Integer.parseInt(r.getProperty(Column.N));
			if(n < options.minSize || n > options.maxSize) {
				Logger.log.info("Skipping " + r.getFile().getName() + " (" + n + ")");
				continue;
			}
			
			StatisticsPathwayResult[] resArray = new StatisticsPathwayResult[results.length];
			for(int i = 0; i < results.length; i++) {
				//linear search...not fast but it will do
				for(StatisticsPathwayResult ir : results[i].getPathwayResults()) {
					if(ir.getFile().equals(r.getFile())) {
						resArray[i] = ir;
						break;
					}
				}
			}
			resultsMap.put(r.getFile(), resArray);
		}
		return resultsMap;
	}
	
	public static class FilterZScoreOptions {
		public double threshold = Double.NaN;
		public int minSize = Integer.MIN_VALUE;
		public int maxSize = Integer.MAX_VALUE;
		public boolean negativeToZero = false;
		public double minDifferenceForSign = 0;
		public boolean digitalize = false;
		
		/**
		 * Used by writeCategorized. The minimum difference between the up and down z-scores
		 * in order to be able to assign a sign.
		 * If 0, the maximum z-score will be used to determine the sign.
		 */
		public FilterZScoreOptions minDifferenceRatioForSign(double v) { minDifferenceForSign = v; return this; }
		
		/**
		 * Used by writeCategorized. Whether to digitalize the results. If true,
		 * the z-score will be digitalized to -1, 0 or 1. Otherwise, the signed total
		 * z-score will be used.
		 */
		public FilterZScoreOptions digitalize(boolean v) { digitalize = v; return this; }
		public FilterZScoreOptions threshold(double v) { threshold = v; return this; }
		public FilterZScoreOptions minSize(int v) { minSize = v; return this; }
		public FilterZScoreOptions maxSize(int v) { maxSize = v; return this; }
		public FilterZScoreOptions negativeToZero(boolean v) { negativeToZero = v; return this; }
	}
}
