// PathVisio,
// a tool for data visualization and analysis using Biological Pathways
// Copyright 2006-2009 BiGCaT Bioinformatics
//
// Licensed under the Apache License, Version 2.0 (the "License"); 
// you may not use this file except in compliance with the License. 
// You may obtain a copy of the License at 
// 
// http://www.apache.org/licenses/LICENSE-2.0 
//  
// Unless required by applicable law or agreed to in writing, software 
// distributed under the License is distributed on an "AS IS" BASIS, 
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
// See the License for the specific language governing permissions and 
// limitations under the License.
//
package org.pathvisio.plugins.statistics;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pathvisio.debug.Logger;
import org.pathvisio.gex.ReporterData;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.plugins.statistics.PathwayMap.PathwayInfo;
import org.pathvisio.util.ProgressKeeper;
import org.pathvisio.util.Stats;
import org.pathvisio.visualization.colorset.Criterion;

/**
 * Calculates statistics on a set of Pathways, based on a predefined set of xrefs
 * rather than a criterion.
 * 
 * Note that the xrefs in the predefined set need to have the same datasource as the xrefs
 * in the dataset.
 * 
 * Copied from org.pathvisio.plugins.statistics.ZScoreCalculator.
 */
public class SetZScoreCalculator 
{			
	private PathwayMap pwyMap;
	private Map<Xref, RefInfo> dataMap;
	private final StatisticsResult result;
	private final ProgressKeeper pk;
	private Map<PathwayInfo, StatisticsPathwayResult> statsMap = 
		new HashMap<PathwayInfo, StatisticsPathwayResult>();

	private Set<Xref> positive;

	public SetZScoreCalculator(Set<Xref> positive, File pwDir, SimpleGex gex, IDMapper gdb, ProgressKeeper pk)
	{
		if (pk != null)
		{
			pk.setProgress (0);
			pk.setTaskName("Analyzing data");
		}
		this.positive = positive;

		result = new StatisticsResult();
		result.crit = new Criterion();
		result.stm = new StatisticsTableModel();
		result.stm.setColumns(new Column[] {Column.PATHWAY_NAME, Column.R, Column.N, Column.TOTAL, Column.PCT, Column.ZSCORE});
		result.pwDir = pwDir;
		result.gex = gex;
		result.gdb = gdb;
		this.pk = pk;
	}

	/**
	 * We have two slightly different methods for calculating zscores:
	 * MappFinder and Alternative
	 * 
	 * This base class abstracts the difference out so we can easily select
	 * one of the two methods.
	 */
	private abstract class Method
	{
		/**
		 * calculate result.bigN and result.bigR
		 */
		public abstract void calculateTotals () 
		throws IDMapperException;

		/**
		 * Do a permutation test to calculate permP and adjP
		 */
		public abstract void permute();
	}

	/**
	 * calculate n and r for a single pathway.
	 * 
	 * dataMap should already have been initialized
	 */
	private StatisticsPathwayResult calculatePathway(PathwayInfo pi) 
	{
		int cPwyMeasured = 0;
		int cPwyPositive = 0;
		int cPwyTotal = pi.srcRefs.size();

		for (Xref ref : pi.srcRefs)
		{
			RefInfo refInfo = dataMap.get(ref);
			if (refInfo.isMeasured()) cPwyMeasured++;
			if (refInfo.isPositive()) cPwyPositive++;
		}

		double zscore = Stats.zscore(cPwyMeasured, cPwyPositive, result.bigN, result.bigR); 
		StatisticsPathwayResult spr = new StatisticsPathwayResult(
				pi.file, pi.name,
				cPwyMeasured, cPwyPositive, cPwyTotal, zscore);
		return spr;
	}

	/**
	 * Information about the result of evaluating a criterion on a xref.
	 * 
	 * A given xref can have 0 or more measured probes associated with it,
	 * and each measured probe can be positive or not. 
	 */
	private static class RefInfo
	{
		final int probesMeasured;
		final int probesPositive;

		/**
		 * Initialize. 
		 * @param aProbesMeasured must be >= 0
		 * @param aProbesPostive must be >= 0, and <= aProbesMeasured.
		 */
		RefInfo(int aProbesMeasured, int aProbesPositive)
		{
			probesMeasured = aProbesMeasured;
			probesPositive = aProbesPositive;
			if (probesMeasured < 0) throw new IllegalArgumentException();
			if (probesPositive < 0) throw new IllegalArgumentException();
			if (probesPositive > probesMeasured) throw new IllegalArgumentException();
		}

