package venn;

public interface VennCounts {
	public int getNrSets();
	public int getUnionCount(int unionIndex);
	public int getUnionIndex(int...i);
	public String getUnionCountLabel(int unionIndex);
}
