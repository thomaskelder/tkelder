package rainbownodes;

import java.awt.Color;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Maps categorical attributes (discrete value to color mapping).
 * @author thomas
 *
 */
public class CategoryMapper implements ColorMapper, Serializable {
	Random rnd = new Random();

	Map<Object, Color> colors = new HashMap<Object, Color>();

	public CategoryMapper setColor(Object value, Color color) {
		colors.put(value, color);
		return this;
	}

	public CategoryMapper generateColors(Collection<Object> values) {
		return generateColors(values, 255);
	}
	
	public CategoryMapper generateColors(Collection<Object> values, int alpha) {
		for(Object v : values) {
			setColor(v, randomColor(alpha));
		}
		return this;
	}

	private Color randomColor(int alpha) {
		int rgb = java.awt.Color.HSBtoRGB(rnd.nextFloat(), 1, 1);
		Color c = new Color(rgb);
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
	}

	public void reset() {
		colors.clear();
	}

	public Color calculate(Object value) {
		return colors.get(value);
	};

	public JPanel createLegend() {
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("fill:pref:grow"));
		
		for(Entry<Object, Color> entry : colors.entrySet()) {
			JLabel lbl = new JLabel("" + entry.getKey());
			lbl.setOpaque(true);
			lbl.setBackground(entry.getValue());
			builder.append(lbl);
		}
		JPanel p = builder.getPanel();
		p.setOpaque(true);
		p.setBackground(Color.WHITE);
		return p;
	}
}
