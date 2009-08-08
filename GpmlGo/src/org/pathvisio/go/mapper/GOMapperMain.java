package org.pathvisio.go.mapper;

import java.awt.Dimension;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.JFrame;

import org.bridgedb.DataSource;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.bio.Organism;
import org.bridgedb.rdb.DBConnector;
import org.bridgedb.rdb.IDMapperRdb;
import org.bridgedb.rdb.SimpleGdbFactory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pathvisio.data.DBConnDerby;
import org.pathvisio.debug.Logger;
import org.pathvisio.go.GOAnnotationFactory;
import org.pathvisio.go.GOAnnotations;
import org.pathvisio.go.GOReader;
import org.pathvisio.go.GOTree;
import org.pathvisio.go.PathwayAnnotation;
import org.pathvisio.go.XrefAnnotation;
import org.pathvisio.go.gui.GOExplorer;
import org.pathvisio.util.FileUtils;

public class GOMapperMain {
	@Option(name = "-action", required = true, usage = "The action you want the script to perform, " +
			"map, prune or gui (see ACTION_* constants in JavaDoc for details)")
	private String action;

	@Option(name = "-go", required = true, usage = "The GO tree file (obo flat format)")
	private File oboFile;
	
	@Option(name = "-annot", usage = "A GO Annotation file, containing two columns: GO-ID, Ensembl ID")
	private File annotFile;
	
	@Option(name = "-org", required = true, usage = "The organism you are mapping")
	private String org;
	
	@Option(name = "-gdb", usage = "The Gene Database for mapping the pathway genes to Ensembl")
	private File gdbFile;
	
	@Option(name = "-mappings", usage = "File to read previously calculated mappings from")
	private File mapFile;
	
	@Option(name = "-code", usage = "System code to use for exporting genes")
	private String dbCode = "L";
		
	@Option(name = "-out", usage = "The file to which the output of the action will be written")
	private File outFile;
	
	@Option(name = "-pathways", usage = "The directory containing the pathway files")
	private File pathwayDir;
	
	@Option(name = "-pext", usage = "The extension of the pathway files (defaults to 'gpml')")
	private String pathwayExt = "gpml";
	
	@Option(name = "-threshold", usage = "The threshold to use for action 'prune'")
	private double pruneThreshold = 0.6;
	
	@Option(name = "-useFileName", usage = "Use file names for pathways in the output files." +
			" If not set, human readable names will be used instead (making the output files unusable " +
			"for the exporting to xrefs action.")
	private boolean useFileName = false;
	
	public static void main(String[] args) {
		BioDataSource.init();
		GOMapperMain main = new GOMapperMain();
		CmdLineParser parser = new CmdLineParser(main);
		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			e.printStackTrace();
			parser.printUsage(System.err);
			System.exit(-1);
		}
		
		try {
			IDMapperRdb gdb = null;
			if(main.gdbFile != null) {
				gdb = SimpleGdbFactory.createInstance(
					main.gdbFile.getAbsolutePath(), new DBConnDerby(), DBConnector.PROP_NONE
				);
			}
			GOTree goTree = GOReader.readGOTree(main.oboFile);
			GOMapper goMapper = new GOMapper(goTree);

			goMapper.setUseFileNames(main.useFileName);
			
			Organism org = Organism.fromLatinName(main.org);
			if(org == null) {
				org = Organism.fromCode(main.org);
			}
			if(org == null) {
				org = Organism.fromShortName(main.org);
			}
			
			GOAnnotations<XrefAnnotation> geneAnnot = null;
			if(main.annotFile != null) {
				final DataSource ds = org == null ? BioDataSource.ENSEMBL : DataSource.getBySystemCode("En" + org.code());
				geneAnnot = GOAnnotations.read(main.annotFile, goTree, new GOAnnotationFactory<XrefAnnotation>() {
					public Collection<XrefAnnotation> createAnnotations(String id, String evidence) {
						return Arrays.asList(new XrefAnnotation[] { new XrefAnnotation(id, ds, evidence) });
					}
				});
			}
			//Calculated the pathway mappings if needed
			if(ACTION_MAP_TREE.equals(main.action) || main.mapFile == null) {
				if(org == null) {
					fatalParamError(
							"Invalid organism " + main.org + "; please specify correct -org parameter.");
				}
				
				if(geneAnnot == null) {
					fatalParamError(
						"Could not read gene annotations from file " + main.annotFile + "\n" +
						"Please specify the -annot or -mapping parameter.");
				}
				if(main.pathwayDir == null) {
					fatalParamError(
						"Please specify the -pathways parameter.");
				}
				if(main.gdbFile == null) {
					fatalParamError(
						"Please specify the -gdb parameter.");
				}
				List<File> gpmlFiles = FileUtils.getFiles(main.pathwayDir, main.pathwayExt, true);
				Logger.log.info("Mapping pathways to GO tree");
				goMapper.calculate(gpmlFiles, gdb, geneAnnot, org);
			} else if(main.mapFile != null) {
				//Read existing mappings
				goMapper.setPathwayAnnotations(GOAnnotations.read(main.mapFile, goTree, new GOAnnotationFactory<PathwayAnnotation>() {
					public Collection<PathwayAnnotation> createAnnotations(String id,
							String evidence) {
						return Arrays.asList(new PathwayAnnotation[] { new PathwayAnnotation(id, Double.parseDouble(evidence)) });
					}
				}));
			}
			
			//Write the pathway mappings if needed
			if(ACTION_MAP_TREE.equals(main.action) && main.outFile != null) {
				Logger.log.info("Writing mappings to " + main.outFile);
				goMapper.getPathwayAnnotations().write(main.outFile);
			}
			
			if(ACTION_PRUNE.equals(main.action)) {
				Logger.log.info("Pruning mappings with threshold " + main.pruneThreshold);
				GOAnnotations<PathwayAnnotation> pruned = goMapper.prune(main.pruneThreshold);
				Logger.log.info("Writing pruned mappings to " + main.outFile);
				pruned.write(main.outFile);
			}
			
			if(ACTION_XREFS.equals(main.action)) {
				DataSource ds = DataSource.getBySystemCode(main.dbCode);
				goMapper.getPathwayAnnotations().writeXrefs(main.outFile, gdb, ds);
			}
			
			if(ACTION_MATRIX.equals(main.action)) {
				ScoreMatrix<String> m = goMapper.getPathwayAnnotations().createEvidenceMatrix("0.0");
				m.write(new BufferedWriter(new FileWriter(main.outFile)));
			}
			
			if(ACTION_GUI.equals(main.action)) {
				GOExplorer gui = new GOExplorer();
				gui.setGO(goTree, goMapper.getPathwayAnnotations());
				gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				gui.setVisible(true);
				gui.setSize(new Dimension(800, 600));
			}
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(-2);
		}
	}

	private static void fatalParamError(String msg) {
		System.err.println(msg);
		System.exit(-1);
	}
	
	/**
	 * Write a scoring matrix containing the mapping scores.
	 */
	private static final String ACTION_MATRIX = "matrix";
	
	/**
	 * Map a set of pathways to the go tree. Generates a similarity score for each GO term and pathway.
	 * Writes mappings to a file, see {@link GOAnnotations#write(File)}.
	 */
	private static final String ACTION_MAP_TREE = "map";
	
	private static final String ACTION_PRUNE = "prune";
	
	private static final String ACTION_GUI= "gui";
	
	/**
	 * Exports mappings to cross-references from a pathway instead of
	 * the pathway names.
	 */
	private static final String ACTION_XREFS = "xrefs";
}
