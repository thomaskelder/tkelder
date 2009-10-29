package various;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bridgedb.IDMapperException;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.bio.Organism;
import org.bridgedb.rdb.DataDerby;
import org.bridgedb.rdb.IDMapperRdb;
import org.bridgedb.rdb.SimpleGdbFactory;
import org.pathvisio.go.GOAnnotationFactory;
import org.pathvisio.go.GOAnnotations;
import org.pathvisio.go.GOReader;
import org.pathvisio.go.GOTerm;
import org.pathvisio.go.GOTree;
import org.pathvisio.go.PathwayAnnotation;
import org.pathvisio.go.XrefAnnotation;
import org.pathvisio.go.mapper.GOMapper;
import org.pathvisio.model.Pathway;
import org.pathvisio.wikipathways.WikiPathwaysCache;
import org.pathvisio.wikipathways.WikiPathwaysClient;
import org.pathvisio.wikipathways.webservice.WSPathwayInfo;

public class ListPathwaysForClassification {
	static final File goOboFile = new File("/home/thomas/projects/pps2/path_results/bigcat/go-venn/data/gene_ontology_edit.obo");
	static final File goAnnotFile = new File("/home/thomas/projects/pps2/path_results/bigcat/go-venn/data/mart_mm_proc.txt");

	public static void main(String[] args) {
		File keggDir = new File("/home/thomas/data/pathways/20090420-kegg");

		try {
			//WikiPathways pathways
			WikiPathwaysClient wpclient = new WikiPathwaysClient(new URL("http://www.wikipathways.org/wpi/webservice/webservice.php"));
			WikiPathwaysCache wpcache = new WikiPathwaysCache(
					wpclient,
					new File("/home/thomas/data/wikipathways/cache/")
			);
			//wpcache.update();

			List<File> allFiles = new ArrayList<File>();
			for(File f : wpcache.getFiles()) {
				WSPathwayInfo info = wpcache.getPathwayInfo(f);
				if("Mus musculus".equals(info.getSpecies())) {
					allFiles.add(f);
				}
			}
			allFiles.addAll(Arrays.asList(keggDir.listFiles()));

			//Load GO classifications for all pathways
			GOTree tree = GOReader.readGOTree(goOboFile);
			GOAnnotations<XrefAnnotation> geneAnnot = getGOAnnotations(tree);
			
			File annotFile = new File("/home/thomas/Desktop/annot.txt");
			GOAnnotations<PathwayAnnotation> pathwayAnnot = null;
			GOMapper goMapper = new GOMapper(tree);
			goMapper.setUseFileNames(true);
			
			if(annotFile.exists()) {
				goMapper.setPathwayAnnotations(GOAnnotations.read(annotFile, tree, new GOAnnotationFactory<PathwayAnnotation>() {
					public Collection<PathwayAnnotation> createAnnotations(String id,
							String evidence) {
						return Arrays.asList(new PathwayAnnotation[] { new PathwayAnnotation(id, Double.parseDouble(evidence)) });
					}
				}));
			} else {
				goMapper.calculate(allFiles, getIdMapper(), geneAnnot, Organism.MusMusculus);
				goMapper.getPathwayAnnotations().write(annotFile);
			}
			pathwayAnnot = goMapper.prune(0.9);
			
			Set<PathwayAnnotation> metAnnot = tree.getRecursiveAnnotations(
					tree.getTerm("GO:0008152"), pathwayAnnot);
			Set<String> inMetabolism = new HashSet<String>();
			for(PathwayAnnotation p : metAnnot) inMetabolism.add(p.getId()); //File path

			//Write pathways
			BufferedWriter out = new BufferedWriter(new FileWriter("/home/thomas/Desktop/pathways.txt"));

			for(File f : allFiles) {
				String title, url = "";
				if(!wpcache.getFiles().contains(f)) {
					Pathway p = new Pathway();
					p.readFromXml(f, true);
					title = p.getMappInfo().getMapInfoName();
					url = p.getMappInfo().getMapInfoDataSource();
				} else {
					WSPathwayInfo info = wpcache.getPathwayInfo(f);
					title = info.getName();
					url = info.getUrl();
				}
				out.append(title);
				out.append("\t");
				out.append(url);
				out.append("\t");
				out.append(f.getName());
				out.append("\t");
				out.append(inMetabolism.contains(f.getAbsolutePath()) ? "Metabolism" : "");
				out.append("\t");
				out.append(getOtherTerms(f, pathwayAnnot));
				out.append("\n");
			}
			out.close();
	} catch(Exception e) {
		e.printStackTrace();
	}
}

static String getOtherTerms(File f, GOAnnotations<PathwayAnnotation> annot) {
	Collection<GOTerm> terms = annot.getTerms(f.getAbsolutePath());
	String s = "";
	for(GOTerm t : terms) {
		s += t.getName() + " (" + t.getId() + "); ";
	}
	return s;
}

static GOAnnotations<XrefAnnotation> getGOAnnotations(GOTree tree) throws IOException {
	return GOAnnotations.read(
			goAnnotFile, tree, new GOAnnotationFactory<XrefAnnotation>() {
				public Collection<XrefAnnotation> createAnnotations(String id, String evidence) {
					return Arrays.asList(new XrefAnnotation[] { new XrefAnnotation(id, BioDataSource.ENSEMBL_MOUSE, evidence) });
				}
			});
}

private static final File idMapperFile = new File("/home/thomas/PathVisio-Data/gene_databases/Mm_Derby_20090509.pgdb");
private static IDMapperRdb idMapper;

static IDMapperRdb getIdMapper() throws IDMapperException {
	if(idMapper == null) idMapper = SimpleGdbFactory.createInstance("" + idMapperFile, new DataDerby(), 0);
	return idMapper;
}
}
