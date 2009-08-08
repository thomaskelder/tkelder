package venn;

import java.util.List;
import java.util.Set;

public class RelativeVennData<K> implements VennCounts {
	private VennData<K> refData;
	private VennData<K> data;
	
	public RelativeVennData(List<Set<K>> sets, List<Set<K>> reference) {
		if(sets.size() != reference.size()) {
			throw new IllegalArgumentException(
				"Sets must have equal sizes."	
			);
		}
		data = new VennData<K>(sets);
		refData = new VennData<K>(reference);
	}
	
	public int getNrSets() {
		return data.getNrSets();
	}
	
	public int getSetCount(int setIndex) {
		//Normalize to reference
		return (int)(100 * doubleRatio(data.getSetCount(setIndex), refData.getSetCount(setIndex)));
	}
	
	public int getUnionCount(int unionIndex) {
		//Normalize to reference
		return (int)(100 * doubleRatio(data.getUnionCount(unionIndex), refData.getUnionCount(unionIndex)));
	}
	
	public int getUnionIndex(int...i) {
		return data.getUnionIndex(i);
	}
	
	public String getUnionCountLabel(int unionIndex) {
		int dataNr = data.getUnionCount(unionIndex);
		int refNr = refData.getUnionCount(unionIndex);
		return getUnionCount(unionIndex) + "% (" + dataNr + "/" + refNr + ")";
	}
	
	private double doubleRatio(int i, int j) {
		return (double)i / j;
	}
}
