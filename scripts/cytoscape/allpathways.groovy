import walkietalkie.*;
import walkietalkie.WalkieTalkie.Parameters;
import walkietalkie.WalkieTalkie.PathwayInfo;
import pvtools.*;
import org.bridgedb.rdb.*;
import org.bridgedb.bio.*;
import cytoscape.*;

String idmPath = "/home/thomas/PathVisio-Data/gene databases/Mm_Derby_20090509.pgdb";
String pathwayPath = "/home/thomas/data/pathways/20090810/mmu/";

//Load the ID mapper database
IDMapperRdb idmapper = PVToolsPlugin.openIDMapper(new File(idmPath));
//Load the pathways
Set<PathwayInfo> pathways = WalkieTalkieUtils.readPathways(new File(pathwayPath));

//Additional parameters
Parameters par = Parameters.create()
    .minGeneConnections(1);

//Generate the master pathway-protein network (containing all significant genes)
WalkieTalkie wt = new WalkieTalkie(
    par, null, pathways, idmapper, null
);
CyNetwork network = WalkieTalkiePlugin.loadSif(wt, true);
network.setTitle("all pathways");