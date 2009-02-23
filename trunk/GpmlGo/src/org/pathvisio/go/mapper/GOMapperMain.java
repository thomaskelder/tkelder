package org.pathvisio.go.mapper;

import java.awt.Dimension;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

import javax.swing.JFrame;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pathvisio.data.DBConnDerby;
import org.pathvisio.data.DBConnector;
import org.pathvisio.data.Gdb;
import org.pathvisio.data.SimpleGdbFactory;
import org.pathvisio.debug.Logger;
import org.pathvisio.go.GOAnnotation;
import org.pathvisio.go.GOAnnotationFactory;
import org.pathvisio.go.GOAnnotations;
import org.pathvisio.go.GOReader;
import org.pathvisio.go.GOTree;
import org.pathvisio.go.PathwayAnnotation;
import org.pathvisio.go.XrefAnnotation;
import org.pathvisio.go.gui.GOExplorer;
import org.pathvisio.model.DataSource;
import org.pathvisio.util.FileUtils;

public class GOMapperMain {
	@Option(name = "-action", required = true, usage = "The action you want the script to perform " +
			"(see ACTION_* constants in JavaDoc for details)")
	private String action;

	@Option(name = "-go", required = true, usage = "The GO tree file (obo flat format)")
	private File oboFile;
	
	@Option(name = "-annot", usage = "A GO Annotation file, containing two columns: GO-ID, Ensembl ID")
	private File annotFile;
	
	@Option(name = "-gdb", usage = "The Gene Database for mapping the pathway genes to Ensembl")
	private File gdbFile;
	
	@Option(name = "-mappings", usage = "File to read previously calculated mappings from")
	private File mapFile;
	
	@Option(name = "-out", usage = "The file to which the output of the action will be written")
	private File outFile;
	
	@Option(name = "-pathways", usage = "The directory containing the pathway files")
	private File pathwayDir;
	
	@Option(name = "-pext", usage = "The extension of the pathway files (defaults to 'gpml')")
	private String pathwayExt = "gpml";
	
	@Option(name = "-threshold", usage = "The threshold to use for action 'prune'")
	private double pruneThreshold = 0.6;
	
	public static void main(String[] args) {
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
			GOTree goTree = GOReader.readGOTree(main.oboFile);
			GOMapper goMapper = new GOMapper(goTree);

			GOAnnotations geneAnnot = null;
			if(main.annotFile != null) {
				geneAnnot = GOAnnotations.read(main.annotFile, goTree, new GOAnnotationFactory() {
					public GOAnnotation createAnnotation(String id, String evidence) {
						return new XrefAnnotation(id, DataSource.ENSEMBL, evidence);
					}
				});
			}
			//Calculated the pathway mappings if needed
			if(ACTION_MAP_TREE.equals(main.action) || main.mapFile == null) {
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
				Gdb gdb = SimpleGdbFactory.createInstance(
						main.gdbFile.getAbsolutePath(), new DBConnDerby(), DBConnector.PROP_NONE
				);
				Logger.log.info("Mapping pathways to GO tree");
				goMapper.calculate(gpmlFiles, gdb, geneAnnot);
			} else if(main.mapFile != null) {
				//Read existing mappings
				goMapper.setPathwayAnnotations(GOAnnotations.read(main.mapFile, goTree, new GOAnnotationFactory() {
					public GOAnnotation createAnnotation(String id,
							String evidence) {
						return new PathwayAnnotation(id, Double.parseDouble(evidence));
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
				GOAnnotations pruned = goMapper.prune(main.pruneThreshold);
				Logger.log.info("Writing pruned mappings to " + main.outFile);
				pruned.write(main.outFile);
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
}
