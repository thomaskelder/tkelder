import walkietalkie.*
import walkietalkie.WalkieTalkie.Parameters
import walkietalkie.WalkieTalkie.PathwayInfo
import pvtools.*
import org.bridgedb.bio.*
import org.bridgedb.*
import org.pathvisio.gex.SimpleGex
import cytoscape.*
import cytoscape.data.CyAttributes

File outFile = new File("/home/thomas/data/rowett/fishoil_bachmair/output");
File dataFile = new File("/home/thomas/data/rowett/fishoil_bachmair/input/Cytoscape.pgex");
def pathwayFiles = [
    "KEGG" : new File("/home/thomas/data/rowett/fishoil_bachmair/pathways/Hsa-KEGG_20100914"),
    "WikiPathways" : new File("/home/thomas/data/rowett/fishoil_bachmair/pathways/wikipathways-analysis-20110513"),
    "Reactome" : new File("/home/thomas/data/rowett/fishoil_bachmair/pathways/Reactome_20110509"),
    "KEGG, WikiPathways and Reactome" : new File("/home/thomas/data/rowett/fishoil_bachmair/pathways")
]
String idmRdb = "idmapper-pgdb:/home/thomas/data/bridgedb/Hs_Derby_20100601.bridge"

//Load the ID mapper database
Class.forName("org.bridgedb.rdb.IDMapperRdb")
Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
IDMapper idmapper = new IDMapperStack()
idmapper.addIDMapper(idmRdb);

//Open the PathVisio dataset
SimpleGex data = PVToolsPlugin.openDataSet(dataFile);

//Additional parameters to WalkieTalkie
Parameters par = Parameters.create()
    .minGeneConnections(0)
    .dataSource(BioDataSource.UNIPROT);

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

//Load the expression data
println("Loading attributes...");
def textCols = [ "SSP", "KEGG", "Uniprot Accession", "Uniprot Protein Name", "Ensembl" ];
PVToolsPlugin.loadAttributes(
        data, 
        new AttributeOptions().excludeCols(textCols)
);
PVToolsPlugin.loadTextAttributes(data, textCols);

//Apply layout
import cytoscape.*
import cytoscape.view.*
import cytoscape.layout.*
for(CyNetworkView view in Cytoscape.getNetworkViewMap().values()) {
	CyLayouts.getLayout("Kamada-Kawai-Noweight").doLayout(view);
}


//Add charts for data values
//http://chart.apis.google.com/chart?chs=300x300&cht=p&chd=t:33,33,33&chco=FF0000,00FF00,0000FF
import cytoscape.*
import cytoscape.data.CyAttributes
import java.awt.Color

Gradient gradient = new Gradient()
	.point(-1000, Color.green)
	.point(0, Color.white)
	.point(1000, Color.red)
	
CyAttributes attr = Cytoscape.getNodeAttributes();

def an = "difference spot density"
Cytoscape.getCyNodesList().each { node ->
	def values = attr.getListAttribute(node.getIdentifier(), an)
	def url = ""
	if(values.size() > 0) {
		url = "http://chart.apis.google.com/chart?chs=300x300&cht=p&chf=bg,s,00000000"
		def nr = values.size()
		url += "&chd=t:" + values.collect { 1.0 / nr }.join(',')
		
		def colors = values.collect { v ->
			Color c = gradient.calculate(v)
			Integer.toHexString(c.getRGB() & 0x00ffffff);
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
