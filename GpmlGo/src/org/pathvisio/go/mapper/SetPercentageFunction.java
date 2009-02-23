package org.pathvisio.go.mapper;

public class SetPercentageFunction implements ScoreFunction {
	public double calculateScore(int setSize, int termSize, int matches) {
		return setSize == 0 ? 0 : (double)matches / (double)setSize;
	}
}
