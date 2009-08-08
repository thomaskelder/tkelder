package venn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VennData<K> implements VennCounts {
	List<Set<K>> sets;
	int[] setIndices;
	int[] sizes;
	
	//Sets for each part of the venn diagram
	//Index is bitwise addition of set combinations
	//So for union 1,2,3 use index 2^0 & 2^1 & 2^2
	List<Set<K>> partialSets;

	public VennData(List<Set<K>> sets) {
		if(sets.size() < 2 || sets.size() > 3) {
			throw new IllegalArgumentException("Number of sets should be either 2 or 3");
		}
		this.sets = sets;
		setIndices = new int[sets.size()];
		for(int i = 0; i < sets.size(); i++) setIndices[i] = i;
		
		partialSets = new ArrayList<Set<K>>();
		int max = 0;
		for(int i = 0; i < sets.size(); i++) max |= (int)Math.pow(2, i);
		for(int i = 0; i <= max; i++) partialSets.add(null);
		calculateOverlap();
	}
	
	public int getUnionIndex(int...index) {
		int get = 0;
		for(int i : index) get |= (int)Math.pow(2, i);
		return get;
	}
	
	public Set<K> getUnion(int unionIndex) {
		return partialSets.get(unionIndex);
	}
	
	public int getUnionCount(int unionIndex) {
		return getUnion(unionIndex).size();
	}
	
	public String getUnionCountLabel(int unionIndex) {
		return "" + getUnionCount(unionIndex);
	}
	
	private void setUnion(Set<K> union, int unionIndex) {
		partialSets.set(unionIndex, union);
	}
	
	public int getNrSets() {
		return sets.size();
	}
	
	public Collection<Set<K>> getSets() {
		return sets;
	}
	
	public Set<K> getSet(int setIndex) {
		return sets.get(setIndex);
	}
	
	public int getSetCount(int setIndex) {
		return getSet(setIndex).size();
	}
	
	private void calculateOverlap() {
		int nset = getNrSets();
		sizes = new int[nset];
		
		Set<K> set123 = null;
		if(nset > 2) {
			for(int i = 0; i < nset; i++) {
				//Find size
				sizes[i] = sets.get(i).size();

				//Calculate total overlap
				if(set123 == null) {
					set123 = new HashSet<K>(sets.get(i));
				} else {
					set123.retainAll(sets.get(i));
				}
			}
		} else {
			set123 = new HashSet<K>();
		}
		setUnion(set123, getUnionIndex(setIndices));
		
		//Calculate overlapping parts
		for(int i = 0; i < nset; i++) {
			for(int j = i + 1; j < nset; j++) {
				Set<K> match = new HashSet<K>(sets.get(i));
				match.removeAll(getUnion(getUnionIndex(setIndices)));
				match.retainAll(sets.get(j));
				setUnion(match, getUnionIndex(i, j));
			}
		}
		//Calculate non-overlapping parts
		for(int i = 0; i < nset; i++) {
			Set<K> unique = new HashSet<K>(sets.get(i));
			unique.removeAll(getUnion(getUnionIndex(setIndices)));
			for(int j = 0; j < nset; j++) {
				if(j != i) unique.removeAll(getUnion(getUnionIndex(i, j)));
			}
			setUnion(unique, getUnionIndex(i));
		}
	}
}
