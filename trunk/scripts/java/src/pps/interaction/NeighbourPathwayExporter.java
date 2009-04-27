package pps.interaction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.bridgedb.DataSource;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pathvisio.debug.Logger;
import org.pathvisio.model.ConverterException;
import org.pathvisio.model.ObjectType;
import org.pathvisio.model.Pathway;
import org.pathvisio.model.PathwayElement;
import org.pathvisio.preferences.PreferenceManager;

/**
 * Exports GPML files containing node-neighbour relations from
 * a sif file. For each node in the network, a GPML file will be created
 * that contains the node plus all first neighbours.
 * @author thomas
 *
 */
public class NeighbourPathwayExporter {
	private static final int NR_DATANODE_X = 10;
	private static final int DATANODE_SPACING = 5 * 15;
	
	@Option(name = "-out", required = true, usage = "The output path to write the GPML files to.")
	private File outPath;
	
	@Option(name = "-sif", required = true, usage = "The sif file defining the network.")
	private File sifFile;
	
	@Option(name = "-minCon", required = true, usage = "Minimum number of neighbours a node should have.")
	private int minCon = 5;
	
	@Option(name = "-sysCode", required = true, usage = "The system code of the ids in the sif file.")
	private String sysCode;
	
	@Option(name = "-symbols", required = false, usage = "An attribute file containing the gene symbols.")
	private File symbolFile;
	
	@Option(name = "-minScore", required = false, usage = "Minimum score an interaction should have to be included.")
	private int minScore = 0;
	
	public static void main(String[] args) {
		PreferenceManager.init();
		Logger.log.setLogLevel(true, true, true, true, true, true);

		NeighbourPathwayExporter main = new NeighbourPathwayExporter();
		CmdLineParser parser = new CmdLineParser(main);
		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			e.printStackTrace();
			parser.printUsage(System.err);
			System.exit(-1);
		}
		
		try {
			Network network = Network.fromSif(
					new BufferedReader(new FileReader(main.sifFile))
			);
			if(main.symbolFile != null) {
				BufferedReader in = new BufferedReader(new FileReader(main.symbolFile));
				network.readSymbols(in);
				in.close();
			}
			
			DataSource ds = DataSource.getBySystemCode(main.sysCode);
			writeGpml(network, main.outPath, main.minCon, ds, main.minScore);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static void writeGpml(Network network, File outPath, int minCon, DataSource targetDs, int minScore) throws ConverterException {
		int nrPathways = 0;
		
		//Clean up network based on minimum score
		for(Node n : new HashSet<Node>(network.getNodes())) {
			for(Edge e : new HashSet<Edge>(network.getOutgoing(n))) {
				int score = Integer.parseInt(e.getType()); //Assumes type is score
				if(score <= minScore) {
					network.removeEdge(e);
				}
			}
			if(network.getFirstNeighbours(n).size() >= minCon) {
				nrPathways++;
			}
		}
		
		Logger.log.trace(nrPathways + " pathways to be exported.");
		
		for(Node n : network.getNodes()) {
			if(network.getFirstNeighbours(n).size() >= minCon) {
				Pathway p = toPathway(network, n, targetDs);
				p.writeToXml(
						new File(outPath, p.getMappInfo().getMapInfoName() + ".gpml"), 
						true
				);
				p.writeToSvg(
						new File(outPath, p.getMappInfo().getMapInfoName() + ".svg")
				);
			}
		}
	}
	
	static Pathway toPathway(Network network, Node node, DataSource targetDs) {
		Pathway pathway = new Pathway();
		pathway.getMappInfo().setMapInfoName(node.getId());
		
		Collection<Node> neighbours = network.getFirstNeighbours(node);
		
		//Add the main datanode
		PathwayElement mainElm = PathwayElement.createPathwayElement(ObjectType.DATANODE);
		mainElm.setInitialSize();
		
		double dnWidth = mainElm.getMWidth();
		double dnHeight = mainElm.getMHeight();
		
		int wx = Math.min(NR_DATANODE_X, neighbours.size());
		
		double mainCx = (wx * (DATANODE_SPACING + dnWidth) + DATANODE_SPACING) / 2; 
		double mainCy = DATANODE_SPACING + dnHeight / 2;
		
		mainElm.setMCenterX(mainCx);
		mainElm.setMCenterY(mainCy);
		mainElm.setTextLabel(node.getId());
		mainElm.setGeneID(node.getId());
		mainElm.setDataSource(targetDs);
		String sym = network.getSymbol(node);
		if(sym != null) mainElm.setTextLabel(sym);
		
		pathway.add(mainElm);
		
		//Add the neighbours
		GridLayout layout = new GridLayout(NR_DATANODE_X);
		List<PathwayElement> layoutElements = new ArrayList<PathwayElement>();
		
		for(Node childNode : neighbours) {
			PathwayElement childElm = PathwayElement.createPathwayElement(ObjectType.DATANODE);
			childElm.setInitialSize();
			childElm.setTextLabel(childNode.getId());
			childElm.setGeneID(childNode.getId());
			childElm.setDataSource(targetDs);
			
			sym = network.getSymbol(childNode);
			if(sym != null) childElm.setTextLabel(sym);
			
			pathway.add(childElm);
			layoutElements.add(childElm);
		}
		
		layout.layout(layoutElements, DATANODE_SPACING, 2 * DATANODE_SPACING + dnHeight);
		return pathway;
	}
}
