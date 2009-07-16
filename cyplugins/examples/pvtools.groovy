import pvtools.*;
import org.bridgedb.rdb.*;
import org.pathvisio.visualization.colorset.*;
import org.pathvisio.gex.SimpleGex;
import org.pathvisio.plugins.statistics.*;
import cytoscape.*;

//!! Before running this example, please run walkietalkie.groovy first !!
/*
This example shows you how to load attributes from a PathVisio dataset and
z-scores into a cytoscape network.
*/

//Change dataPath to the right example-data directory in your svn checkout
String dataPath = "/home/thomas/code/googlerepo/cyplugins/examples/example-data/";
//Change this to the location of the mouse synonym database (see http://www.pathvisio.org)
File idmFile = new File("/home/thomas/PathVisio-Data/gene databases/Mm_Derby_20090509.pgdb");

File dataFile = new File(dataPath + "dataset.pgex");
File pathwayDir = new File(dataPath);
String expression = "[pvalue] < 0.05";

//Load the PathVisio dataset
println("Loading dataset " + dataFile);
SimpleGex data = PVToolsPlugin.openDataSet(dataFile);

println("Loading attributes...");
PVToolsPlugin.loadAttributes(data);

//Load the ID mapper database
IDMapperRdb idmapper = PVToolsPlugin.openIDMapper(idmFile);

//Define a criterion for calculating the zscore
crit = new Criterion();
crit.setExpression(expression, data.getSampleNames());

ZScoreCalculator zsc = new ZScoreCalculator(
    crit, pathwayDir, data, idmapper, null
);
println("Calculating z-scores...");
StatisticsResult stats = zsc.calculateAlternative();

println("Loading z-scores...");
String attrName = "zscore-p0.05";
PVToolsPlugin.loadZscores(stats, attrName);

//Create a visual style that scales the pathway nodes by z-score
PVToolsPlugin.setZscoreVisualStyle(Cytoscape.getCurrentNetworkView().getVisualStyle(), attrName, 4, 10, 80);
