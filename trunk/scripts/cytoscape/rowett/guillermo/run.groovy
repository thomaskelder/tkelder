import walkietalkie.*
import walkietalkie.WalkieTalkie.Parameters
import walkietalkie.WalkieTalkie.PathwayInfo
import pvtools.*
import org.bridgedb.bio.*
import org.bridgedb.*
import org.pathvisio.gex.SimpleGex
import cytoscape.*
import cytoscape.data.CyAttributes

File outFile = new File("/home/thomas/data/rowett/guillermo/output")
File dataFile = new File("/home/thomas/data/rowett/guillermo/input/guillermo.pgex")
def pathwayFiles = [
    "KEGG" : new File("/home/thomas/data/rowett/guillermo/pathways/Rno_KEGG_20100917"),
    "WikiPathways" : new File("/home/thomas/data/rowett/guillermo/pathways/wikipathways-analysis-20110426"),
    "Reactome" : new File("/home/thomas/data/rowett/guillermo/pathways/Reactome_20110509"),
    "KEGG, WikiPathways and Reactome" : new File("/home/thomas/data/rowett/guillermo/pathways")
]
String idmRdb = "idmapper-pgdb:/home/thomas/data/bridgedb/Rn_Derby_20101110.bridge"

//Load the ID mapper database
Class.forName("org.bridgedb.rdb.IDMapperRdb")
Class.forName("org.apache.derby.jdbc.EmbeddedDriver")
IDMapper idmapper = new IDMapperStack()
idmapper.addIDMapper(idmRdb)

//Add custom id mapping
//String idmTxt = "idmapper-text:file:///home/thomas/data/rowett/custom_entrez_ens.txt"
//Class.forName("org.bridgedb.file.IDMapperText")
//idmapper.addIDMapper(idmTxt)

//Open the PathVisio dataset
SimpleGex data = PVToolsPlugin.openDataSet(dataFile)

//Additional parameters to WalkieTalkie
Parameters par = Parameters.create()
    .minGeneConnections(0)
    .dataSource(BioDataSource.UNIPROT)

pathwayFiles.each {
    //Load the pathways
    Set<PathwayInfo> pathways = WalkieTalkieUtils.readPathways(it.value)
    //Build the network including all proteins in the dataset
    WalkieTalkie wt = new WalkieTalkie(
        par, null, pathways, idmapper, data
    )
    CyNetwork network = WalkieTalkiePlugin.loadSif(wt, true)
    network.setTitle(it.key)
    //Load the label and type attributes
    WalkieTalkiePlugin.loadAttributes(wt)
}

//Load the expression data
println("Loading attributes...")
def textCols = ["SSP_PDQ", "SSP_SS", "Accession Uniprot", "Uniprot protein name", "KEGG", "Ensembl"]

PVToolsPlugin.loadAttributes(
        data, 
        new AttributeOptions().excludeCols(textCols)
)
PVToolsPlugin.loadTextAttributes(data, textCols)

//Apply layout
import cytoscape.*
import cytoscape.view.*
import cytoscape.layout.*
for(CyNetworkView view in Cytoscape.getNetworkViewMap().values()) {
	CyLayouts.getLayout("Kamada-Kawai-Noweight").doLayout(view);
}

//Add charts for data values
import cytoscape.*
import cytoscape.data.CyAttributes

CyAttributes attr = Cytoscape.getNodeAttributes();

Cytoscape.getCyNodesList().each { node ->
	def labels = [ "DHPG", "HT", "AE" ];
	def values = labels.collect { l ->
		attr.getListAttribute(node.getIdentifier(), l)
	}
	values = GroovyCollections.transpose(values);

	def url = "http://chart.apis.google.com/chart" +
		"?chxr=0,0,1.5|1,0,2&chxt=y,x&chbh=a,0,10&chs=300x300" +
		"&cht=bvg&chds=0,1.5&chf=bg,s,00000000&chco=0000FF,ABABFF" +
		"&chxs=1,676767,30,0,l,676767"
		
	url += "&chd=t:" + values.collect { it.join(",") }.join("|")
	url += "&chxl=1:|" + labels.join("|")
	
	if(values.size() == 0) url = ""
	attr.setAttribute(node.getIdentifier(), "node.customGraphics1", url)
}
