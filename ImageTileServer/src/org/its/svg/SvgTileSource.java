package org.its.svg;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.batik.dom.util.DOMUtilities;
import org.its.Tile;
import org.its.TileSource;
import org.its.Tiler;
import org.its.TilerException;
import org.its.util.Logger;
import org.its.util.Xml;
import org.w3c.dom.Document;

public class SvgTileSource implements TileSource {
	private static final int MAX_SVG_MEM = 5; //Maximum svg documents to keep in memory
	int tileSize;
	int zoom;
	File svgPath;
	
	Map<String, Document> svgDocuments;
	
	public SvgTileSource(File svgPath) {
		this.svgPath = svgPath;
		svgDocuments = Collections.synchronizedMap(new HashMap<String, Document>());
	}
	
	public void setZoom(int zoom) {
		this.zoom = zoom;
	}
	
	public int getZoom() {
		return zoom;
	}
	
	public void setTileSize(int size) {
		tileSize = size;
	}
	
	public int getTileSize() {
		return tileSize;
	}
	
	public Tile getTile(String id, int x, int y, int z) throws TilerException {
		//Id is the svg file, read it
		try {
			return getTiler(id).getTile(x, y, z);
		} catch (Exception e) {
			throw new TilerException(e);
		}
		
	}

	public String[] getIds() {
		Set<String> files = new HashSet<String>();
		findFiles(svgPath, files);
		return files.toArray(new String[files.size()]);
	}
	
	/**
	 * Finds all top level files in the given directory
	 * and adds them to the set.
	 */
	private void findFiles(File base, Set<String> files) {
		File[] list = base.listFiles();
		if(list == null) return;
		
		for(File svg : list) {
			if(!svg.isDirectory()) {
				files.add(toId(svg));
			} else {
				findFiles(svg, files);
			}
		}
	}
	
	private static final String PATH_REPLACE = ",";
	
	private String toId(File svg) {
		String subPath = svg.getAbsolutePath().substring(svgPath.getAbsolutePath().length() + 1);
		return subPath.replaceAll(File.separator, PATH_REPLACE);
	}
	
	private File fromId(String id) {
		return new File(svgPath + "/" + id.replaceAll(PATH_REPLACE, "/"));
	}
	
	public Tiler getTiler(String id) throws TilerException {
		//Id is the svg file, read it
		try {
			Document svg = svgDocuments.get(id);
			if(svg == null) {
				if(svgDocuments.size() > MAX_SVG_MEM) {
					//Make room for the new document
					svgDocuments.remove(svgDocuments.keySet().iterator().next());
				}
				Logger.log.trace("Loading SVG document " + id);
				svg = Xml.readSvg(fromId(id));
				svgDocuments.put(id, svg);
			} else {
				Logger.log.trace("Using cached SVG document");
			}
			//Clone the reference svg, because batik is not thread save
			//see: https://issues.apache.org/bugzilla/show_bug.cgi?id=46360
			svg = DOMUtilities.deepCloneDocument(svg, svg.getImplementation());
			return new SvgTiler(svg, tileSize, zoom);
		} catch (Exception e) {
			throw new TilerException(e);
		}
	}
}
