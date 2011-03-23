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

File outFile = new File("/home/thomas/data/rowett/output2");
File dataFile = new File("/home/thomas/data/rowett/data/Cytoscape.pgex");
def pathwayFiles = [
    "KEGG" : new File("/home/thomas/data/rowett/pathways/hsa_KEGG_gpml-20100614"),
    "WikiPathways" : new File("/home/thomas/data/rowett/pathways/wikipathways-analysiscollection"),
    "KEGG and WikiPathways" : new File("/home/thomas/data/rowett/pathways")
];
String idmRdb = "idmapper-pgdb:/home/thomas/data/bridgedb/Hs_Derby_20100601.bridge"
String idmTxt = "idmapper-text:file:///home/thomas/data/rowett/custom_entrez_ens.txt"

//Load the ID mapper database
Class.forName("org.bridgedb.file.IDMapperText")
Class.forName("org.bridgedb.rdb.IDMapperRdb")
Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
IDMapper idmapper = new IDMapperStack()
idmapper.addIDMapper(idmRdb);
idmapper.addIDMapper(idmTxt);

//Open the PathVisio dataset
SimpleGex data = PVToolsPlugin.openDataSet(dataFile);

//Additional parameters to WalkieTalkie
Parameters par = Parameters.create()
    .minGeneConnections(1)
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
        .point(-1E3, Color.BLUE)
        .point(0, Color.WHITE)
        .point(1E3, Color.YELLOW);

    Graphic pieGraph = new PieGraphic(["Difference spot density"], gradient);
    GraphicsManager.getInstance().addGraphic(
        pieGraph, view);
        
    //Save a legend for the graphics
    pieGraph.saveLegend(new File(outFile, "legend_spotdiff.png"), "Legend for spot density difference");
}
