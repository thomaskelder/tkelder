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

def createFiltered = { tissue, wt, pathways, filterFiles, title ->
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
	VisualStyle vis = Cytoscape.getNetworkView(tissue + " - all significant").getVisualStyle();
	
	//Set the visual style
	CyNetworkView view = Cytoscape.getCurrentNetworkView();
	CyLayouts.getLayout("force-directed").doLayout(view);
	Cytoscape.getDesktop().setFocus(view.getNetwork().getIdentifier());
	Cytoscape.getVisualMappingManager().setVisualStyle(vis.getName());
}

File idmFile = new File(PPSGlobals.idmPath);
File pathwayDir = new File(PPSGlobals.pathwayPath);

//Load the ID mapper database
IDMapperRdb idmapper = PVToolsPlugin.openIDMapper(idmFile);

//Load the pathways
Set<PathwayInfo> pathways = WalkieTalkieUtils.readPathways(pathwayDir);

//Additional parameters
Parameters par = Parameters.create()
    .minGeneConnections(1)
	.firstNeighbours(false);

//Open the PathVisio dataset
SimpleGex data = PVToolsPlugin.openDataSet(PPSGlobals.dataFile);

for(String tissue in PPSGlobals.tissues) {
	//Define the criterion to select siginifcant genes
	crit = new Criterion();
	crit.setExpression(PPSGlobals.expression.replaceAll("TIS", tissue), data.getSampleNames());
	
	//Generate the filtered networks
	WalkieTalkie wt = new WalkieTalkie(
	    par, crit, pathways, idmapper, data
	);
	
	List<File> filterFiles = new ArrayList<File>();
	
	String[] filterNames = [
		"Mm_Fatty_Acid_Omega_Oxidation_WP33_20747.gpml",
		"Mm_Fatty_Acid_Beta_Oxidation_3_WP415_21556.gpml",
		"Mm_Beta_Oxidation_of_Unsaturated_Fatty_Acids_WP204_21109.gpml",
		"Mm_Mitochondrial_LC-Fatty_Acid_Beta-Oxidation_WP401_21526.gpml",
		"Mm_Fatty_Acid_Beta_Oxidation_1_WP221_21143.gpml",
		"Mm_Fatty_Acid_Beta_Oxidation_2_WP82_20851.gpml",
		"mmu00010.xml_map.gpml",
		"Mm_Synthesis_and_Degradation_of_Ketone_Bodies_WP543_21822.gpml",
		"mmu00072.xml_map.gpml",
	];
	for(String f in filterNames) filterFiles.add(new File(pathwayDir, f));
	createFiltered(tissue, wt, pathways, filterFiles, tissue + "- Fatty acid oxidation, glycolysis, gluconeogenesis and ketogenesis");
}
