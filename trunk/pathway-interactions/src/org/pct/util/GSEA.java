package org.pct.util;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bridgedb.Xref;
import org.pct.util.ArgsData.DIDMapper;
import org.pct.util.ArgsData.DPathways;
import org.pct.util.ArgsData.DWeights;
import org.pct.util.ArgsParser.AHelp;
import org.pct.util.ArgsParser.AIDMapper;
import org.pct.util.ArgsParser.APathways;
import org.pct.util.ArgsParser.AWeights;

import uk.co.flamingpenguin.jewel.cli.Option;
import edu.mit.broad.genome.alg.gsea.DefaultGeneSetCohort;
import edu.mit.broad.genome.alg.gsea.GeneSetScoringTable;
import edu.mit.broad.genome.alg.gsea.GeneSetScoringTables;
import edu.mit.broad.genome.alg.gsea.KSTests;
import edu.mit.broad.genome.alg.gsea.PValueCalculator;
import edu.mit.broad.genome.alg.gsea.PValueCalculatorImpls;
import edu.mit.broad.genome.math.RandomSeedGenerators;
import edu.mit.broad.genome.objects.DefaultRankedList;
import edu.mit.broad.genome.objects.FSet;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;

/**
 * Perform GSEA analysis
 * @author thomas
 */
public class GSEA {
	private final static Logger log = Logger.getLogger(GSEA.class.getName());
	
	public static Collection<GeneSet> createGeneSets(Map<String, Set<Xref>> pathways, Map<Xref, Double> scores) {
		List<GeneSet> sets = new ArrayList<GeneSet>();
		for(String name : pathways.keySet()) {
			Set<Xref> xrefs = pathways.get(name);
			Set<Xref> withData = new HashSet<Xref>(xrefs);
			for(Xref x : xrefs) {
				if(!scores.containsKey(x)) withData.remove(x);
			}
			if(withData.size() == 0) continue; //No measured genes in set
			
			String[] ids = new String[withData.size()];
			int i = 0;
			for(Xref x : withData) ids[i++] = x.getId();

			GeneSet gs = new FSet(name, name, ids);
			sets.add(gs);
		}
		return sets;
	}
	
	public static EnrichmentDb runGSEA(Map<String, Set<Xref>> pathways, Map<Xref, Double> scores, int nperm) throws Exception {
		//Set score for xrefs that are in pathway but not in scores to 0
		Map<Xref, Double> fullScores = new HashMap<Xref, Double>(scores);
		Set<Xref> noScoreXrefs = new HashSet<Xref>();
		for(Set<Xref> x : pathways.values()) noScoreXrefs.addAll(x);
		noScoreXrefs.removeAll(scores.keySet());
		for(Xref x : noScoreXrefs) {
			fullScores.put(x, 0.0);
		}
		
		String[] rankedNames = new String[fullScores.size()];
		float[] rankedScores = new float[fullScores.size()];
		
		List<Entry<Xref, Double>> scoreList = new LinkedList<Entry<Xref, Double>>(fullScores.entrySet());
		Collections.sort(scoreList, new Comparator<Entry<Xref, Double>>() {
			public int compare(Entry<Xref, Double> o1, Entry<Xref, Double> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
		
		String rlog = "Ranked scores: ";
		for(int i = 0; i < scoreList.size(); i++) {
			rankedNames[i] = scoreList.get(i).getKey().getId();
			rankedScores[i] = scoreList.get(i).getValue().floatValue();
			rlog += rankedScores[i] + " (" + rankedNames[i] + "), ";
		}
		log.info(rlog + "...");
		
		RankedList ranks = new DefaultRankedList("ranked score", rankedNames, rankedScores);
		
		KSTests test = new KSTests();
		GeneSetScoringTable table = new GeneSetScoringTables.Weighted();
		GeneSet[] gs = createGeneSets(pathways, scores).toArray(new GeneSet[0]);
		EnrichmentDb edb = test.executeGsea(
				ranks, gs, nperm, 
				new RandomSeedGenerators.Timestamp(), null, 
				new DefaultGeneSetCohort.Generator(table, false)
		);
		
		PValueCalculator pvc = new PValueCalculatorImpls.GseaImpl("meandiv");
        EnrichmentResult[] results = pvc.calcNPValuesAndFDR(edb.getResults());
        return edb.cloneDeep(results);
	}
	
	public static void reportResults(PrintWriter out, EnrichmentDb edb) {
		EnrichmentResult[] results = edb.getResults();
		Arrays.sort(results, new Comparator<EnrichmentResult>() {
			public int compare(EnrichmentResult o1, EnrichmentResult o2) {
				double d = o2.getScore().getFDR() - o1.getScore().getFDR();
				if(d > 0) return -1;
				if(d < 0) return 1;
				return 0;
			}
		});
		out.println("Pathway\tES\tNES\tNP\tFDR\tFWER\tSize");
		for (EnrichmentResult r : results) {
            out.println(r.getGeneSetName() + "\t" +
                    r.getScore().getES() + "\t" +
                    r.getScore().getNES() + "\t" +
                    r.getScore().getNP() + "\t" +
                    r.getScore().getFDR() + "\t" +
                    r.getScore().getFWER() + "\t" + 
                    r.getGeneSet().getNumMembers()
            );
        }
	}
	
	public static void main(String[] args) {
		try {
			Args pargs = ArgsParser.parse(args, Args.class);
			DIDMapper didm = new DIDMapper(pargs);
			DPathways dpws = new DPathways(pargs, didm);
			DWeights dw = new DWeights(pargs, didm);
			
			Map<Xref, Double> values = pargs.isAbs() ? XrefData.absoluteWeights(dw.getWeightValues()) : dw.getWeightValues();
			EnrichmentDb result = runGSEA(dpws.getPathways(), values, pargs.getNperm());
			PrintWriter out = new PrintWriter(pargs.getOut());
			reportResults(out, result);
			out.close();
		} catch(Exception e) {
			log.log(Level.SEVERE, "Fatal error", e);
		}
	}
	
	private interface Args extends APathways, AWeights, AIDMapper, AHelp {
		@Option(shortName = "o", description = "The output file to write the results to.")
		File getOut();
		@Option(defaultValue = "1000", description = "The number of permutations to perform for pvalue calculation")
		int getNperm();
		@Option(description = "Ignore the sign of the values, take the absolute.")
		boolean isAbs();
	}
}
