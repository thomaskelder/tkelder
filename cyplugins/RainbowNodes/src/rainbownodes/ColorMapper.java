package rainbownodes;

import java.awt.Color;

import javax.swing.JPanel;

/**
 * Class that maps values to colors, such as a gradient.
 * @author thomas
 */
public interface ColorMapper {
	public Color calculate(Object value);
	
	public JPanel createLegend();
}
