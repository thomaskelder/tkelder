package org.pathvisio.go.mapper;

public class IncludesPercentageFunction implements ScoreFunction {
	public double calculateScore(int setSize, int termSize, int matches) {
		if(setSize > 0 && matches > termSize) return 2; //If the pathway includes the whole GO term, set score to 2
		return setSize == 0 ? 0 : (double)matches / (double)setSize; //Otherwise, use the percentage of pathway that matches term
	}
}
