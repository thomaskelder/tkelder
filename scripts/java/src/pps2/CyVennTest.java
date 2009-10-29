package pps2;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.DataDerby;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.preferences.PreferenceManager;
import org.pathvisio.visualization.colorset.Criterion;
import org.pathvisio.visualization.colorset.Criterion.CriterionException;

import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;

public class CyVennTest {
	public static void main(String[] args) {
		PreferenceManager.init();
		BioDataSource.init();

		try {
			CyVennTest vd = new CyVennTest();
			vd.start();
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	File outPath = new File("/home/thomas/projects/pps2/stat_results/bigcat/TimeVsZero");
	File gexFile = new File("/home/thomas/projects/pps2/stat_results/PPS2_timecourse_pathvisio.pgex");

	String[] timePoints = new String[] {
			"t0.6", "t2", "t18", "t48"
	};

	SimpleGex data;

	public CyVennTest() throws IDMapperException {
		outPath.mkdirs();
		data = new SimpleGex("" + gexFile, false, new DataDerby());
	}

	void start() throws IDMapperException, CriterionException, MalformedURLException, IOException {
		double q = 0.05;

		for(String diet : ["HF", "LF"]) {
			CyNetwork network = Cytoscape.createNetwork(tissue, true);

			String[] criteria = new String[timePoints.length ];

			for(int i = 0; i < timePoints.length; i++) {
				String t1 = timePoints[i];
				String c = "[" + diet + "_limma_t0_vs_" + t1 + "_qvalue] < " + q;
				criteria[i] = c;
			}

			List<Set<Xref>> matches = new ArrayList<Set<Xref>>();

			for(String expr : criteria) {
				Criterion c = new Criterion();
				c.setExpression(expr, data.getSampleNames());

				Set<Xref> match = new HashSet<Xref>();
				matches.add(match);

				int maxRow = data.getNrRow();
				for(int i = 0; i < maxRow; i++) {
					Map<String, Object> sdata = data.getRow(i).getByName();
					Xref xref = data.getRow(i).getXref();
					if(c.evaluate(sdata)) match.add(xref);
				}
			}

			List<CyNode> nodes = new ArrayList<CyNode>();

			for(int i = 0; i < matches.size(); i++) {
				CyNode n = Cytoscape.getCyNode(diet + timePoints[i], true);
				network.addNode(n);
			}

			CyAttributes attr = Cytoscape.getEdgeAttributes();
			
			for(int i = 0; i < matches.size(); i++) {
				for(int j = i + 1; j < matches.size(); j++) {
					
					CyNode ni = nodes.get(i);
					CyNode nj = nodes.get(j);
					CyEdge e = Cytoscape.getCyEdge(
							ni.getIdentifier(),
							ni.getIdentifier() + "->" + nj.getIdentifier(),
							nj.getIdentifier(), 
							"na");
					network.addEdge(e);
					Set<Xref> shared = new HashSet<Xref>(matches.get(i));
					shared.retainAll(matches.get(j));
					attr.setAttribute(e.getIdentifier(), "nshared", shared.size());
				}
			}
		}
	}
}
