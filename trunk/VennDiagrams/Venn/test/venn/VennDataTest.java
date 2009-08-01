package venn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

public class VennDataTest extends TestCase {
	public void testUnionCounts() {
		//Test if the union counts are correct
		Set<Integer> set1 = new HashSet<Integer>();
		set1.add(1);
		set1.add(2);
		set1.add(3);
		set1.add(4);
		set1.add(5);
		Set<Integer> set2 = new HashSet<Integer>();
		set2.add(3);
		set2.add(4);
		set2.add(5);
		set2.add(6);
		Set<Integer> set3 = new HashSet<Integer>();
		set3.add(5);
		set3.add(6);
		set3.add(7);
		set3.add(8);
		List<Set<Integer>> sets = new ArrayList<Set<Integer>>();
		sets.add(set1); sets.add(set2); sets.add(set3);
		VennData<Integer> vd123 = new VennData<Integer>(sets);
		assertSize(vd123.getUnion(vd123.getUnionIndex(0)).size(), 2);
		assertSize(vd123.getUnion(vd123.getUnionIndex(1)).size(), 0);
		assertSize(vd123.getUnion(vd123.getUnionIndex(2)).size(), 2);
		assertSize(vd123.getUnion(vd123.getUnionIndex(0,1,2)).size(), 1);
		assertSize(vd123.getUnion(vd123.getUnionIndex(0,1)).size(), 2);
		assertSize(vd123.getUnion(vd123.getUnionIndex(1,2)).size(), 1);
		assertSize(vd123.getUnion(vd123.getUnionIndex(0,2)).size(), 0);
	}
	
	void assertSize(int size, int should) {
		assertTrue("Wrong size " + size + ", should be " + should, size == should);
	}
}
