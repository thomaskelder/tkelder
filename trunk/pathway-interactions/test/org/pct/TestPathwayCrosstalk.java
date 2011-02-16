package org.pct;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.bridgedb.BridgeDb;
import org.bridgedb.IDMapper;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.pct.model.AttributeKey;
import org.pct.model.JungGraph;
import org.pct.model.Network;
import org.pct.util.GpmlUtils;


public class TestPathwayCrosstalk extends TestCase {
	static {
		BioDataSource.init();
	}
	static final File[] pathwayFiles = new File[] {
		new File("test-data/pathway1.gpml"),
		new File("test-data/pathway2.gpml")
	};
	static final File idmapperFile = new File("test-data/idmapper.txt");
	
	Network<Xref, String> interactions;
	PathwayCrossTalk crosstalk;
	IDMapper idmapper;
	Map<String, Set<Xref>> pathways;
	
	protected void setUp() throws Exception {
		try {
			interactions = new Network<Xref, String>(new JungGraph<Xref, String>());
			interactions.readFromXGMML(new FileReader(new File("test-data/interactions.xgmml")), 
					Network.xrefFactory, Network.defaultFactory, true
			);
			Class.forName("org.bridgedb.file.IDMapperText");
			IDMapper idmapper = BridgeDb.connect("idmapper-text:file://" + idmapperFile.getAbsolutePath());
			
			pathways = GpmlUtils.readPathwaySets(Arrays.asList(pathwayFiles), false, idmapper, 
					BioDataSource.ENTREZ_GENE, BioDataSource.CHEBI
			);
			
			crosstalk = new PathwayCrossTalk();
		} catch(Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public void testInteractionCount() {
		try {
			crosstalk.setNperm(1);
			crosstalk.setPerformFisher(false);
			
			System.err.println(pathways);
			
			Network<String, String> ctn = crosstalk.createCrossTalkNetwork(pathways, interactions);
			//Check if the interaction counts and score are correct
			String edge = ctn.getGraph().getEdge(pathwayFiles[0].getName(), pathwayFiles[1].getName());
			String c1 = ctn.getEdgeAttribute(edge,
					AttributeKey.InteractionCount.name());
			String c2 = ctn.getEdgeAttribute(edge,
					AttributeKey.InteractionScore.name());
			assertNotNull(c1);
			assertNotNull(c2);
			//There should be 6 interactions between the test pathways
			assertEquals("5", c1);
			assertEquals("5.0", c2);
			
		} catch(Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testOmitOverlap() {
		try {
			crosstalk.setNperm(1);
			crosstalk.setPerformFisher(false);
			crosstalk.setOmitOverlap(true);
			
			System.err.println(pathways);
			
			Network<String, String> ctn = crosstalk.createCrossTalkNetwork(pathways, interactions);
			//Check if the interaction counts are correct
			String edge = ctn.getGraph().getEdge(pathwayFiles[0].getName(), pathwayFiles[1].getName());
			String c1 = ctn.getEdgeAttribute(edge,
					AttributeKey.InteractionCount.name());
			String c2 = ctn.getEdgeAttribute(edge,
					AttributeKey.InteractionScore.name());
			assertNotNull(c1);
			assertNotNull(c2);
			//There should be 6 interactions between the test pathways
			assertEquals("2", c1);
			assertEquals("2.0", c2);
			
		} catch(Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testWriteXGMML() {
		try {
			crosstalk.setNperm(1);
			crosstalk.setPerformFisher(false);
			
			//Write the network
			File tmp = File.createTempFile("pct", ".xgmml");
			FileWriter out = new FileWriter(tmp);
			Network<String, String> n1 = crosstalk.createCrossTalkNetwork(pathways, interactions);
			n1.writeToXGMML(out);
			out.close();
			
			//Read and compare
			Network<String, String> n2 = new Network<String, String>(
					new JungGraph<String, String>()
			);
			n2.readFromXGMML(new FileReader(tmp), Network.defaultFactory, Network.defaultFactory);
			
			assert(n1.getGraph().getNodes().equals(n2.getGraph().getNodes()));
			assert(n1.getGraph().getEdges().equals(n1.getGraph().getEdges()));
			assert(n1.getNetworkAttributes().equals(n2.getNetworkAttributes()));
			assert(n1.getRawAttributes().equals(n2.getRawAttributes()));
		} catch(Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
