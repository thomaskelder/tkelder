package org.its.svg;

import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.its.Tile;
import org.its.Tiler;
import org.its.TilerException;
import org.its.TilerInfo;
import org.its.util.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SvgTiler implements Tiler {
	private int tileSize;

	private Document svg;
	private int size;

	private int zoom;

	public SvgTiler(Document svg, int tileSize, int zoom) {
		this.svg = svg;
		this.tileSize = tileSize;
		this.zoom = zoom;
		prepareDocument();
	}

	private void prepareDocument() {
		//Make the image a square
		Element root = svg.getDocumentElement();
		String wStr = root.getAttributeNS(null, "width").replaceAll("px", "");
		String hStr = root.getAttributeNS(null, "height").replaceAll("px", "");
		double svgWidth = Double.parseDouble(wStr);
		double svgHeight = Double.parseDouble(hStr);
		size = (int)Math.max(svgWidth, svgHeight);
		root.setAttributeNS(null, "width", size + "");
		root.setAttributeNS(null, "height", size + "");
	}

	public TilerInfo getTileInfo() {
		//Assumes square image
		double[] resolutions = new double[getNrLevels()];
		for(int i = 0; i < resolutions.length; i++) {
			resolutions[i] = getResolution(i);
		}
		return new TilerInfo(resolutions, new Rectangle(0, 0, size, size));
	}

	private int getNrTiles(double resolution) {
		return (int)Math.ceil(((double)size / resolution) / tileSize);
	}
	
	private double getResolution(int level) {
		return Math.pow(2, getNrLevels() - zoom - 1)/Math.pow(2, level);
	}
	
	private int getNrLevels() {
		return (int)Math.ceil((Math.log(getNrTiles(1)) / Math.log(2))) + zoom;
	}

	public String getTileName(int level, int x, int y) {
		return level + "/" + x + "/" + y + ".png";
	}

	public Tile getTile(int x, int y, int z) throws TilerException {
		Logger.log.trace("Creating tile for level: " + z + ", x: " + x + ", y: " + y);
		double res = getResolution(z);
		int tiles = getNrTiles(res);
		Logger.log.trace("Size: " + size);
		Logger.log.trace("Resolution: " + res);
		Logger.log.trace("Number tiles: " + tiles);
		int absX = x * (size / tiles);
		int absY = (tiles - 1 - y) * (size / tiles);
		int absSize = size / tiles;

		if(absX > size || absY > size) {
			throw new TilerException("Tile " + z + ", " + x + ", " + y + " out of image bounds");
		}
		try {
			return new Tile(x, y, z, cutTile(absX, absY, absSize, absSize));
		} catch(Exception e) {
			throw new TilerException(e);
		}
	}

	private byte[] cutTile(int x, int y, int width, int height) throws IOException, TranscoderException {
		Rectangle area = new Rectangle(x, y, width, height);
		Transcoder transcoder = new PNGTranscoder();
		transcoder.addTranscodingHint(ImageTranscoder.KEY_AOI, area);
		transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, (float)tileSize);
		transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, (float)tileSize);

		TranscoderInput input = new TranscoderInput(svg);
		// Create the transcoder output.
		ByteArrayOutputStream ostream = new ByteArrayOutputStream();
		TranscoderOutput output = new TranscoderOutput(ostream);
		transcoder.transcode(input, output);
		return ostream.toByteArray();
	}

	public void setTileSize(int tileSize) {
		this.tileSize = tileSize;
	}

	public int getTileSize() {
		return tileSize;
	}

	public int getZoom() {
		return zoom;
	}
	
	public void setZoom(int zoom) {
		this.zoom = zoom;
	}
}
