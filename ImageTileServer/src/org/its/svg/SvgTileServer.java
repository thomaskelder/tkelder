package org.its.svg;

import java.io.File;

import javax.servlet.ServletException;

import org.its.CachedTileSource;
import org.its.TileServer;
import org.its.TileSource;

public class SvgTileServer extends TileServer {
	private TileSource tileSource;
	
	protected TileSource getTileSource() {
		return tileSource;
	}
	
	private File getImagePath() {
		String p = getInitParameter("image-path");
		if(p != null) {
			return new File(p);
		} else {
			return new File(getServletContext().getRealPath(".") + "/" + "images/");
		}
	}
	
	private File getTilePath() {
		String p = getInitParameter("tile-path");
		if(p != null) {
			return new File(p);
		} else {
			return new File(getServletContext().getRealPath(".") + "/" + "tiles/");
		}
	}
	
	public void init() throws ServletException {
		tileSource = new CachedTileSource(
				new SvgTileSource(getImagePath()),
				getTilePath()
		);
		tileSource.setTileSize(getTileSize());
		tileSource.setZoom(getZoom());
	}
}
