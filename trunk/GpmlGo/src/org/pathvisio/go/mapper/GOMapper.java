package org.pathvisio.go.mapper;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.bio.Organism;
import org.bridgedb.rdb.IDMapperRdb;
import org.pathvisio.debug.Logger;
import org.pathvisio.go.GOAnnotation;
import org.pathvisio.go.GOAnnotations;
import org.pathvisio.go.GOTerm;
import org.pathvisio.go.GOTree;
import org.pathvisio.go.PathwayAnnotation;
import org.pathvisio.go.XrefAnnotation;
import org.pathvisio.model.ConverterException;
import org.pathvisio.model.Pathway;

public class GOMapper {
	GOTree goTree;
	GOAnnotations pathwayAnnotations;
	ScoreFunction scoreFunction = new SetPercentageFunction();
	boolean useFileNames = true;
	
	public GOMapper(GOTree goTree) {
		this.goTree = goTree;
	}
	
	/**
	 * Set the pathway mappings directly (e.g. from previous calculations)
	 */
	public void setPathwayAnnotations(GOAnnotations a) {
		pathwayAnnotations = a;
	}
	
	public GOAnnotations getPathwayAnnotations() {
		return pathwayAnnotations;
	}
	
	/**
	 * Calculates the pathway mappings.
	 * @param gpmlFiles
	 * @param gdb
	 * @param geneAnnotations
	 * @throws DataException
	 * @throws ConverterException
	 */
	public void calculate(List<File> gpmlFiles, IDMapperRdb gdb, GOAnnotations geneAnnotations, Organism org) throws IDMapperException, ConverterException {
		pathwayAnnotations = new GOAnnotations();
		
		int i = 0;
		for(File f : gpmlFiles) {
			Logger.log.info("Processing file " + ++i + " out of " + gpmlFiles.size());
			Pathway p = new Pathway();
			p.readFromXml(f, true);
			String name = p.getMappInfo().getMapInfoName();
			String id = null;
			if(useFileNames) {
				id = f.getAbsolutePath();
			} else {
				id = name + "(" + f.getName() + ")";
			}
			mapPathway(id, p, gdb, geneAnnotations, org);
		}
	}
	
	private void mapPathway(String id, Pathway p, IDMapperRdb gdb, GOAnnotations geneAnnotations, Organism org) throws IDMapperException {
		Set<Xref> pathwayXrefs = new HashSet<Xref>();
		for(Xref x : p.getDataNodeXrefs()) {
			pathwayXrefs.addAll(gdb.getCrossRefs(x, DataSource.getBySystemCode("En" + org.code())));
		}
		
		Logger.log.info(pathwayXrefs.size() + "");
		
		//Compare xref vector of GO with xref vector of pathway
		for(GOTerm term : goTree.getTerms()) {
			//Skip deprecated terms
			if(term.isObsolete()) continue;
			
			Set<XrefAnnotation> termXrefs = new HashSet<XrefAnnotation>();
			//Assumes ensembl ids
			for(GOAnnotation goa : goTree.getRecursiveAnnotations(term, geneAnnotations)) {
				if(goa instanceof XrefAnnotation) termXrefs.add((XrefAnnotation)goa);
			}
			
			int matches = 0;
			//Simply count matches, later weigh by size of GO
			for(Xref x : pathwayXrefs) {
				if(termXrefs.contains(x)) matches++;
			}
			double score = calculateScore(matches, pathwayXrefs.size(), termXrefs.size());
			if(score > 0) {
				pathwayAnnotations.addAnnotation(term, new PathwayAnnotation(id, score));
			}
		}
	}
	
	/**
	 * Filter the annotations by removing all mappings
	 * for which the score is below the threshold and all
	 * parent mappings for which the child is already above
	 * the threshold.
	 * @return The pruned annotations
	 */
	public GOAnnotations prune(double threshold) {
		GOAnnotations pruned = new GOAnnotations();
		
		for(GOTerm term : goTree.getTerms()) {
			for(GOAnnotation goa : pathwayAnnotations.getAnnotations(term)) {
				PathwayAnnotation pa = (PathwayAnnotation)goa;
				if(pa.getScore() >= threshold) {
					boolean best = true;
					//Check if children all have a lower score
					for(GOTerm child : goTree.getChildren(term.getId())) {
						PathwayAnnotation cpa = 
							(PathwayAnnotation)pathwayAnnotations.getAnnotation(child, pa.getId());
						if(cpa != null && cpa.getScore() >= threshold) {
							best = false;
							break;
						}
					}
					if(best) {
						//Add the annotation
						pruned.addAnnotation(term, new PathwayAnnotation(pa.getId(), pa.getScore()));
					}
				}
			}
		}
		
		return pruned;
	}
	
	public void setUseFileNames(boolean useFileNames) {
		this.useFileNames = useFileNames;
	}
	
	public ScoreFunction getScoreFunction() {
		return scoreFunction;
	}
	
	public void setScoreFunction(ScoreFunction f) {
		scoreFunction = f;
	}
	
	private double calculateScore(int matches, int pathwaySize, int termSize) {
		return scoreFunction.calculateScore(pathwaySize, termSize, matches);
	}
}