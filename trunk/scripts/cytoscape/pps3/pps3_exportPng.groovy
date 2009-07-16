import cytoscape.*;
import cytoscape.view.*;
import cytoscape.view.CyDesktopManager.Arrange;
import cytoscape.layout.*;
import cytoscape.data.CyAttributes;
import cytoscape.util.export.*;
import java.awt.Color;
import PPSGlobals;

//Export all views to an image
String ext = "png";
BitmapExporter exporter = new BitmapExporter(ext, 10);
//Arrange
CyDesktopManager.arrangeFrames(Arrange.CASCADE);

for(CyNetworkView v in Cytoscape.getNetworkViewMap().values()) {
    //Set zoom to fit
    v.fitContent();

    //Export
    File f = new File(PPSGlobals.outPath, v.getTitle() + "." + ext);
    exporter.export(v, new FileOutputStream(f)); 
}