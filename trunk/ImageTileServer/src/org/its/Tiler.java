package org.its;

/**
 * Responsible for cutting tiles from an image. Each image
 * has its own Tiler instance.
 * @author thomas
 */
public interface Tiler {
	public Tile getTile(int x, int y, int z) throws TilerException;
	public TilerInfo getTileInfo();
	public void setTileSize(int size);
	public int getTileSize();
	public void setZoom(int zoom);
	public int getZoom();
}