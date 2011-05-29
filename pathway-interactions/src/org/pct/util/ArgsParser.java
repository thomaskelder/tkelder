package org.pct.util;

import java.io.File;
import java.util.List;

import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;
import uk.co.flamingpenguin.jewel.cli.CliFactory;
import uk.co.flamingpenguin.jewel.cli.Option;

/**
 * Commonly used command line arguments to be parsed by args4j.
 * @author thomas
 */
public class ArgsParser {
	public static <A> A parse(String[] args, Class<? extends A> c) throws ArgumentValidationException {
		return CliFactory.parseArguments(c, args);
	}

	public interface AHelp {
		@Option(helpRequest = true, shortName = "h")
		boolean getHelp();
	}
	
	public interface APathways {
		@Option(shortName = "p", description = "The path(s) to the GPML files (will look for *.gpml recursively).")
		public List<File> getPathway();

		@Option(defaultValue = "5", description = "The minimum number of xrefs a pathway should contain to be included")
		public int getMinPwSize();
		
		@Option(defaultValue = "2147483647", description = "The maximum number of xrefs a pathway should contain to be included")
		public int getMaxPwSize();

		@Option(defaultValue = "-1", description = "Set this parameter to a value between 0 and 1 to merge pathways of which the overlap exceeds this threshold.")
		public double getMergeThreshold();

		@Option(description = "Should the Kegg meta pathways be filtered out?")
		public boolean isExclKeggMeta();
		
		@Option(description = "Should the Kegg disease pathways be filtered out?")
		public boolean isExclKeggDisease();
	}

	public interface ANetwork {
		@Option(shortName = "n", description = "The xgmml file(s) of the network(s) to use. " +
			"If this is a directory, the script will recursively look for all .xgmml files in the directory.")
		public List<File> getNetworks();
	}
	
	public interface ACrossTalk {
		@Option(defaultValue = "true", description = "Don't perform Fisher's exact test to calculate p-values.")
		public boolean isFisher();

		@Option(description = "Add this parameter to omit interactions between xrefs that are in both pathways.")
		public boolean isOmitOverlap();

		@Option(defaultValue = "2", description = "The number of threads to use.")
		public int getNt();

		@Option(defaultValue = "1000", description = "The number of permutations to perform for pvalue calculation")
		public int getNperm();
	}

	public interface AIDMapper {
		@Option(description = "Bridgedb connection strings to idmappers to use to translate between xrefs of different types.")
		public List<String> getIdm();
		public boolean isIdm();
		
		@Option(defaultValue = { "L", "Ce" }, description = "The datasource(s) to translate all xrefs to (use system code).")
		public List<String> getDs();
	}

	public interface AWeights {
		@Option(shortName = "w", description = "A tsv file containing the weight values.")
		public File getWeights();
		public boolean isWeights();
		
		@Option(description= "Add this parameter if the weights file contains a header.")
		public boolean isWeightsHeader();

		@Option(defaultValue = "2", description = "The column in the weights file that contains the weights, counting from 0 (defaults to 2)")
		public int getWeightsCol();

		@Option(defaultValue = "0", description= "The column in the weights file that contains the xref identifiers, counting from 0 (defaults to 0)")
		public int getIdCol();

		@Option(defaultValue = "1", description = "The column in the weights file that contains the xref system codes, counting from 0 (defaults to 1). " +
		"If a fixed datasource has been specified with weightsDs, this parameter has no effect.")
		public int getSysCol();

		@Option(defaultValue = "", description = "The datasource of all identifiers in the weights file.")
		public String getWeightsSys();
		public boolean isWeighhtsSys();
	}
}
