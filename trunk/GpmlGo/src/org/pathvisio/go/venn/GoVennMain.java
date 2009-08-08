package org.pathvisio.go.venn;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;

import org.bridgedb.DataSource;
import org.bridgedb.bio.BioDataSource;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pathvisio.go.GOAnnotationFactory;
import org.pathvisio.go.GOAnnotations;
import org.pathvisio.go.GOReader;
import org.pathvisio.go.GOTerm;
import org.pathvisio.go.GOTree;
import org.pathvisio.go.XrefAnnotation;

public class GoVennMain {
	@Option(name = "-go", required = true, usage = "The GO tree file (obo flat format)")
	private File oboFile;
	
	@Option(name = "-annot", usage = "A GO Annotation file, containing two columns: GO-ID, Ensembl ID")
	private File annotFile;
	
	@Option(name = "-out", usage = "The file to which the output of the action will be written")
	private File outFile;
	
	public static void main(String[] args) {
		BioDataSource.init();
		
		GoVennMain main = new GoVennMain();
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
			final DataSource ds = BioDataSource.ENSEMBL_MOUSE;
			GOAnnotations<XrefAnnotation> geneAnnot = GOAnnotations.read(
					main.annotFile, goTree, new GOAnnotationFactory<XrefAnnotation>() {
				public Collection<XrefAnnotation> createAnnotations(String id, String evidence) {
					return Arrays.asList(new XrefAnnotation[] { new XrefAnnotation(id, ds, evidence) });
				}
			});
			
			GOTerm[] terms = new GOTerm[] {
					goTree.getTerm("GO:0006979"),
					goTree.getTerm("GO:0008152"),
					goTree.getTerm("GO:0006954"),
					goTree.getTerm("GO:0006006")
			};
			Writer out = new BufferedWriter(new FileWriter(main.outFile));
			GoVenn.writeVennMappings(goTree, geneAnnot, terms, out);
			out.close();
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(-2);
		}
	}
}
