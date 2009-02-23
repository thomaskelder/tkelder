package org.its;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.its.util.Logger;
import org.its.util.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A TMS server for arbitrary images.
 * @author thomas
 */
public abstract class TileServer extends HttpServlet {
	protected abstract TileSource getTileSource();
	
	protected int getZoom() {
		String z = getInitParameter("zoom");
		if(z == null) {
			return 1;
		} else {
			return Integer.parseInt(z);
		}
	}
	
	protected int getTileSize() {
		String ts = getInitParameter("tile-size");
		if(ts == null) {
			return 256;
		} else {
			return Integer.parseInt(ts);
		}
	}
	
	/*
	 * http://tileserver/1.0.0/path_to_image.svg/z/x/y.png 
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			String pathInfo = req.getPathInfo();
			Logger.log.trace("Processing request " + pathInfo);
			
			if(pathInfo == null) pathInfo = "";

			String[] infoArray = new String[0];
			//Replace double slashes
			pathInfo.replaceAll("//", "/");
			//Remove leading and trailing slashes
			if(pathInfo.startsWith("/")) {
				pathInfo = pathInfo.substring(1);
			}
			if(pathInfo.endsWith("/")) {
				pathInfo = pathInfo.substring(0, pathInfo.length() - 1);
			}
			infoArray = pathInfo.split("/");

			switch(infoArray.length) {
			case 0:
				resp.getWriter().append(getRoot(req));
				break;
			case 1:
				if(pathInfo.equals(TMS_VERSION)) {
					resp.getWriter().append(getTms(req));
				} else {
					resp.getWriter().append(getRoot(req));
				}
				break;
			case 2:
				resp.getWriter().append(getTileMap(req, infoArray[1]));
				break;
			case 5:
				String title = infoArray[1];
				int level = Integer.parseInt(infoArray[2]);
				int x = Integer.parseInt(infoArray[3]);
				int y = Integer.parseInt(infoArray[4].replace(".png", ""));
				Tile tile = getTileSource().getTile(title, x, y, level);
			    resp.setContentType("image/png");
			    resp.addHeader("Cache-Control", "max-age=86400, must-revalidate");
			    resp.getOutputStream().write(tile.getData()); 
				break;
				
			}
		} catch(Exception e) {
			Logger.log.error(e);
			resp.sendError(500, error(e));
		}
	}
	
	private String error(Throwable e) {
		/*
		 <?xml version="1.0" ?>
		 <TileMapServerError>
		   <Message>The requested tile is outside the bounding box of the tile map.</Message>
		 </TileMapServerError>
		 */
		String xml = "<?xml version=\"1.0\" ?><TileMapServerError><Message>";
		xml += e.getClass().toString() + ": " + e.getMessage();
		xml += "</Message></TileMapServerError>";
		return xml;
	}
	/**
	 * Returns the root resource description, containing a 
	 * TileMapService for each SVG file.
	 * @return The xml message 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws ParserConfigurationException 
	 * @throws TransformerException 
	 */
	private String getRoot(HttpServletRequest req) throws FileNotFoundException, IOException, ParserConfigurationException, TransformerException {
		/*
			<?xml version="1.0" ?>
			 <Services>
			   <TileMapService title="Example Static Tile Map Service" version="1.0.0" href="http://www.osgeo.org/services/tilemapservice.xml" />
			 </Services>
		 */
		//Create the response document
		Document doc = Xml.newDocument();
		Element root = doc.createElement("Services");
		doc.appendChild(root);
		
		Element service = doc.createElement("TileMapService");
		service.setAttribute("title", getServiceTitle());
		service.setAttribute("version", TMS_VERSION);
		service.setAttribute("href", getServiceUrl(req));
		root.appendChild(service);
		
		return Xml.toString(doc);
	}
	
	/**
	 * Returns the TileMapService description
	 * @return
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws TransformerException 
	 */
	private String getTms(HttpServletRequest req) throws ParserConfigurationException, FileNotFoundException, IOException, TransformerException {
		/*
		 <?xml version="1.0" encoding="UTF-8" ?>
		 <TileMapService version="1.0.0" services="http://www.osgeo.org/services/root.xml">
		   <Title>Example Static Tile Map Service</Title>
		   <Abstract>This is a longer description of the static tiling map service.</Abstract>
		   <TileMaps>
		     <TileMap 
		       title="Vancouver Island Base Map" 
		       srs="EPSG:26910" 
		       profile="none" 
		       href="http://www.osgeo.org/services/basemap.xml" />
		   </TileMaps>
		 </TileMapService>
		 */
		Document doc = Xml.newDocument();
		Element root = doc.createElement("TileMapService");
		doc.appendChild(root);
		
		root.setAttribute("version", TMS_VERSION);
		root.setAttribute("services", getRootUrl(req));
		
		Element title = doc.createElement("Title");
		title.setTextContent(getServiceTitle());
		root.appendChild(title);
		Element abstr = doc.createElement("Abstract");
		abstr.setTextContent(getServiceTitle());
		root.appendChild(abstr);
		
		Element tileMaps = doc.createElement("TileMaps");
		
		//Find the available images
		String[] ids = getTileSource().getIds();
		
		//Create a service for each id
		for(String id : ids) {
			Element map = doc.createElement("TileMap");
			map.setAttribute("title", id);
			map.setAttribute("href", getMapUrl(req, id));
			map.setAttribute("profile", PROFILE);
			tileMaps.appendChild(map);
		}
		
		root.appendChild(tileMaps);
		return Xml.toString(doc);
	}
	
	private String getTileMap(HttpServletRequest req, String title) throws FileNotFoundException, TilerException, IOException, ParserConfigurationException, TransformerException {
		/*
		  <?xml version="1.0" encoding="UTF-8" ?>  
		  <TileMap version="1.0.0" tilemapservice="http://http://tms.osgeo.org/1.0.0">
		   <Title>VMAP0 World Map</Title>
		   <Abstract>A map of the world built from the NGA VMAP0 vector data set.</Abstract>
		   <SRS>EPSG:4326</SRS>
		   <BoundingBox minx="-180" miny="-90" maxx="180" maxy="90" />
		   <Origin x="-180" y="-90" />  
		   <TileFormat width="256" height="256" mime-type="image/jpeg" extension="jpg" />
		   <TileSets profile=global-geodetic">
		     <TileSet href="http://tms.osgeo.org/1.0.0/vmap0/0" units-per-pixel="0.703125" order="0" />
		     <TileSet href="http://tms.osgeo.org/1.0.0/vmap0/1" units-per-pixel="0.3515625" order="1" />
		     <TileSet href="http://tms.osgeo.org/1.0.0/vmap0/2" units-per-pixel="0.17578125" order="2" />
		     <TileSet href="http://tms.osgeo.org/1.0.0/vmap0/3" units-per-pixel="0.08789063" order="3" />
		   </TileSets>
		 </TileMap>
		 */
		Document doc = Xml.newDocument();
		Element root = doc.createElement("TileMap");
		root.setAttribute("version", TMS_VERSION);
		root.setAttribute("tilemapservice", getServiceUrl(req));
		doc.appendChild(root);
		Element titleElm = doc.createElement("Title");
		titleElm.setTextContent(title);
		root.appendChild(titleElm);
		Element abstr = doc.createElement("Abstract");
		abstr.setTextContent(title);
		root.appendChild(abstr);
		Element srs = doc.createElement("SRS");
		root.appendChild(srs);
		TilerInfo info = getTileSource().getTiler(title).getTileInfo();
		
		Element bound = doc.createElement("BoundingBox");
		bound.setAttribute("minx", "" + info.getBounds().x);
		bound.setAttribute("miny", "" + info.getBounds().y);
		bound.setAttribute("maxx", "" + (info.getBounds().x + info.getBounds().width));
		bound.setAttribute("maxy", "" + (info.getBounds().y + info.getBounds().height));
		root.appendChild(bound);
		Element origin = doc.createElement("Origin");
		origin.setAttribute("x", "" + info.getBounds().x);
		origin.setAttribute("y", "" + info.getBounds().y);
		root.appendChild(origin);
		Element tileformat = doc.createElement("TileFormat");
		tileformat.setAttribute("width", "" + getTileSize());
		tileformat.setAttribute("height", "" + getTileSize());
		tileformat.setAttribute("mime-type", "image/png");
		tileformat.setAttribute("extension", "png");
		root.appendChild(tileformat);
		
		Element tilesets = doc.createElement("TileSets");
		tilesets.setAttribute("profile", PROFILE);
		root.appendChild(tilesets);
		
		for(int i = 0; i < info.getResolutions().length; i++) {
			Element ts = doc.createElement("TileSet");
			ts.setAttribute("href", getMapUrl(req, title) + "/" + i);
			ts.setAttribute("units-per-pixel", "" + info.getResolutions()[i]);
			ts.setAttribute("order", "" + i);
			tilesets.appendChild(ts);
		}
		return Xml.toString(doc);
	}

	protected String getMapUrl(HttpServletRequest req, String title) {
		return getServiceUrl(req) + "/" + title;
	}
	
	protected String getRootUrl(HttpServletRequest req) {
		return req.getScheme() + "://" + req.getServerName() + 
			":" + req.getServerPort() + req.getContextPath() + 
			req.getServletPath() + "/";
	}
	
	protected String getServiceTitle() {
		return getServletName();
	}
	
	private String getServiceUrl(HttpServletRequest req) {
		return getRootUrl(req) + TMS_VERSION;
	}
	
	public static final String TMS_VERSION = "1.0.0";
	static final String PROFILE = "local";
}
