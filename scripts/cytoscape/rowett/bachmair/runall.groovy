import walkietalkie.*;
import walkietalkie.WalkieTalkie.Parameters;
import walkietalkie.WalkieTalkie.PathwayInfo;
import pvtools.*;
import rainbownodes.*;
import org.pathvisio.plugins.statistics.*;
import org.bridgedb.bio.*;
import org.bridgedb.*;
import org.pathvisio.visualization.colorset.*;
import org.pathvisio.gex.SimpleGex;
import cytoscape.*;
import cytoscape.view.*;
import cytoscape.layout.*;
import cytoscape.data.CyAttributes;
import cytoscape.visual.*;
import java.awt.Color;

File outFile = new File("/home/kato/data/rowett/bachmair/output2");
File dataFile = new File("/home/kato/data/rowett/bachmair/data/Cytoscape amended protein list 2012-02-14.pgex");
def pathwayFiles = [
    "KEGG" : new File("/home/kato/data/rowett/bachmair/pathways/hsa_KEGG_gpml-20100614"),
    "WikiPathways" : new File("/home/kato/data/rowett/bachmair/pathways/wikipathways-analysiscollection"),
    "KEGG and WikiPathways" : new File("/home/kato/data/rowett/bachmair/pathways")
];
String idmRdb = "idmapper-pgdb:/home/kato/data/bridgedb/Hs_Derby_20110601.bridge"
String idmTxt = "idmapper-text:file:///home/kato/data/rowett/bachmair/custom_entrez_ens.txt"

//Load the ID mapper database
Class.forName("org.bridgedb.file.IDMapperText")
Class.forName("org.bridgedb.rdb.IDMapperRdb")
Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
IDMapper idmapper = new IDMapperStack()
idmapper.addIDMapper(idmRdb);
idmapper.addIDMapper(idmTxt);

//Open the PathVisio dataset
SimpleGex data = PVToolsPlugin.openDataSet(dataFile);

/*
//Additional parameters to WalkieTalkie
Parameters par = Parameters.create()
    .minGeneConnections(1)
    .dataSource(BioDataSource.ENSEMBL_HUMAN);

pathwayFiles.each {
    //Load the pathways
    Set<PathwayInfo> pathways = WalkieTalkieUtils.readPathways(it.value);
    
    //Build the network including all proteins in the dataset
    WalkieTalkie wt = new WalkieTalkie(
        par, null, pathways, idmapper, data
    );
    CyNetwork network = WalkieTalkiePlugin.loadSif(wt, true);
    network.setTitle(it.key);
    //Load the label and type attributes
    WalkieTalkiePlugin.loadAttributes(wt);
}
*/

//Load the expression data
println("Loading attributes...");
def textCols = [ "SSP", "KEGG", "Uniprot Accession#", "Uniprot Protein Name", "Ensembl", "KEGG" ];
PVToolsPlugin.loadAttributes(
        data, 
        new AttributeOptions().excludeCols(textCols)
);
PVToolsPlugin.loadTextAttributes(data, textCols);

/*
//Apply node graphics to color by CHANGE column
import rainbownodes.*;
import java.awt.Color;
import cytoscape.*;
import cytoscape.view.*;
import cytoscape.layout.*;

//Perform layout and set graphics on all networks
for(CyNetworkView view in Cytoscape.getNetworkViewMap().values()) {
    // Get the network view and perform layout algorithm
    CyLayouts.getLayout("kamada-kawai").doLayout(view);

    //Apply the graphics
    Gradient gradient = new Gradient()
        .point(-4, Color.BLUE)
        .point(0, Color.WHITE)
        .point(4, Color.YELLOW);

    Graphic pieGraph = new PieGraphic(["Change"], gradient);
    GraphicsManager.getInstance().addGraphic(
        pieGraph, view);
        
    //Save a legend for the graphics
    pieGraph.saveLegend(new File(outFile, "legend_spotdiff.png"), "Legend for spot density difference");
}
*/

//Add charts for data values
//http://chart.apis.google.com/chart?chs=300x300&cht=p&chd=t:33,33,33&chco=FF0000,00FF00,0000FF
import cytoscape.*
import cytoscape.data.CyAttributes
import java.awt.Color

Gradient gradient = new Gradient()
	.point(-4, Color.BLUE)
	.point(0, Color.WHITE)
	.point(4, Color.YELLOW)
	
CyAttributes attr = Cytoscape.getNodeAttributes();

def an = "Change"
Cytoscape.getCyNodesList().each { node ->
	def values = attr.getListAttribute(node.getIdentifier(), an)
	def url = ""
	if(values.size() > 0) {
		url = "http://chart.apis.google.com/chart?chs=300x300&cht=p&chf=bg,s,00000000"
		def nr = values.size()
		url += "&chd=t:" + values.collect { 1.0 / nr }.join(',')
		
		def colors = values.collect { v ->
			Color c = gradient.calculate(v)
			//Integer.toHexString(c.getRGB() & 0x00ffffff);
			String.format("%06X", (0xFFFFFF & c.getRGB()));
		}
		
		url += "&chco=" + colors.join(",")
	}
	println values
	println url
	attr.setAttribute(node.getIdentifier(), "node.customGraphics1", url)
}

class Gradient {
	SortedMap<Double, Color> pointColors;

	Gradient() {
		pointColors = new TreeMap<Double, Color>();
	}

	void addPoint(double value, Color c) {
		pointColors.put(value, c);
	}
	
	Gradient point(double value, Color c) {
		addPoint(value, c);
		return this;
	}

	double getMin() {
		return pointColors.firstKey();
	}

	double getMax() {
		return pointColors.lastKey();
	}
	
	Color calculate(double value) {
		double valueStart = getMin();
		double valueEnd = getMax();
		Color colorStart = null;
		Color colorEnd = null;

		//Easy cases
		if(value <= getMin()) return pointColors.get(getMin());
		if(value >= getMax()) return pointColors.get(getMax());

		//Find what colors the value is in between
		//Keys are sorted!
		List<Double> values = new ArrayList<Double>(pointColors.keySet());
		for(int i = 0; i < values.size() - 1; i++) {
			double d = values.get(i);
			double dnext = values.get(i + 1);
			if(value > d && value < dnext)
			{
				valueStart = d;
				colorStart = pointColors.get(d);
				valueEnd = dnext;
				colorEnd = pointColors.get(dnext);
				break;
			}
		}

		if(colorStart == null || colorEnd == null) return null; //Check if the values/colors are found

		// Interpolate to find the color belonging to the given value
		double alpha = (value - valueStart) / (valueEnd - valueStart);
		double red = colorStart.getRed() + alpha*(colorEnd.getRed() - colorStart.getRed());
		double green = colorStart.getGreen() + alpha*(colorEnd.getGreen() - colorStart.getGreen());
		double blue = colorStart.getBlue() + alpha*(colorEnd.getBlue() - colorStart.getBlue());

		return new Color((int)red, (int)green, (int)blue);
	}
}
