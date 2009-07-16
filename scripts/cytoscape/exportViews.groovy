import cytoscape.util.export.*;
import cytoscape.*;
import cytoscape.view.*;
import cytoscape.view.CyDesktopManager;
import cytoscape.view.CyDesktopManager.Arrange;

String path = "/home/thomas/projects/pps1/path_results/pathvisio-z/network/images/";
String ext = "png";

BitmapExporter exporter = new BitmapExporter(ext, 5);
for(CyNetworkView v in Cytoscape.getNetworkViewMap().values()) {
    //Arrange
    CyDesktopManager.arrangeFrames(Arrange.CASCADE);

    //Set zoom to fit
    v.fitContent();

    //Export
    f = new File(path, v.getTitle() + "." + ext);
    exporter.export(v, new FileOutputStream(f)); 
}