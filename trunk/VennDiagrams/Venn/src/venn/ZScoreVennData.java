package venn;

import java.util.List;
import java.util.Set;

public class ZScoreVennData<K> implements VennCounts {
	private VennData<K> refData;
	private VennData<K> data;
	
	private int bigN;
	private int bigR;
	
	public ZScoreVennData(List<Set<K>> sets, List<Set<K>> reference, int bigN, int bigR) {
		if(sets.size() != reference.size()) {
			throw new IllegalArgumentException(
				"Sets must have equal sizes."	
			);
		}
		data = new VennData<K>(sets);
		refData = new VennData<K>(reference);
		this.bigN = bigN;
		this.bigR = bigR;
	}
	
	public int getNrSets() {
		return data.getNrSets();
	}
	
	public double getZScore(int unionIndex) {
		//Calculate z-score for this intersection
		return zscore(refData.getUnionCount(unionIndex), data.getUnionCount(unionIndex));
	}
	
	public int getUnionCount(int unionIndex) {
		//Return the z-score * 100 for better precision (the counts are used relative)
		return (int)(getZScore(unionIndex) * 10);
	}
	
	public int getUnionIndex(int...i) {
		return data.getUnionIndex(i);
	}
	
	public String getUnionCountLabel(int unionIndex) {
		int dataNr = data.getUnionCount(unionIndex);
		int refNr = refData.getUnionCount(unionIndex);
		double z = getZScore(unionIndex);
		return String.format ("%3.2f", z) + " (" + dataNr + "/" + refNr + ")";
	}
	
	private double zscore (double n, double r)
	{
		double N = (double)bigN;
		double R = (double)bigR;
		
		double f1 = r - (n * (R / N));
		double f2 = R / N;
		double f3 = 1.0 - (R / N);
		double f4 = 1.0 - ((n - 1) / (N - 1));
		
		return f1 / Math.sqrt (n * f2 * f3 * f4);
	}
}
