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
import cytoscape.view.CyDesktopManager.Arrange;
import cytoscape.util.export.*;
import PPSGlobals;

def createFiltered = { wt, pathways, filterFiles, title ->
	// Get the pathway to filter on
	List<PathwayInfo> filterPws = [];

	for(PathwayInfo pwi in pathways) {
		if(filterFiles.contains(pwi.getFile())) {
			filterPws.add(pwi);
		}
	}
	
	CyNetwork network = WalkieTalkiePlugin.loadSif(wt, filterPws, true);
	network.setTitle(title);

	//Get the visual style used in the master network
	VisualStyle vis = Cytoscape.getNetworkView("all significant").getVisualStyle();
	
	//Set the visual style
	CyNetworkView view = Cytoscape.getCurrentNetworkView();
	CyLayouts.getLayout("force-directed").doLayout(view);
	Cytoscape.getDesktop().setFocus(view.getNetwork().getIdentifier());
	Cytoscape.getVisualMappingManager().setVisualStyle(vis.getName());
	
	//Export to an image
	String ext = "png";
	BitmapExporter exporter = new BitmapExporter(ext, 10);
	CyDesktopManager.arrangeFrames(Arrange.CASCADE);

    view.fitContent();
    File f = new File(PPSGlobals.outPath, view.getTitle() + "." + ext);
    exporter.export(view, new FileOutputStream(f));
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
	.firstNeighbours(true);

//Open the PathVisio dataset
File dataFile = new File(PPSGlobals.dataPath + "PPS2_HFvsLF t0_average 2logratio stats.pgex");
SimpleGex data = PVToolsPlugin.openDataSet(dataFile);

//Define the criterion to select siginifcant genes
crit = new Criterion();
crit.setExpression(PPSGlobals.expression, data.getSampleNames());

//Generate the filtered networks
WalkieTalkie wt = new WalkieTalkie(
    par, crit, pathways, idmapper, data
);

List<File> filterFiles = null;
filterFiles = [ new File(pathwayDir, "Mm_Complement_and_Coagulation_Cascades_KEGG_WP449_22601.gpml") ];
createFiltered(wt, pathways, filterFiles, "Complement and coagulation cascades");
//filterFiles = [ new File(pathwayDir, "Mm_Adipogenesis_WP447_28134.gpml") ];
//createFiltered(wt, pathways, filterFiles, "Adipogenesis");