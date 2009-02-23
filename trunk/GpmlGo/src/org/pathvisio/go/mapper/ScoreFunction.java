package org.pathvisio.go.mapper;

public interface ScoreFunction {
	public double calculateScore(int setSize, int termSize, int matches);
}
