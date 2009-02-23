var serverUrl;
var image;

//Avoid pink tiles
OpenLayers.IMAGE_RELOAD_ATTEMPTS = 3;

function init() {
	//Query the available maps
	setServer();
}

function setServer() {
	serverUrl = OpenLayers.Util.getElement('servertext').value;
	OpenLayers.loadURL(serverUrl, null, this, parseServerXml);
}

/*
<TileMapService services="http://localhost:8180/SvgTileServer/TileServer/" version="1.0.0">
<Title>SvgTileServer</Title>
<Abstract>SvgTileServer</Abstract>
<TileMaps>
<TileMap href="http://localhost:8180/SvgTileServer/TileServer/1.0.0/WP140.svg" profile="local" title="WP140.svg"/>
</TileMaps>
</TileMapService>
 */
function parseServerXml(request) {
	var xml = OpenLayers.parseXMLString(request.responseText);
	var mapNodes = xml.getElementsByTagName("TileMap");

	var selectImageDiv = OpenLayers.Util.getElement('images');
	selectImageDiv.innerHTML = ""; //Clear old content

	var select = document.createElement("SELECT");
	select.id = "selectimage";
	selectImageDiv.appendChild(select);

	var mapTitles = [];
	for(var i = 0; i < mapNodes.length; i++) { 
		mapTitles[i] = mapNodes[i].getAttribute("title");
	}
	mapTitles.sort(function(x,y){ 
		var a = String(x).toUpperCase(); 
		var b = String(y).toUpperCase(); 
		if (a > b) 
			return 1 
			if (a < b) 
				return -1 
				return 0; 
	});

	for(var j = 0; j < mapTitles.length; j++) {
		//Create a select element
		var option = document.createElement("OPTION");
		if(j == 0) option.SELECTED = "1";
		option.value = mapTitles[j];
		option.innerHTML = mapTitles[j];
		select.appendChild(option);
	}
	select.onchange = setImage;

	if(mapNodes.length == 0) {
		window.alert("No images found on tile server");
	} else {
		setImage();
	}
}

function setImage() {
	var select = document.getElementById("selectimage");
	image = select.value;
	var url = serverUrl + "/" + image;
	OpenLayers.loadURL(url, null, this, parseImageXml);
}

function parseImageXml(request) {
	var xml = OpenLayers.parseXMLString(request.responseText);

	//Parse resolution / zoom
	var tsElms = xml.getElementsByTagName("TileSet");
	var maxResolution = 1.0;
	var minResolution = 10000.0;
	for(var i = 0; i < tsElms.length; i++) {
		var res = parseFloat(tsElms[i].getAttribute("units-per-pixel"));
		if(res > maxResolution) {
			maxResolution = res;
		}
		if(res < minResolution) {
			minResolution = res;
		}
	}
	var nrZoom = tsElms.length;

	//Parse bounds
	var boundElm = xml.getElementsByTagName("BoundingBox")[0];
	var bounds = new OpenLayers.Bounds(
			boundElm.getAttribute("minx"), boundElm.getAttribute("miny"),
			boundElm.getAttribute("maxx"), boundElm.getAttribute("maxy")
	);
	var origin = new OpenLayers.Pixel(
			boundElm.getAttribute("minx"), boundElm.getAttribute("miny")	
	);
	//Parse tile format and size
	var formElm = xml.getElementsByTagName("TileFormat")[0];
	var ext = formElm.getAttribute("extension");
	var tileWidth = formElm.getAttribute("width");
	var tileHeight = formElm.getAttribute("height");

	var options = {
			tileSize: new OpenLayers.Size(tileWidth, tileHeight),
			maxExtent: bounds,
			maxResolution: maxResolution,
			minResolution: minResolution,
			numZoomLevels: nrZoom,
			tileOrigin: origin
	}

	OpenLayers.Util.getElement('map').innerHTML = "";
	var map = new OpenLayers.Map( 'map', options);

	var layer = new OpenLayers.Layer.TMS( "TMS",
			serverUrl, {layername: image, type: ext, serviceVersion:""} );

	map.addLayer(layer);
	map.setCenter(bounds.getCenterLonLat(), 0);

	map.removeControl (map.controls [1]);
	var navcontrol = new OpenLayers.Control.PanZoomBar()
	map.addControl (navcontrol);

	var loadingpanel = new OpenLayers.Control.LoadingPanel();
	map.addControl(loadingpanel);
}