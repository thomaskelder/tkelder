package org.pathvisio.go;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import junit.framework.TestCase;

import org.pathvisio.go.mapper.GOMapper;

public class TestGO extends TestCase {
	GOTree goTree;

	protected void setUp() throws Exception {
		try {
			goTree = GOReader.readGOTree(
					new File("/home/thomas/projects/go-pathway-mapping/gene_ontology_edit.obo")
			);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private GOMapper loadPathwayGoTree() throws IOException {
		GOMapper goMapper = new GOMapper(goTree);

		//Read existing mappings
		goMapper.setPathwayAnnotations(GOAnnotations.read(
				new File("/home/thomas/projects/go-pathway-mapping/pathway-mapping-proc.txt"),
				goTree, new GOAnnotationFactory() {
					public GOAnnotation createAnnotation(String id,
							String evidence) {
						return new PathwayAnnotation(id, Double.parseDouble(evidence));
					}
		}));
		return goMapper;
	}
	
	public void testReadGO() {
		//Test parent child relationship
		//GO:0016779 is_a GO:0016772
		GOTerm term = goTree.getTerm("GO:0016772");
		Set<GOTerm> children = goTree.getChildren(term.getId());
		assertTrue(children.contains(goTree.getTerm("GO:0016779")));

		//Test child parent relationship
		//GO:0016760 is_a GO:0016759 and is_a GO:0035251
		term = goTree.getTerm("GO:0016760");
		Set<GOTerm> parents = goTree.getParents(term.getId());
		assertTrue(parents.contains(goTree.getTerm("GO:0016759")));
		assertTrue(parents.contains(goTree.getTerm("GO:0035251")));
	}

	public void testPrune() {
		try {
			GOMapper goMapper = loadPathwayGoTree();

			GOAnnotations pruned = goMapper.prune(0.6);
			
			//Test that no parent or child has the same annotation
			for(GOTerm term : goTree.getTerms()) {
				Collection<GOAnnotation> annot = pruned.getAnnotations(term);
				for(GOTerm p : goTree.getParents(term.getId())) {
					for(GOAnnotation a : annot) {
						if(pruned.getAnnotation(p, a.getId()) != null) {
							fail("Parent also has annotation " + a.getId());
						}
					}
				}
				for(GOTerm c : goTree.getChildren(term.getId())) {
					for(GOAnnotation a : annot) {
						if(pruned.getAnnotation(c, a.getId()) != null) {
							fail("Child also has annotation " + a.getId());
						}
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
