import walkietalkie.*;
import walkietalkie.WalkieTalkie.Parameters;
import walkietalkie.WalkieTalkie.PathwayInfo;
import pvtools.*;
import rainbownodes.*;
import org.pathvisio.plugins.statistics.*;
import org.bridgedb.rdb.*;
import org.bridgedb.bio.*;
import org.pathvisio.visualization.colorset.*;
import org.pathvisio.gex.SimpleGex;
import cytoscape.*;
import cytoscape.view.*;
import cytoscape.layout.*;
import cytoscape.data.CyAttributes;
import cytoscape.visual.*;
import java.awt.Color;

File outFile = new File("/home/thomas/projects/demo_michiel");
File dataFile = new File("/home/thomas/projects/demo_michiel/ChIPTargetList_t47DGeneExpression_Combined_PathvisioTable.pgex");
File pathwayFile = new File("/home/thomas/data/pathways/20090916_Hs");
File idmFile = new File("/home/thomas/PathVisio-Data/gene databases/Hs_Derby_20090509.pgdb");

expr_p = 0.01;
chip_p = 0.05;

//Load the ID mapper database
IDMapperRdb idmapper = PVToolsPlugin.openIDMapper(idmFile);
//Open the PathVisio dataset
SimpleGex data = PVToolsPlugin.openDataSet(dataFile);
//Load the pathways
Set<PathwayInfo> pathways = WalkieTalkieUtils.readPathways(pathwayFile);

//Additional parameters to WalkieTalkie
Parameters par = Parameters.create()
    .minGeneConnections(1);

Criterion exprCrit = new Criterion();
exprCrit.setExpression(
		"[GeneExpression_FDR] <= " + expr_p + " OR [ChIP_FDR] <= " + chip_p, 
		data.getSampleNames()
);

//Build the network including all genes that have a significant
//change in expression
WalkieTalkie wt = new WalkieTalkie(
    par, exprCrit, pathways, idmapper, data
);
CyNetwork network = WalkieTalkiePlugin.loadSif(wt, true);
network.setTitle("all significant expression");
//Load the label and type attributes
WalkieTalkiePlugin.loadAttributes(wt);

//Load the expression data
println("Loading attributes...");
PVToolsPlugin.loadAttributes(data);
PVToolsPlugin.loadTextAttributes(data, ["DataType"]);

//Calculate z-scores
ZScoreCalculator zsc = new ZScoreCalculator(
	    exprCrit, pathwayFile, data, idmapper, null
);
println("Calculating z-scores...");
StatisticsResult stats = zsc.calculateAlternative();

String attrName = "zscore_expr_p" + expr_p;
PVToolsPlugin.loadZscores(stats, attrName);

//	Create a visual style that scales the pathway nodes by z-score
VisualStyle zvis = PVToolsPlugin.createZscoreVisualStyle("zscore_expr", WalkieTalkieVisualStyle.NAME, attrName, 6, 10, 80);

// Generate networks filtered by z-score
double[] zscores = [ 2 ];
for(z in zscores) {
	Set<PathwayInfo> filter = WalkieTalkieUtils.pathwaysByZScore(pathwayFile, stats, z);
	CyNetwork n = WalkieTalkiePlugin.loadSif(wt, filter, true);
	String title = "z-score above " + z;
	n.setTitle(title);
    CyNetworkView v = Cytoscape.getNetworkView(n.getIdentifier());
    v.setTitle(title);
}

// Generate networks containing only pathways that have at least one
// gene measured by chip experiment
Parameters parChip = Parameters.create()
    .minGeneConnections(1);

Criterion exprChip = new Criterion();
exprChip.setExpression("[ChIP_FDR] <= " + chip_p, data.getSampleNames());

//Build the network including all genes that have a significant
//change in expression
WalkieTalkie wtChip = new WalkieTalkie(
	parChip, exprChip, pathways, idmapper, data
);
Set<PathwayInfo> chipPathways = wtChip.getIncludedPathways();
CyNetwork chipNetwork = WalkieTalkiePlugin.loadSif(wt, chipPathways, true);
chipNetwork.setTitle("ChIP FDR " + chip_p);

//Apply bar graphics
//Create the graphics
//Gradient gradient = new Gradient()
//            .point(0, Color.WHITE)
//            .point(13, Color.RED);
//
//List<String> visAttr = [ "Expr_t0", "Expr_t1", "Expr_t2", "Expr_t4", "Expr_t8", "Expr_t12", "Expr_t24" ];
//Graphic barGraph = new BarGraphic(visAttr, gradient);
//barGraph.saveLegend(new File(outFile, "legend.png"), "Legend for expression data");

//Perform layout and vizmapper on all networks
for(CyNetworkView view in Cytoscape.getNetworkViewMap().values()) {
	// Get the network view and perform layout algorithm
	CyLayouts.getLayout("kamada-kawai").doLayout(view);

	// Apply the visual style
	Cytoscape.getDesktop().setFocus(view.getNetwork().getIdentifier());
	Cytoscape.getVisualMappingManager().setVisualStyle(zvis.getName());
	
	//GraphicsManager.getInstance().addGraphic(barGraph, view);
}
