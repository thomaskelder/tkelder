package org.pct.scripts;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.math.stat.descriptive.rank.Percentile;
import org.bridgedb.Xref;
import org.pct.PathwayCrossTalk;
import org.pct.io.GmlWriter;
import org.pct.model.AttributeKey;
import org.pct.model.Graph;
import org.pct.model.Network;
import org.pct.util.ArgsData;
import org.pct.util.ArgsParser;
import org.pct.util.ArgsData.DCrossTalk;
import org.pct.util.ArgsData.DIDMapper;
import org.pct.util.ArgsData.DNetworks;
import org.pct.util.ArgsData.DPathways;
import org.pct.util.ArgsData.DWeights;
import org.pct.util.ArgsParser.ACrossTalk;
import org.pct.util.ArgsParser.AHelp;
import org.pct.util.ArgsParser.AIDMapper;
import org.pct.util.ArgsParser.ANetwork;
import org.pct.util.ArgsParser.APathways;
import org.pct.util.ArgsParser.AWeights;
import org.pct.util.XrefData.WeightProvider;

import uk.co.flamingpenguin.jewel.cli.Option;

public class RunPps2TexpSeries {
	private final static Logger log = Logger.getLogger(RunPps2TexpSeries.class.getName());

	private interface T {
		public double t(double w);
	}
	
	static WeightProvider<Xref> min(final Map<Xref, Double> w) {
		return new WeightProvider<Xref>() {
			public double getWeight(Xref n1, Xref n2) {
				Double w1 = w.get(n1);
				Double w2 = w.get(n2);
				return Math.min(w1 == null ? 0 : w1, w2 == null ? 0 : w2);
			}
		};
	}
	
	static WeightProvider<Xref> avg(final Map<Xref, Double> w) {
		return new WeightProvider<Xref>() {
			public double getWeight(Xref n1, Xref n2) {
				Double w1 = w.get(n1);
				Double w2 = w.get(n2);
				double avg = ((w1 == null ? 0 : w1) + (w2 == null ? 0 : w2)) / 2.0;
				return avg;
			}
		};
	}
	
	static WeightProvider<Xref> tweights(final Map<Xref, Double> w, final T t) {
		return new WeightProvider<Xref>() {
			public double getWeight(Xref n1, Xref n2) {
				Double w1 = w.get(n1);
				Double w2 = w.get(n2);
				double avg = ((w1 == null ? 0 : w1) + (w2 == null ? 0 : w2)) / 2.0;
				return t.t(avg);
			}
		};
	}
	
	private interface Args extends AIDMapper, APathways, ACrossTalk, AWeights, ANetwork, AHelp {
		@Option(shortName = "o", description = "The output file name (without extension)")
		public File getOut();
	}
	
	public static void main(String[] args) {
		try {
			Args pargs = ArgsParser.parse(args, Args.class);
			DIDMapper didm = new DIDMapper(pargs);
			DPathways dpws = new DPathways(pargs, didm);
			DCrossTalk dct = new DCrossTalk(pargs, didm);
			DNetworks<Xref, String> dnw = ArgsData.loadCrossTalkInteractions(pargs);
			
			DWeights dw = new DWeights(pargs, didm);
			
			Map<Xref, Double> weightValues = dw.getTransformedWeightValues();
			Map<String, WeightProvider<Xref>> weightsMap = new HashMap<String, WeightProvider<Xref>>();

			PathwayCrossTalk ct = dct.getPathwayCrossTalk();
			
			double[] weightValuesArray = new double[weightValues.size()];
			int x = 0;
			for(Iterator<Double> it = weightValues.values().iterator(); it.hasNext();) {
				weightValuesArray[x++] = it.next();
			}
			
			Set<XrefPair> xrefPairs = new HashSet<XrefPair>();
			Graph<Xref,String> igraph = dnw.getMergedNetwork().getGraph();
			for(String e : igraph.getEdges()) {
				XrefPair xpi = new XrefPair(igraph.getFirst(e), igraph.getSecond(e));
				XrefPair xpo = new XrefPair(igraph.getSecond(e), igraph.getFirst(e));
				xrefPairs.add(xpi);
				xrefPairs.add(xpo);
			}
			weightsMap.put("Tavg", avg(weightValues));
			weightsMap.put("Tmin", min(weightValues));
			
			double[] sigm = new double[] { 20 };
			Percentile p = new Percentile(90); //Use the 90th percentile as threshold
			final double tau = p.evaluate(weightValuesArray);
			
			for(int i = 0; i < sigm.length; i++) {
				final double a = sigm[i];
				log.info("Parameters for sigmoid: a = " + a + ", t = " + tau);
				T t = new T() { public double t(double w) {
					return 1.0 / (1.0 + Math.exp(-a * (w - tau))); 
				}};
				weightsMap.put("sigm_" + (int)a, tweights(weightValues, t));
			}
			
			//Also include calculation without weights
			weightsMap.put("", new WeightProvider<Xref>() {
				public double getWeight(Xref n1, Xref n2) {
					return 1;
				}
			});
			ct.setWeightsMap(weightsMap);
			
			Network<String, String> ctn = ct.createCrossTalkNetwork(dpws.getPathways(), dnw.getMergedNetwork());

			//Add command line arguments for future reference
			String as = '"' + StringUtils.join(args, "\" \"") + '"';
			ctn.setNetworkAttribute(AttributeKey.Args.name(), as);
			
			PathwayCrossTalk.addPathwayAttributes(ctn, dpws.getPathwayFiles());
			
			PrintWriter out = new PrintWriter(new File(pargs.getOut() + ".gml"));
			GmlWriter.writeGml(out, ctn);
			out.close();
			out = new PrintWriter(new File(pargs.getOut() + ".xgmml"));
			ctn.writeToXGMML(out);
			out.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	static class XrefPair {
		Xref x1, x2;
		public XrefPair(Xref x1, Xref x2) {
			this.x1 = x1;
			this.x2 = x2;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((x1 == null) ? 0 : x1.hashCode());
			result = prime * result + ((x2 == null) ? 0 : x2.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			XrefPair other = (XrefPair) obj;
			if (x1 == null) {
				if (other.x1 != null)
					return false;
			} else if (!x1.equals(other.x1))
				return false;
			if (x2 == null) {
				if (other.x2 != null)
					return false;
			} else if (!x2.equals(other.x2))
				return false;
			return true;
		}
	}
}