		/**
		 * Calculate the positive fraction of probes. 
		 * E.g if 2 out of 3 probes are positive, count only 2/3.
		 * This is not the method Used by MAPPFinder.
		 */
		double getPositiveFraction()
		{
			return (double)probesPositive / (double)probesMeasured;
		}

		/** 
		 * returns true if probesPositive > 0, meaning that at
		 * least one of the probes is measured and positive.
		 * 
		 * If probesMeasured is false, this will be false too.
		 * 
		 * This is a very optimistic way of calling a ref positive, because there could be
		 * 1000 non-positive probes. But this is the method used by MAPPFinder.
		 * 
		 * For an alternative way, check getPositiveFraction
		 */
		boolean isPositive()
		{
			return probesPositive > 0;
		}

		/**
		 * returns true if at least one probe is measured
		 */
		boolean isMeasured()
		{
			return probesMeasured > 0;
		}		
	}

	/**
	 * Checks if the given ref evaluates positive for the criterion
	 * 
	 * Assumes that ref has already been cached earlier in a call to
	 * result.gex.cacheData(...)
	 */
	private RefInfo evaluateRef (Xref srcRef)
	{
		int cGeneTotal = 0;
		int cGenePositive = 0;
		List<ReporterData> rows = result.gex.getCachedData().getData(srcRef);

		if (rows != null)
		{
			cGeneTotal = rows.size();			
			for (ReporterData row : rows)
			{
				if (pk != null && pk.isCancelled()) return null;
				boolean eval = positive.contains(row.getXref());
				if (eval) cGenePositive++;
			}

		}

		return new RefInfo (cGeneTotal, cGenePositive);
	}

	/**
	 * Implementation of the Alternative method for calculating zscores.
	 * This takes the whole dataset into account.
	 */
	private class AlternativeMethod extends Method
	{
		@Override
		/**
		 * calculate bigN and bigR, based on the dataset.
		 * This goes through every row of the dataset and counts the number
		 * of total rows (bigN) and the number of rows meeting our criterion (bigR)
		 */
		public void calculateTotals() throws IDMapperException
		{
			int maxRow = result.gex.getNrRow();
			for (int i = 0; i < maxRow; ++i)
			{
				if (pk != null && pk.isCancelled()) return;
				ReporterData d = result.gex.getRow(i);
				result.bigN++;
				boolean eval = positive.contains(d.getXref());
				if (eval)
				{
					result.bigR++;
				}		

				if (pk != null) pk.setProgress ((int)(0.2 * (double)i / (double)maxRow * 100.0));
			}
		}

		@Override
		public void permute() 
		{
			//TODO: currently only implemented for MappFinderMethod.
			// adjP and permP will be 0 for all pathways
		}
	}

	/**
	 * Implementation of the MAPPFinder method for calculating zscores.
	 * This takes only the part of the dataset into account that maps to pathways.
	 */
	private class MappFinderMethod extends Method
	{		
		/**
		 * Shuffle the values of the map,
		 * so that each K, V pair is (likely) broken up,
		 * each K will (likely) get a new V.
		 * 
		 * @param map: the map to be permuted. This value is modified directly.
		 */
		private <K, V> void permuteMap (Map<K, V> map)
		{
			List<V> values = new ArrayList<V>();
			values.addAll (map.values());
			Collections.shuffle (values);

			int i = 0;
			for (K key : map.keySet())
			{
				map.put (key, values.get(i));
				i++;
			}
		}

