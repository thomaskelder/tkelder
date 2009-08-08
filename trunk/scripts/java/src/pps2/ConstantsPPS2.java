package pps2;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.DataDerby;
import org.bridgedb.rdb.IDMapperRdb;
import org.bridgedb.rdb.SimpleGdbFactory;
import org.pathvisio.debug.Logger;
import org.pathvisio.go.GOAnnotationFactory;
import org.pathvisio.go.GOAnnotations;
import org.pathvisio.go.GOTerm;
import org.pathvisio.go.GOTree;
import org.pathvisio.go.XrefAnnotation;
import org.pathvisio.util.Utils;

public class ConstantsPPS2 {
	private static final File idMapperFile = new File("/home/thomas/PathVisio-Data/gene_databases/Mm_Derby_20090509.pgdb");
	private static IDMapperRdb idMapper;
	private static DataSource ds = BioDataSource.ENTREZ_GENE;
	private static Set<DataSource> dsSet = Utils.setOf(ds);
	
	static IDMapperRdb getIdMapper() throws IDMapperException {
		if(idMapper == null) idMapper = SimpleGdbFactory.createInstance("" + idMapperFile, new DataDerby(), 0);
		return idMapper;
	}
	
	static final File pathwayDir = new File("/home/thomas/data/pathways/20090715");
	static final File goOboFile = new File("/home/thomas/projects/pps2/path_results/bigcat/go-venn/data/gene_ontology_edit.obo");
	static final File goAnnotFile = new File("/home/thomas/projects/pps2/path_results/bigcat/go-venn/data/mart_mm_proc.txt");
	
	static final String[] termNames = new String[] {
			"GO:0008152",
			"GO:0006979",
			"GO:0006954"
	};
	
	static GOTerm[] getGOTerms(GOTree tree) {
		GOTerm[] terms = new GOTerm[termNames.length];
		for(int i = 0; i < termNames.length; i++) terms[i] = tree.getTerm(termNames[i]);
		return terms;
	}
	
	static String[] getGOTermLabels(GOTerm[] terms) {
		String[] lbs = new String[terms.length];
		for(int i = 0; i < lbs.length; i++) lbs[i] = terms[i].getName();
		return lbs;
	}
	
	static GOAnnotations<XrefAnnotation> getGOAnnotations(GOTree tree) throws IOException {
		return GOAnnotations.read(
				goAnnotFile, tree, new GOAnnotationFactory<XrefAnnotation>() {
			public Collection<XrefAnnotation> createAnnotations(String id, String evidence) {
				Set<XrefAnnotation> annot = new HashSet<XrefAnnotation>();
				try {
					for(Xref x : idMapper.mapID(new Xref(id, BioDataSource.ENSEMBL_MOUSE), dsSet)) {
						annot.add(new XrefAnnotation(x.getId(), x.getDataSource(), evidence));
					}
				} catch(IDMapperException e) {
					Logger.log.error("Unable to map ids for GO annotations", e);
				}
				return annot;
			}
		});
	}
}
