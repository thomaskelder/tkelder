package org.its;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.its.util.FileUtils;

/**
 * Wraps a TileSource into an implementation that caches
 * the generated tiles to the file system.
 * @author thomas
 */
public class CachedTileSource implements TileSource {
	TileSource source;
	File tilePath;
	
	public CachedTileSource(TileSource source, File tilePath) {
		this.source = source;
		this.tilePath = tilePath;
	}
	
	public void setTileSize(int size) {
		source.setTileSize(size);
	}
	
	public int getTileSize() {
		return source.getTileSize();
	}
	
	public int getZoom() {
		return source.getZoom();
	}
	
	public void setZoom(int zoom) {
		source.setZoom(zoom);
	}
	
	public String[] getIds() {
		return source.getIds();
	}
	
	public Tiler getTiler(String id) throws TilerException {
		return source.getTiler(id);
	}
	
	public Tile getTile(String id, int x, int y, int z) throws TilerException {
		//First look for the cached tile file
		File cache = new File(tilePath.getAbsolutePath() + 
				"/" + id + "/" + z + "/" + x + "/" + y + ".png");
		Tile tile = null;
		if(!cache.exists()) {
			//If not exists, create it
			tile = source.getTile(id, x, y, z);
			try {
				cache.getParentFile().mkdirs();
				writeCache(cache, tile.getData());
			} catch (IOException e) {
				throw new TilerException(e);
			}
		} else {
			try {
				tile = new Tile(x, y, z, FileUtils.readFile(cache));
			} catch (IOException e) {
				throw new TilerException(e);
			}
		}
		return tile;
	}
	
	private void writeCache(File f, byte[] data) throws IOException {
		f.getParentFile().mkdirs();
		OutputStream out = new BufferedOutputStream(
			new FileOutputStream(f)
		);
		out.write(data);
		out.flush();
		out.close();
	}
}
