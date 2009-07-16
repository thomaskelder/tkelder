import walkietalkie.*;
import walkietalkie.WalkieTalkie.Parameters;
import walkietalkie.WalkieTalkie.PathwayInfo;
import pvtools.*;
import rainbownodes.*;
import org.bridgedb.rdb.*;
import org.bridgedb.bio.*;
import org.pathvisio.visualization.colorset.*;
import org.pathvisio.gex.SimpleGex;
import cytoscape.*;
import cytoscape.view.*;
import cytoscape.layout.*;
import cytoscape.data.CyAttributes;
import PPSGlobals;

//Function to calculate log2 ratio and set to attributes
def setLog2ratio = { network, attr1, attr2, attrNew ->
	CyAttributes attr = Cytoscape.getNodeAttributes();
	for(Object no : Cytoscape.getCyNodesList()) {
		String id = ((CyNode)no).getIdentifier();
		if(attr.hasAttribute(id, attr1) && attr.hasAttribute(id, attr2)) {
			double d1 = attr.getDoubleAttribute(id, attr1);
			double d2 = attr.getDoubleAttribute(id, attr2);
			attr.setAttribute(id, attrNew, Math.log(d2/d1)/Math.log(2));
		}
	}
}

File idmFile = new File("/home/thomas/PathVisio-Data/gene databases/Mm_Derby_20090509.pgdb");
File pathwayDir = new File("/home/thomas/data/pathways/20090715");

//Load the ID mapper database
IDMapperRdb idmapper = PVToolsPlugin.openIDMapper(idmFile);

//Load the pathways
Set<PathwayInfo> pathways = WalkieTalkieUtils.readPathways(pathwayDir);

//Additional parameters
Parameters par = Parameters.create()
    .minGeneConnections(1)
    .dataSource(BioDataSource.UNIPROT);

for(String s : PPSGlobals.stats) {
    println("Processing " + s + "...");
    //Open the PathVisio dataset
    File dataFile = new File(PPSGlobals.dataPath + "pps3_grahams_" + s + ".pgex");
    SimpleGex data = PVToolsPlugin.openDataSet(dataFile);
    //Generate a pathway-protein network
    WalkieTalkie wt = new WalkieTalkie(
        par, null, pathways, idmapper, data
    );
    CyNetwork network = WalkieTalkiePlugin.loadSif(wt, true);
    network.setTitle(s);
    //Load the label and type attributes
    WalkieTalkiePlugin.loadAttributes(wt);
    
    //Load the expression data
    println("Loading attributes...");
    PVToolsPlugin.loadAttributes(data);
    
    //Calculate log2 ratios
    List<String> visAttr = null;
    switch(s) {
    	case "time":
    	    visAttr = PPSGlobals.getVisAttr(s);
    	    setLog2ratio(network, "10% 1W L", "10% 4W L", visAttr[0]);
    		setLog2ratio(network, "10% 1W V", "10% 4W V", visAttr[1]);
    		setLog2ratio(network, "45% 1W L", "45% 4W L", visAttr[2]);
    		setLog2ratio(network, "45% 1W V", "45% 4W V", visAttr[3]);
    		break;
    	case "leptin":
    		visAttr = PPSGlobals.getVisAttr(s);
    	    setLog2ratio(network, "10% 1W L", "10% 1W V", visAttr[0]);
    		setLog2ratio(network, "10% 4W L", "10% 4W V", visAttr[1]);
    		setLog2ratio(network, "45% 1W L", "45% 1W V", visAttr[2]);
    		setLog2ratio(network, "45% 4W L", "45% 4W V", visAttr[3]);
    		break;
    	case "diet":
    		visAttr = PPSGlobals.getVisAttr(s);
    	    setLog2ratio(network, "10% 1W V", "45% 1W V", visAttr[0]);
    		setLog2ratio(network, "10% 4W V", "45% 4W V", visAttr[1]);
    		setLog2ratio(network, "10% 1W V", "45% 1W V", visAttr[2]);
    		setLog2ratio(network, "10% 4W L", "45% 4W L", visAttr[3]);
	    	break;
    }
    
    //Get the network view and perform layout algorithm
    CyNetworkView view = Cytoscape.getNetworkView(network.getIdentifier());
    view.setTitle(s);
    CyLayouts.getLayout("kamada-kawai").doLayout(view);

    //Apply the visual style "WalkieTalkie" (included in this plugin)
    Cytoscape.getVisualMappingManager().setVisualStyle(WalkieTalkieVisualStyle.NAME);
}