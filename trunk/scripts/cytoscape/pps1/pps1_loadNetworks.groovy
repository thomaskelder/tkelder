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

for(String tissue in PPSGlobals.tissues) {
	println("Processing " + tissue);
	//Define the criterion to select siginifcant genes
	crit = new Criterion();
	println(expression.replaceAll("TIS", tissue));
	crit.setExpression(expression.replaceAll("TIS", tissue), data.getSampleNames());
	
	//Generate the master pathway-protein network (containing all significant genes)
	WalkieTalkie wt = new WalkieTalkie(
	    par, crit, pathways, idmapper, data
	);
	CyNetwork network = WalkieTalkiePlugin.loadSif(wt, true);
	network.setTitle(tissue + " - all significant");
	//Load the label and type attributes
	WalkieTalkiePlugin.loadAttributes(wt);
	
	//Load the expression data
	println("Loading attributes for " + tissue);
	PVToolsPlugin.loadAttributes(data);
	
	//Calculate z-scores for each tissue
	println("Calculating z-scores for " + tissue);
	ZScoreCalculator zsc = new ZScoreCalculator(
		    crit, pathwayDir, data, idmapper, null
	);
	StatisticsResult stats = zsc.calculateAlternative();
	stats.save(new File(PPSGlobals.outPath, "pps1_anova_q0.05_" + tissue + ".txt"));
	
	println("Loading z-scores...");
	String attrName = "zscore_q0.05_" + tissue;
	PVToolsPlugin.loadZscores(stats, attrName);
	//	Create a visual style that scales the pathway nodes by z-score
	VisualStyle zvis = PVToolsPlugin.createZscoreVisualStyle(tissue + "_zscore_q_0_05", WalkieTalkieVisualStyle.NAME, attrName, 6, 10, 80);
	Cytoscape.getDesktop().setFocus(network.getIdentifier());
	Cytoscape.getVisualMappingManager().setVisualStyle(zvis.getName());
}
