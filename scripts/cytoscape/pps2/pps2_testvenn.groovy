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

File gexFile = new File("/home/thomas/projects/pps2/stat_results/PPS2_timecourse_pathvisio.pgex");

String[] timePoints = ["t0.6", "t2", "t18", "t48"];

SimpleGex data = new SimpleGex("" + gexFile, false, new DataDerby());

double q = 0.05;

for(String diet : ["HF", "LF"]) {
    CyNetwork network = Cytoscape.createNetwork(diet, true);

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
        println("Adding node " + n);
        network.addNode(n);
        nodes.add(n);
    }

    CyAttributes attr = Cytoscape.getEdgeAttributes();
    
    for(int i = 0; i < matches.size(); i++) {
        for(int j = i + 1; j < matches.size(); j++) {
            Set<Xref> shared = new HashSet<Xref>(matches.get(i));
            shared.retainAll(matches.get(j));
            
            if(shared.size() == 0) continue;
            
        	CyNode ni = nodes.get(i);
            CyNode nj = nodes.get(j);
            println(ni);
            println(nj);
			CyEdge e = Cytoscape.getCyEdge(
					ni.getIdentifier(),
					ni.getIdentifier() + "->" + nj.getIdentifier(),
					nj.getIdentifier(), 
					"na");
            println(e);
            network.addEdge(e);

            attr.setAttribute(e.getIdentifier(), "nshared", (int)shared.size());
        }
    }
}