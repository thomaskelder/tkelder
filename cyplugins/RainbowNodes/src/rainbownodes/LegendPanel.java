package rainbownodes;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import cytoscape.Cytoscape;

public class LegendPanel extends JPanel {
	private RainbowNodesPlugin plugin;
	
	public LegendPanel(RainbowNodesPlugin plugin) {
		super();
		this.plugin = plugin;
		
		reset();
	}
	
	public void reset() {
		setLayout(new BorderLayout());
		removeAll();
		
		JPanel view = new JPanel(new BorderLayout());
		JScrollPane sp = new JScrollPane(view);
		
		String vid = Cytoscape.getCurrentNetworkView().getIdentifier();
		for(Graphic g : plugin.getGraphicsManager().getRegisteredGraphics().get(vid)) {
			view.add(g.createLegend(), BorderLayout.CENTER);
		}
		
		add(sp, BorderLayout.CENTER);
	}
}