		/**
		 * Perform a permutation test and calculate PermuteP values.
		 *
		 * permutes the data 1000 times while keeping the labels fixed.
		 * Calculate the rank of the actual zscore compared to the permuted zscores.
		 * Two-tailed test, so checks for very low z-scores as well as very high z-scores.
		 */
		public void permute()
		{
			// create a deep copy of dataMap.
			Map<Xref, RefInfo> dataMap2 = new HashMap<Xref, RefInfo>();
			for (Xref key : dataMap.keySet()) { dataMap2.put (key, dataMap.get(key)); }

			// we count the number of times a zscore is extremer,
			// i.e. further away from 0 than the actual zscore.
			Map<PathwayInfo, Integer> extremer = new HashMap<PathwayInfo, Integer>();

			for (PathwayInfo pi : pwyMap.pathways) extremer.put (pi, 0);

			for (int i = 0; i < 999; ++i)
			{
				permuteMap (dataMap2);

				for (PathwayInfo pi : pwyMap.pathways)
				{
					int cPwyMeasured = 0;
					int cPwyPositive = 0;

					for (Xref ref : pi.srcRefs)
					{
						RefInfo refInfo = dataMap2.get(ref);
						if (refInfo.isMeasured()) cPwyMeasured++;
						if (refInfo.isPositive()) cPwyPositive++;
					}
					double zscore = Stats.zscore(cPwyMeasured, cPwyPositive, result.bigN, result.bigR); 

					// compare absolutes -> two-tailed test
					if (Math.abs(zscore) > Math.abs((statsMap.get(pi)).getZScore()))
					{
						extremer.put (pi, extremer.get(pi) + 1);
					}					
				}
			}

			// report p-vals
			for (PathwayInfo pi : pwyMap.pathways)
			{
				double pval = (double)extremer.get(pi) / 1000.0;
				StatisticsPathwayResult spr = statsMap.get(pi);
				spr.permP = pval;
				System.out.println (spr.getProperty(Column.PATHWAY_NAME) + "\t" + spr.getZScore() + "\t" + pval);
			}
		}

		/**
		 * calculate bigN and bigR. This only takes the part of the dataset
		 * that maps to pathways, which leads to a calculation very similar to
		 * MAPPFinder.
		 */
		@Override
		public void calculateTotals() 
		{
			// go over all datanodes in all pathways
			for (Xref ref : dataMap.keySet())
			{		
				RefInfo refInfo = dataMap.get(ref);
				if (refInfo.isMeasured()) result.bigN++;
				if (refInfo.isPositive()) result.bigR++;
			}		
		}

	}

	private void calculateDataMap()
	{
		dataMap = new HashMap<Xref, RefInfo>();
		// go over all datanodes in all pathways
		for (Xref srcRef : pwyMap.getSrcRefs())
		{			
			if (pk != null && pk.isCancelled()) return;					
			RefInfo refInfo = evaluateRef (srcRef);				
			dataMap.put (srcRef, refInfo);			
		}		
	}

	private StatisticsResult calculate(Method m) throws IDMapperException
	{
		// read all pathways
		if (pk != null) 
		{
			if (pk.isCancelled()) return null;
			pk.setTaskName("Creating pathway list");
			pk.setProgress(0);
		}
		pwyMap = new PathwayMap (result.pwDir);

		// cache data for all pathways at once.
		if (pk != null) 
		{
			if (pk.isCancelled()) return null;
			pk.setTaskName("Reading dataset");
			pk.setProgress(20);
		}
		result.gex.cacheData(pwyMap.getSrcRefs(), null, result.gdb);

		// calculate dataMap
		if (pk != null) 
		{
			if (pk.isCancelled()) return null;
			pk.setTaskName("Calculating expression data");
			pk.setProgress(40);
		}
		calculateDataMap();

		if (pk != null) 
		{
			if (pk.isCancelled()) return null;
			pk.setTaskName("Calculating on dataset");
			pk.setProgress(60);
		}
		m.calculateTotals();
		Logger.log.info ("N: " + result.bigN + ", R: " + result.bigR);

		int i = 0;
		for (PathwayInfo pi : pwyMap.pathways)
		{
			if (pk != null)
			{
				if (pk.isCancelled()) return null;
				pk.setTaskName("Analyzing " + pi.file.getName());
				pk.setProgress((int)((0.6 + (0.2 * (double)i / (double)pwyMap.pathways.size())) * 100.0));
			}
			StatisticsPathwayResult spr = calculatePathway(pi);
			statsMap.put (pi, spr);
			if (spr != null) result.stm.addRow (spr);
		}

		if (pk != null) 
		{
			if (pk.isCancelled()) return null;
			pk.setTaskName("Calculating permutation P values");
			pk.setProgress(80);
		}
		m.permute();

		result.stm.sort();
		if (pk != null) 
		{
			pk.setProgress (100);
			pk.setTaskName("Done");
		}
		return result;
	}

	/** 
	 * calculate StatisticsResult, using
	 * the alternative method (not used by MappFinder)
	 * 
	 * This alternative method includes the whole dataset into the calculation
	 * of the N and R parameters for the zscore, not just the part
	 * of the dataset that maps to Pathways.
	 */
	public StatisticsResult calculateAlternative() throws IDMapperException
	{
		return calculate (new AlternativeMethod());
	}

	public StatisticsResult calculateMappFinder() throws IDMapperException
	{
		return calculate (new MappFinderMethod());
	}
}