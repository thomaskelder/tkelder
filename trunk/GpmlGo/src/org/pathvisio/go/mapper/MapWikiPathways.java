package org.pathvisio.go.mapper;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperStack;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.bio.Organism;
import org.bridgedb.rdb.GdbProvider;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pathvisio.go.GOAnnotationFactory;
import org.pathvisio.go.GOAnnotations;
import org.pathvisio.go.GOReader;
import org.pathvisio.go.GOTree;
import org.pathvisio.go.PathwayAnnotation;
import org.pathvisio.go.XrefAnnotation;
import org.pathvisio.wikipathways.WikiPathwaysCache;
import org.pathvisio.wikipathways.WikiPathwaysClient;

public class MapWikiPathways {
	@Option(name = "-go", required = true, usage = "The GO tree file (obo flat format)")
	private File oboFile;

	@Option(name = "-idmConfig", usage = "The BridgeDb config file")
	private File idmConfig;

	@Option(name = "-out", usage = "The path to which the output files will be written")
	private File outFile;

	@Option(name = "-cache", usage = "The path containing the cached pathway files")
	private File cacheFile;
	
	@Option(name = "-threshold", usage = "The threshold to use for assigning the annotation.")
	private double pruneThreshold = 0.6;

	@Option(name = "-wikipathways.url", usage = "The url to the wikipathways webservice.")
	private String wpUrl = "http://www.wikipathways.org/wpi/webservice/webservice.php";
	
	public static void main(String[] args) {
		BioDataSource.init();
		MapWikiPathways main = new MapWikiPathways();
		CmdLineParser parser = new CmdLineParser(main);
		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			e.printStackTrace();
			parser.printUsage(System.err);
			System.exit(-1);
		}
		
		try {
			GdbProvider idmProv = GdbProvider.fromConfigFile(main.idmConfig);
			
			WikiPathwaysClient client = new WikiPathwaysClient(new URL(main.wpUrl));
			WikiPathwaysCache cache = new WikiPathwaysCache(client, main.cacheFile);
			cache.update();
			
			for(Organism org : idmProv.getOrganisms()) {
				IDMapperStack idm = new IDMapperStack();
				for(IDMapper i : idmProv.getGdbs(org)) {
					idm.addIDMapper(i);
				}
				
				GOTree goTree = GOReader.readGOTree(main.oboFile);
				GOMapper goMapper = new GOMapper(goTree);
				goMapper.setScoreFunction(new IncludesPercentageFunction());
				goMapper.setUseFileNames(true);
				
				DataSource ensDs = BioDataSource.getSpeciesSpecificEnsembl(org);
				final DataSource tgtds = ensDs == null ? BioDataSource.ENTREZ_GENE : ensDs;
				System.err.println(tgtds);
				GOAnnotations<XrefAnnotation> geneAnnot = GOAnnotations.fromIDMapper(idm, tgtds, goTree, new GOAnnotationFactory<XrefAnnotation>() {
					public Collection<XrefAnnotation> createAnnotations(String id, String evidence) {
						return Arrays.asList(new XrefAnnotation[] { new XrefAnnotation(id, tgtds, evidence) });
					}
				});
				
				List<File> gpmlFiles = new ArrayList<File>();
				for(File f : cache.getFiles()) {
					if(org.latinName().equals(cache.getPathwayInfo(f).getSpecies())) {
						gpmlFiles.add(f);
					}
				}
				goMapper.calculate(gpmlFiles, idm, geneAnnot, tgtds);
				
				GOAnnotations<PathwayAnnotation> pruned = goMapper.prune(main.pruneThreshold);
				
				pruned.write(new File(main.outFile, 
						"go-mappings-" + main.pruneThreshold + "-" + org.shortName()));
			}
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(-2);
		}
	}
}
