package org.pct.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pct.util.ArgsData.DIDMapper;
import org.pct.util.ArgsParser.AHelp;
import org.pct.util.ArgsParser.AIDMapper;
import org.pct.util.ArgsParser.AWeights;

import uk.co.flamingpenguin.jewel.cli.Option;

/**
 * Reads data from a txt file and maps it to xrefs.
 * @author thomas
 *
 */
public class XrefData {
	private final static Logger log = Logger.getLogger(XrefData.class.getName());
	
	public static Map<Xref, Double> readWeights(File f, IDMapper idm, boolean header, DataSource fixedDs, int col, int idCol, int dsCol, DataSource... tgtDs) throws IOException, IDMapperException {
		Map<Xref, List<Double>> rawValues = new HashMap<Xref, List<Double>>();

		BufferedReader in = new BufferedReader(new FileReader(f));
		String line = header ? in.readLine() : null;
		if(header) log.info("Reading column " + line.split("\t")[col]);
		while((line = in.readLine()) != null) {
			String[] cols = line.split("\t");
			DataSource ds = fixedDs == null ? DataSource.getBySystemCode(cols[dsCol]) : fixedDs;
			if("".equals(cols[idCol]) || ds == null) {
				log.warning("Skipping line '" + line + "', invalid xref");
			}
			double value = Double.parseDouble(cols[col]);
			Xref x = new Xref(cols[idCol], ds);
			if(!tgtDs.equals(ds)) {
				//Map the id
				for(Xref xx : idm.mapID(x, tgtDs)) {
					List<Double> v = rawValues.get(xx);
					if(v == null) rawValues.put(xx, v = new ArrayList<Double>());
					v.add(value);
				}
			} else {
				List<Double> v = rawValues.get(x);
				if(v == null) rawValues.put(x, v = new ArrayList<Double>());
				v.add(value);
			}
		}

		//Average values per xref
		final Map<Xref, Double> values = new HashMap<Xref, Double>();
		for(Xref x : rawValues.keySet()) {
			double avg = 0;
			for(double d : rawValues.get(x)) avg += d;
			avg = avg / rawValues.get(x).size();
			values.put(x, avg);
		}
		return values;
	}
	
	public static Map<Xref, Double> absoluteWeights(Map<Xref, Double> weights) {
		Map<Xref, Double> abs = new HashMap<Xref, Double>();
		for(Entry<Xref, Double> e : weights.entrySet()) {
			abs.put(e.getKey(), Math.abs(e.getValue()));
		}
		return abs;
	}
	
	/**
	 * Transform weights to lie within range 0,1.
	 */
	public static Map<Xref, Double> transformWeights(Map<Xref, Double> weights) {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		for(double d : weights.values()) {
			min = Math.min(d, min);
			max = Math.max(d, max);
		}
		Map<Xref, Double> norm = new HashMap<Xref, Double>();
		if(min < 0 || max > 1) {
			for(Xref x : weights.keySet()) {
				double d = weights.get(x);
				double n = (d - min) / (max - min);
				norm.put(x, n);
			}
		} else {
			norm.putAll(weights);
		}
		return norm;
	}
	
	/**
	 * Rank the values in the weights map using 
	 * standard competition ranking ("1224" ranking).
	 */
	public static Map<Xref, Double> rankWeights(Map<Xref, Double> weights) {
		List<Entry<Xref, Double>> list = new LinkedList<Entry<Xref, Double>>(weights.entrySet());
		Collections.sort(list, new Comparator<Entry<Xref, Double>>() {
			public int compare(Entry<Xref, Double> o1, Entry<Xref, Double> o2) {
				return o1.getValue().compareTo(o2.getValue());
			}
		});
		Map<Xref, Double> ranked = new LinkedHashMap<Xref, Double>();
		double i = 0;
		double prev = Double.NaN;
		for(Entry<Xref, Double> e : list) {
			double v = e.getValue();
			if(v != prev) i++;
			ranked.put(e.getKey(), i);
			prev = v;
		}
		return ranked;
	}
	
	
	public static void writeMappedData(File fin, File fout, IDMapper idm, boolean header, DataSource fixedDs, int idCol, int dsCol, DataSource... tgtDs) throws IOException, NumberFormatException, IDMapperException {
		BufferedReader in = new BufferedReader(new FileReader(fin));
		PrintWriter out = new PrintWriter(fout);
		
		if(header) {
			out.println("mapped_xref\t" + in.readLine());
		}
		String line;
		while((line = in.readLine()) != null) {
			String[] cols = line.split("\t");
			DataSource ds = fixedDs == null ? DataSource.getBySystemCode(cols[dsCol]) : fixedDs;
			if("".equals(cols[idCol]) || ds == null) {
				log.warning("Skipping line '" + line + "', invalid xref");
			}
			String remainder = "";
			for(String c : cols) remainder += "\t" + c;
			
			Xref x = new Xref(cols[idCol], ds);
			if(!tgtDs.equals(ds)) {
				//Map the id
				for(Xref xx : idm.mapID(x, tgtDs)) out.println(xx + remainder);
			} else {
				out.println(x + remainder);
			}
		}
		out.close();
	}
	
	public interface WeightProvider<N> {
		public double getWeight(N n1, N n2);
	}
	
	public static void main(String[] args) {
		try {
			Args pargs = ArgsParser.parse(args, Args.class);
			DIDMapper didm = new DIDMapper(pargs);
			
			DataSource ds = null;
			if(pargs.getWeightsSys() != null) ds = DataSource.getBySystemCode(pargs.getWeightsSys());
			
			log.info("Converting from " + ds + " to " + Arrays.toString(didm.getDataSources()));
			writeMappedData(pargs.getWeights(), pargs.getOut(), 
					didm.getIDMapper(), pargs.isWeightsHeader(), ds, pargs.getIdCol(), pargs.getSysCol(), didm.getDataSources()
			);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	interface Args extends AIDMapper, AWeights, AHelp {
		@Option(shortName = "o", description = "Output file")
		File getOut();
	}
}
