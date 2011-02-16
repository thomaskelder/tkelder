package org.pct.io;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.pct.model.AttributeKey;
import org.pct.model.Network;

public class XGMMLWriter {
	final static String NS = "http://www.cs.rpi.edu/XGMML";
	
	public static <N, E> void write(Network<N, E> network, PrintWriter out) {
		//Find out the attribute types
		Map<String, Boolean> atypes = new HashMap<String, Boolean>();
		for(String a : network.listNodeAttributes()) {
			atypes.put(a, network.isNumericAttribute(a));
		}
		for(String a : network.listEdgeAttributes()) {
			atypes.put(a, network.isNumericAttribute(a));
		}

		//Open the graph element
		out.println(
			open("graph",
					attr("xmlns", NS) + 
					attr("id", "" + System.currentTimeMillis()) +
					attr("label", network.getTitle())
			)
		);
		
		//Print the graph attributes
		for(Entry<String, String> e : network.getNetworkAttributes().entrySet()) {
			String type = isNumeric(e.getKey()) ? "real" : "string";
			out.println(tag("att", "",
					attr("label", e.getKey()) +
					attr("name", e.getKey()) +
					attr("value", e.getValue()) +
					attr("type", type)
			));
		}
		
		//Create the nodes
		for(N n : network.getGraph().getNodes()) {
			String symbol = network.getNodeAttribute(n, AttributeKey.Label.name());
			
			out.println(open("node", attr("id", "" + n) + attr("label", "" + n)));
			
			out.println(tag("att", "",
					attr("label", "canonicalName") +
					attr("name", "canonicalName") +
					attr("value", symbol == null ? "" : symbol) +
					attr("type", "string")
			));
			
			for(Entry<String, String> e : network.getNodeAttributes(n).entrySet()) {
				String type = atypes.get(e.getKey()) ? "real" : "string";
				out.println(tag("att", "",
						attr("label", e.getKey()) +
						attr("name", e.getKey()) +
						attr("value", e.getValue()) +
						attr("type", type)
				));
			}
			
			out.println(close("node"));
		}
		
		//Create the edges
		for(E edge : network.getGraph().getEdges()) {
			String interaction = network.getEdgeAttribute(edge, AttributeKey.Interaction.name());
			
			N src = network.getGraph().getFirst(edge);
			N tgt = network.getGraph().getSecond(edge);
			out.println(open("edge", 
				attr("id", "" + edge) +
				attr("label", "" + edge) +
				attr("source", "" + src) +
				attr("target", "" + tgt)
			));
			
			out.println(tag("att", "",
					attr("label", "interaction") +
					attr("name", "interaction") +
					attr("value", interaction == null ? "" : interaction) +
					attr("type", "string")
			));
			
			for(Entry<String, String> e : network.getEdgeAttributes(edge).entrySet()) {
				String type = atypes.get(e.getKey()) ? "real" : "string";
				out.println(tag("att", "",
						attr("label", e.getKey()) +
						attr("name", e.getKey()) +
						attr("value", e.getValue()) +
						attr("type", type)
				));
			}
			
			out.println(close("edge"));
		}
		out.println(close("graph"));
	}
	
	private static String attr(String name, String value) {
		return name + "=\"" + encode(value) + "\" ";
	}
	
	private static String open(String name, String attr) {
		return "<" + name + " " + attr + ">";
	}
	
	private static String close(String name) {
		return "</" + name + ">";
	}

	private static String tag(String name, String value, String attr) {
		return "<" + name + " " + attr + ">" + encode(value) + "</" + name + ">";
	}
	
	private static boolean isNumeric(String v) {
		try {
			Double.parseDouble(v);
		} catch(NumberFormatException e) { return false; }
		return true;
	}
	
	//TODO: replace with library
	private static String encode(String str) {
		// Find and replace any "magic", control, non-printable etc. characters
        // For maximum safety, everything other than printable ASCII (0x20 thru 0x7E) is converted into a character entity
        
        StringBuilder sb;
        
        sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char c;

            c = str.charAt(i);
            if ((c < ' ') || (c > '~'))
             {
                sb.append("&#x");
                sb.append(Integer.toHexString((int)c));
                sb.append(";");
            }
            else if (c == '"') {
                sb.append("&quot;");
            }
            else if (c == '\'') {
                sb.append("&apos;");
            }
            else if (c == '&') {
                sb.append("&amp;");
            }
            else if (c == '<') {
                sb.append("&lt;");
            }
            else if (c == '>') {
                sb.append("&gt;");
            }
            else {
                sb.append(c);
            }
        }

		return sb.toString();
	}
}