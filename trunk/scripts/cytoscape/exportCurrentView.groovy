import cytoscape.util.export.*;
import cytoscape.*;
import cytoscape.view.*;
import cytoscape.view.CyDesktopManager;
import cytoscape.view.CyDesktopManager.Arrange;

String path = "/home/thomas/projects/pps1/path_results/pathvisio-z/network/images/";
String ext = "png";

BitmapExporter exporter = new BitmapExporter(ext, 5);

CyNetworkView v = Cytoscape.getCurrentNetworkView();
//Export
f = new File(path, v.getTitle() + "." + ext);
exporter.export(v, new FileOutputStream(f));