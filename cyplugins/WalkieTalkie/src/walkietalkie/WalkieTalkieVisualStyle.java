// PathVisio,
// a tool for data visualization and analysis using Biological Pathways
// Copyright 2006-2009 BiGCaT Bioinformatics
//
// Licensed under the Apache License, Version 2.0 (the "License"); 
// you may not use this file except in compliance with the License. 
// You may obtain a copy of the License at 
// 
// http://www.apache.org/licenses/LICENSE-2.0 
//  
// Unless required by applicable law or agreed to in writing, software 
// distributed under the License is distributed on an "AS IS" BASIS, 
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
// See the License for the specific language governing permissions and 
// limitations under the License.
//
package walkietalkie;

import java.awt.Color;

import cytoscape.visual.EdgeAppearanceCalculator;
import cytoscape.visual.NodeAppearanceCalculator;
import cytoscape.visual.NodeShape;
import cytoscape.visual.VisualPropertyType;
import cytoscape.visual.VisualStyle;
import cytoscape.visual.calculators.BasicCalculator;
import cytoscape.visual.mappings.DiscreteMapping;
import cytoscape.visual.mappings.ObjectMapping;
import cytoscape.visual.mappings.PassThroughMapping;

/**
 * Defines a VisualStyle for the WalkieTalkie plugin
 */
public class WalkieTalkieVisualStyle extends VisualStyle {
	public static final String NAME = "WalkieTalkie";
	NodeAppearanceCalculator nac;
	EdgeAppearanceCalculator eac;
	
	public WalkieTalkieVisualStyle(VisualStyle toCopy) {
		super(toCopy);
		setName(NAME);
		init();
	}
	
	public WalkieTalkieVisualStyle() {
		super(NAME);
		init();
	}
	
	private void init() {
		getGlobalAppearanceCalculator().setDefaultBackgroundColor(Color.WHITE);
		
		nac = getNodeAppearanceCalculator();
		eac = getEdgeAppearanceCalculator();
		if(nac == null) {
			nac = new NodeAppearanceCalculator();
			setNodeAppearanceCalculator(nac);
		}
		if(eac == null) {
			eac = new EdgeAppearanceCalculator();
			setEdgeAppearanceCalculator(eac);
		}
		
		setLabelMapping();
		setColorMapping();
		setShapeMapping();
	}
	
	void setShapeMapping() {
		DiscreteMapping shapeMapping = new DiscreteMapping(
				nac.getDefaultAppearance().get(VisualPropertyType.NODE_SHAPE),
				WalkieTalkie.ATTR_TYPE,
				ObjectMapping.NODE_MAPPING
		);
		shapeMapping.putMapValue(WalkieTalkie.TYPE_GENE, NodeShape.DIAMOND);
		shapeMapping.putMapValue(WalkieTalkie.TYPE_PATHWAY, NodeShape.ELLIPSE);
		
		nac.setCalculator(
				new BasicCalculator("Node shape", shapeMapping, VisualPropertyType.NODE_SHAPE)
		);
	}
	
	void setLabelMapping() {
		nac.setCalculator(
				new BasicCalculator(
						WalkieTalkie.ATTR_LABEL,
						new PassThroughMapping("", WalkieTalkie.ATTR_LABEL),
						VisualPropertyType.NODE_LABEL
				)
		);
	}
	
	void setColorMapping() {
		eac.getDefaultAppearance().set(VisualPropertyType.EDGE_COLOR, new Color(153, 153, 153));
		
		DiscreteMapping colorMapping = new DiscreteMapping(
				nac.getDefaultAppearance().get(VisualPropertyType.NODE_FILL_COLOR),
				WalkieTalkie.ATTR_TYPE,
				ObjectMapping.NODE_MAPPING
		);
		colorMapping.putMapValue(WalkieTalkie.TYPE_GENE, new Color(255, 153, 153));
		colorMapping.putMapValue(WalkieTalkie.TYPE_PATHWAY, new Color(153, 153, 255));
		
		nac.setCalculator(
				new BasicCalculator("Node shape", colorMapping, VisualPropertyType.NODE_FILL_COLOR)
		);					
	}
}
