package org.its;

/**
 * A tile source. Classes that implement this interface
 * provide tiles and tile information.
 * @author thomas
 */
public interface TileSource {
	public Tile getTile(String id, int x, int y, int z) throws TilerException;
	public void setTileSize(int size);
	public int getTileSize();
	public void setZoom(int zoom);
	public int getZoom();
	public Tiler getTiler(String id) throws TilerException;
	public String[] getIds();
}
