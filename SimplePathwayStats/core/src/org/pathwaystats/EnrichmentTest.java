package org.pathwaystats;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Implements enrichment analysis for sets of xrefs.
 * (inspired by Ackermann & Strimmer, http://www.biomedcentral.com/1471-2105/10/47).
 * @author thomas
 */
public class EnrichmentTest {
	static final Logger log = Logger.getLogger("org.pathwaystats");
	
	private EnrichmentTest() { }
	
	/**
	 * 
	 * @param <P> Class of the set identifiers.
	 * @param <E> Class of the entity (e.g. gene, protein) identifiers.
	 * @param sets The xref sets, map keys are set identifiers.
	 * @param data The data that will be used to calculate set scores 
	 * (e.g. expression values, t-statistics or any other gene-level statistic).
	 * @param options Optional parameters for the test.
	 * @return The test results containing scores and p-values.
	 */
	public static <E, P> ResultMap<P> calculateResults(Multimap<P, E> sets, Multimap<E, Double> data, TestOptions options) {
		ResultMap<P> results = new ResultMap<P>();
		
		//Calculate set scores
		Map<P, Double> scores = new HashMap<P, Double>();
		for(P setId : sets.keySet()) {
			scores.put(setId, setScore(sets.get(setId), data, options.setScoreCalculator));
		}
		
		//Calculate p-values by xref sampling
		Map<P, Double> nrExceed = new HashMap<P, Double>();
		for(P setId : sets.keySet()) nrExceed.put(setId, 0.0);
		Multimap<E, Double> sampleData = new ArrayListMultimap<E, Double>();
		sampleData.putAll(data);
		
		log.info("Calculating enrichment for " + sets.keySet().size() + " sets and " + sampleData.size() + " measurements");
		for(int i = 0; i < options.numberPermutations; i++) {
			if(i % 100 == 0) log.fine("Permutation " + i);
			
			EnrichmentUtils.permuteMap(sampleData);
			
			for(P setId : sets.keySet()) {
				double score = scores.get(setId);
				if(!Double.isNaN(score)) {
					double sampleScore = setScore(sets.get(setId), sampleData, options.setScoreCalculator);
					if(options.setScoreComparator.exceeds(score, sampleScore)) {
						nrExceed.put(setId, nrExceed.get(setId) + 1);
					}
				}
			}
		}
		
		for(P setId : sets.keySet()) {
			double score = scores.get(setId);
			double pvalue = nrExceed.get(setId) / options.numberPermutations;
			if(Double.isNaN(score)) pvalue = Double.NaN;
			Collection<E> set = sets.get(setId);
			Set<E> measured = new HashSet<E>(set);
			measured.retainAll(data.keySet());
			results.addResult(new EnrichmentResult<P>(
					setId, set.size(), measured.size(), score, pvalue
			));
		}
		
		return results;
	}
	
	private static <E> double setScore(Collection<E> set, Multimap<E, Double> data, SetScoreCalculator scoreCalculator) {
		Multimap<E, Double> setData = new HashMultimap<E, Double>();
		for(E x : set) {
			if(data.containsKey(x)) {
				setData.putAll(x, data.get(x));
			}
		}
		return scoreCalculator.score(setData);
	}
	
	/**
	 * Set score will be the mean of all entity values in the set.
	 */
	public static final SetScoreCalculator CALC_SET_MEAN = new SetMeanCalculator();
	/**
	 * Set score will be the mean of all squared entity values in the set.
	 * Use this instead of CALC_SET_MEAN when you want to take into account both up and down 
	 * regulated genes, but your gene-level statistic is signed (e.g. fold-change).
	 */
	public static final SetScoreCalculator CALC_SET_MEAN_LOG10 = new SetMeanSquareCalculator();
	/**
	 * Set score will be the mean of all squared entity values in the set.
	 * Use this instead of CALC_SET_MEAN when you want to take into account both up and down 
	 * regulated genes, but your gene-level statistic is signed (e.g. fold-change).
	 */
	public static final SetScoreCalculator CALC_SET_MEAN_SQUARE = new SetMeanSquareCalculator();
	/**
	 * Set score will be the median of all entity values in the set.
	 */
	public static final SetScoreCalculator CALC_SET_MEDIAN = new SetMedianCalculator();
	
	/**
	 * Original score will exceed sampled score when sampled > original.
	 */
	public static final SetScoreComparator COMP_LARGEST = new SetScoreComparator() {
		public boolean exceeds(double original, double sampled) {
			return sampled > original;
		}
	};
	
	/**
	 * Original score will exceed sampled score when sampled < original.
	 */
	public static final SetScoreComparator COMP_SMALLEST = new SetScoreComparator() {
		public boolean exceeds(double original, double sampled) {
			return sampled < original;
		}
	};
	
	/**
	 * Optional parameters for enrichment test.
	 * @author thomas
	 */
	public static class TestOptions {
		int numberPermutations = 1000;
		SetScoreCalculator setScoreCalculator = CALC_SET_MEAN;
		SetScoreComparator setScoreComparator = COMP_LARGEST;
		
		/**
		 * Set the number of permutations to perform when calculating
		 * the p-value.
		 */
		public TestOptions numberPermutations(int v) { numberPermutations = v; return this; }
		/**
		 * Specify how the set score will be calculated. Use your own implemention of 
		 * {@link SetScoreCalculator} or one of the predefined calculators (see CALC_* fields).
		 */
		public TestOptions setScoreCalculator(SetScoreCalculator v) { setScoreCalculator = v; return this; }
		/**
		 * Specify when a sampled score exceeds an original score during permutation.
		 * Use your own implementation of {@link SetScoreComparator} or one of the 
		 * predifined comparators (see COMP_* fields).
		 */
		public TestOptions setScoreComparator(SetScoreComparator v) { setScoreComparator = v; return this; }
	}
	
	public interface SetScoreCalculator{
		public <E> double score(Multimap<E, Double> values);
	}
	
	public interface SetScoreComparator {
		/**
		 * Check if the sampled score exceeds the original score.
		 * @param original The original score
		 * @param sampled The sampled score
		 * @return true if the sampled score exceeds the original score.
		 */
		public boolean exceeds(double original, double sampled);
	}
	
	private static class SetMeanCalculator implements SetScoreCalculator {
		public <E> double score(Multimap<E, Double> values) {
			if(values.size() == 0) {
				return Double.NaN;
			}
			double sum = 0;
			for(double v : values.values()) sum += v;
			return sum / values.size();
		}
	}
	
	private static class SetMeanSquareCalculator implements SetScoreCalculator {
		public <E> double score(Multimap<E, Double> values) {
			if(values.size() == 0) {
				return Double.NaN;
			}
			double sum = 0;
			for(double v : values.values()) sum += v * v;
			return sum / values.size();
		}
	}
	
	private static class SetMedianCalculator implements SetScoreCalculator {
		public <E> double score(Multimap<E, Double> values) {
			if(values.size() == 0) return Double.NaN;
			List<Double> sorted = new ArrayList<Double>(values.values());
			Collections.sort(sorted);
			return sorted.get(sorted.size() / 2);
		}
	}
	
	public static class EnrichmentResult<K> {
		K id;
		private double pvalue;
		private double score;
		private int size;
		private int measured;
		
		public EnrichmentResult(K id, int size, int measured, double score, double pvalue) {
			this.pvalue = pvalue;
			this.score = score;
			this.id = id;
			this.size = size;
			this.measured = measured;
		}
		
		public K getId() {
			return id;
		}
		
		public double getPvalue() {
			return pvalue;
		}
		
		public double getScore() {
			return score;
		}

		public int getSize() {
			return size;
		}
		
		public int getMeasured() {
			return measured;
		}
	}
	
	public static class ResultMap<K> {
		Map<K, EnrichmentResult<K>> map = new HashMap<K, EnrichmentResult<K>>();
		
		void addResult(EnrichmentResult<K> r) {
			map.put(r.getId(), r);
		}
		
		public EnrichmentResult<K> getResult(K id) {
			return map.get(id);
		}
		
		public List<EnrichmentResult<K>> getSortedByPvalue() {
			List<EnrichmentResult<K>> sorted = new ArrayList<EnrichmentResult<K>>();
			sorted.addAll(map.values());
			Collections.sort(sorted,
					new Comparator<EnrichmentResult<K>>() {
						public int compare(EnrichmentResult<K> o1,
								EnrichmentResult<K> o2) {
							if(o1.pvalue == o2.pvalue) return 0;
							if(Double.isNaN(o1.pvalue)) return 1;
							if(Double.isNaN(o2.pvalue)) return -1;
							return o1.pvalue - o2.pvalue > 0 ? 1 : -1;
						}
					}
			);
			return sorted;
		}
		
		public void writeAsTxt(Writer out, Column... columns) throws IOException {
			//Write header
			boolean first = true;
			for(Column c : columns) {
				if(first) first = false;
				else out.append("\t");
				out.append(c.getHeader());
			}
			out.append("\n");
			
			//Write values
			for(EnrichmentResult<K> r : getSortedByPvalue()) {
				first = true;
				for(Column c : columns) {
					if(first) first = false;
					else out.append("\t");
					out.append(c.getValue(r));
				}
				out.append("\n");
			}
		}
		
		public static final Column COL_SCORE = new Column() {
			public String getHeader() { 
				return "score";
			}
			public String getValue(EnrichmentTest.EnrichmentResult<?> r) {
				return String.format ("%3.2f", (float)r.getScore());
			}
		};
		
		public static final Column COL_PVALUE = new Column() {
			public String getHeader() { 
				return "p-value";
			}
			public String getValue(EnrichmentTest.EnrichmentResult<?> r) { 
				return String.format ("%3.4f", (float)r.getPvalue());
			}
		};
		
		public static final Column COL_PATHWAY_ID = new Column() {
			public String getHeader() { 
				return "Pathway ID";
			}
			public String getValue(EnrichmentTest.EnrichmentResult<?> r) { 
				return r.getId().toString();
			}
		};
		
		public static final Column COL_SIZE = new Column() {
			public String getHeader() { 
				return "Nr in pathway";
			}
			public String getValue(EnrichmentTest.EnrichmentResult<?> r) { 
				return "" + r.getSize();
			}
		};
		
		public static final Column COL_MEASURED = new Column() {
			public String getHeader() { 
				return "Nr measured";
			}
			public String getValue(EnrichmentTest.EnrichmentResult<?> r) { 
				return "" + r.getMeasured();
			}
		};
	}

	
	public static interface Column {
		public String getHeader();
		public String getValue(EnrichmentResult<?> r);
	}
}
