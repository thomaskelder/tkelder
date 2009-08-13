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
import PPSGlobals;

File idmFile = new File(PPSGlobals.idmPath);
File pathwayDir = new File(PPSGlobals.pathwayPath);

String expression = PPSGlobals.expression;

//Load the ID mapper database
IDMapperRdb idmapper = PVToolsPlugin.openIDMapper(idmFile);

//Load the pathways
Set<PathwayInfo> pathways = WalkieTalkieUtils.readPathways(pathwayDir);

//Additional parameters
Parameters par = Parameters.create()
    .minGeneConnections(1);

//Open the PathVisio dataset
SimpleGex data = PVToolsPlugin.openDataSet(PPSGlobals.dataFile);

//Define the criterion to select siginifcant genes
crit = new Criterion();
crit.setExpression(expression, data.getSampleNames());

//Generate the master pathway-protein network (containing all significant genes)
WalkieTalkie wt = new WalkieTalkie(
    par, crit, pathways, idmapper, data
);
CyNetwork network = WalkieTalkiePlugin.loadSif(wt, true);
network.setTitle("all significant");
//Load the label and type attributes
WalkieTalkiePlugin.loadAttributes(wt);

//Load the expression data
println("Loading attributes...");
PVToolsPlugin.loadAttributes(data);

//Calculate z-scores
ZScoreCalculator zsc = new ZScoreCalculator(
	    crit, pathwayDir, data, idmapper, null
);
println("Calculating z-scores...");
StatisticsResult stats = zsc.calculateAlternative();
stats.save(new File(PPSGlobals.outPath, "pps2_HFvsLF_t0_zscores_q0.05.txt"));

println("Loading z-scores...");
String attrName = "zscore_q0.05";
PVToolsPlugin.loadZscores(stats, attrName);

//	Create a visual style that scales the pathway nodes by z-score
VisualStyle zvis = PVToolsPlugin.createZscoreVisualStyle("zscore_q_0_05", WalkieTalkieVisualStyle.NAME, attrName, 6, 10, 80);

// Generate networks filtered by z-score
double[] zscores = [ 2, 3, 4 ];
for(z in zscores) {
	Set<PathwayInfo> filter = WalkieTalkieUtils.pathwaysByZScore(pathwayDir, stats, z);
	CyNetwork n = WalkieTalkiePlugin.loadSif(wt, filter, true);
	String title = "z-score above " + z;
	n.setTitle(title);
    CyNetworkView v = Cytoscape.getNetworkView(n.getIdentifier());
    v.setTitle(title);
}

//Perform layout and vizmapper on all networks
for(CyNetworkView view in Cytoscape.getNetworkViewMap().values()) {
	// Get the network view and perform layout algorithm
	CyLayouts.getLayout("kamada-kawai").doLayout(view);

	// Apply the visual style
	Cytoscape.getDesktop().setFocus(view.getNetwork().getIdentifier());
	Cytoscape.getVisualMappingManager().setVisualStyle(zvis.getName());
}
