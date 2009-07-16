import walkietalkie.*;
import walkietalkie.WalkieTalkie.Parameters;
import walkietalkie.WalkieTalkie.PathwayInfo;
import pvtools.*;
import org.bridgedb.rdb.*;
import org.pathvisio.visualization.colorset.*;
import org.pathvisio.gex.SimpleGex;
import cytoscape.*;
import cytoscape.view.*;
import cytoscape.layout.*;

/*
This example shows you how to run the WalkieTalkie script from a groovy
 script.
*/

//Change dataPath to the right example-data directory in your svn checkout
String dataPath = "/home/thomas/code/googlerepo/cyplugins/examples/example-data/";
//Change this to the location of the mouse synonym database (see http://www.pathvisio.org)
File idmFile = new File("/home/thomas/PathVisio-Data/gene databases/Mm_Derby_20090509.pgdb");

File dataFile = new File(dataPath + "dataset.pgex");
File pathwayDir = new File(dataPath);
String expression = "[pvalue] < 0.05";
int minConnections = 1;

//Load the PathVisio dataset
SimpleGex data = PVToolsPlugin.openDataSet(dataFile);

//Load the ID mapper database
IDMapperRdb idmapper = PVToolsPlugin.openIDMapper(idmFile);

//Load the pathways
Set<PathwayInfo> pathways = WalkieTalkieUtils.readPathways(pathwayDir);

//Define a criterion that selects the significant genes
crit = new Criterion();
crit.setExpression(expression, data.getSampleNames());

//Additional parameters
Parameters par = Parameters.create()
    .minGeneConnections(minConnections);

WalkieTalkie wt = new WalkieTalkie(
        par, crit, pathways, idmapper, data
);

//Load the master network (containing all significant nodes)
//And create a view
CyNetwork network = WalkieTalkiePlugin.loadSif(wt, true);
network.setTitle("example");
//Load the label and type attributes
WalkieTalkiePlugin.loadAttributes(wt);

//Get the network view and perform layout algorithm
CyNetworkView view = Cytoscape.getNetworkView(network.getIdentifier());
CyLayouts.getLayout("kamada-kawai").doLayout(view);

//Apply the visual style "WalkieTalkie" (included in this plugin)
Cytoscape.getVisualMappingManager().setVisualStyle(WalkieTalkieVisualStyle.NAME);
