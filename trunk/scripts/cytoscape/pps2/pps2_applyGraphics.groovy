import cytoscape.*;
import cytoscape.view.*;
import cytoscape.layout.*;
import java.awt.Color;
import cytoscape.visual.CalculatorCatalog;
import cytoscape.visual.NodeAppearanceCalculator;
import cytoscape.visual.VisualPropertyType;
import cytoscape.visual.VisualStyle;
import cytoscape.visual.calculators.BasicCalculator;
import cytoscape.visual.mappings.BoundaryRangeValues;
import cytoscape.visual.mappings.ContinuousMapping;
import cytoscape.visual.mappings.ObjectMapping;
import PPSGlobals;

//Create a visual style to color the gene nodes
String attr = PPSGlobals.vizAttr;

VisualStyle vsSource = Cytoscape.getVisualMappingManager().getVisualStyle();
VisualStyle vs = new VisualStyle(vsSource);
vs.setName(vsSource.getName() + "_" + attr);

Color centerColor = Color.WHITE;
double minValue = -2;
Color minColor = Color.GREEN;
double maxValue = 2;
Color maxColor = Color.RED;
NodeAppearanceCalculator nac = vs.getNodeAppearanceCalculator();
ContinuousMapping cm = new ContinuousMapping(nac.getDefaultAppearance().get(
		VisualPropertyType.NODE_FILL_COLOR), ObjectMapping.NODE_MAPPING);
BoundaryRangeValues min = new BoundaryRangeValues(minColor, minColor, minColor);
BoundaryRangeValues center = new BoundaryRangeValues(centerColor, centerColor, centerColor);
BoundaryRangeValues max = new BoundaryRangeValues(maxColor, maxColor, maxColor);

cm.addPoint(minValue, min);
cm.addPoint(0, center);
cm.addPoint(maxValue, max);
cm.setControllingAttributeName(attr, Cytoscape.getCurrentNetwork(), true);
nac.setCalculator(new BasicCalculator(attr, cm, VisualPropertyType.NODE_FILL_COLOR));

CalculatorCatalog cat = Cytoscape.getVisualMappingManager().getCalculatorCatalog();
if(cat.getVisualStyle(vs.getName()) == null) {
	cat.addVisualStyle(vs);
}

for(CyNetworkView view in Cytoscape.getNetworkViewMap().values()) {
	// Apply the visual style
	Cytoscape.getDesktop().setFocus(view.getNetwork().getIdentifier());
	Cytoscape.getVisualMappingManager().setVisualStyle(vs.getName());
}