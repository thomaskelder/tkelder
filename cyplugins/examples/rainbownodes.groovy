import java.awt.Color;
import java.util.List;
import cytoscape.Cytoscape;
import cytoscape.view.CyNetworkView;
import rainbownodes.*;

//!! Before running this example, please run walkietalkie.groovy and pvtools.groovy first !!
/*
This example shows you how to visualize attributes using bar graphics.
*/

//Create the graphics
List<String> attributes = [
    "t1",
    "t2",
    "t3"
];
Graphic barGraph = new BarGraphic(
        attributes, new Gradient()
            .point(-3, Color.GREEN)
            .point(0, Color.YELLOW)
            .point(3, Color.RED)
);

CyNetworkView view = Cytoscape.getCurrentNetworkView();

//Apply the graphics
GraphicsManager.getInstance().addGraphic(barGraph, view);
