package org.wikipathways.client;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.EventListener;

public class SvgModel {
	String svgId;
	double zoomFactor = 1;
	
	double widthOrig;
	double heightOrig;
	
	public SvgModel(String svgId) {
		this.svgId = svgId;
		widthOrig = getWidth();
		heightOrig = getHeight();
		Log.firebug(widthOrig + ", " + heightOrig);
		setViewBox(0, 0, widthOrig, heightOrig);
	}
	
	native Element getSvgObject(String id)/*-{
		return $doc.getElementById(id);
	}-*/;
	
	native Element getSvgNode(String id)/*-{
		var cdoc = $doc.getElementById(id)['contentDocument'];
		return cdoc.documentElement;
	}-*/;
	
	public void addEventListener(EventListener listener) {
		DOM.setEventListener(getSvgObject(svgId), listener);
	}
	
	public double getHeight() {
		String svgValue = getSvgNode(svgId).getAttribute("height");
		svgValue = svgValue.replaceAll("px", "");
		return Double.parseDouble(svgValue);
		//return getSvgObject(svgId).getOffsetHeight();
	}
	
	public double getWidth() {
		String svgValue = getSvgNode(svgId).getAttribute("width");
		svgValue = svgValue.replaceAll("px", "");
		return Double.parseDouble(svgValue);
		//return getSvgObject(svgId).getOffsetWidth();
	}
	
	public void setZoomFactor(double zoomFactor) {
		this.zoomFactor = zoomFactor;
		Element svgObj = getSvgObject(svgId);
		svgObj.setAttribute("width", widthOrig * zoomFactor + "");
		svgObj.setAttribute("height", heightOrig * zoomFactor + "");
		Element svgNode = getSvgNode(svgId);
		svgNode.setAttribute("width", widthOrig * zoomFactor + "px");
		svgNode.setAttribute("height", heightOrig * zoomFactor + "px");
	}
	
	public double getZoomFactor() {
		return zoomFactor;
	}
	
	public void zoomToFit(double fitWidth, double fitHeight) {
		double factorX = fitWidth / widthOrig;
		double factorY = fitHeight / heightOrig;
		setZoomFactor(Math.min(factorX, factorY));
	}
	
	public void setViewBox(double x, double y, double w, double h) {
		getSvgNode(svgId).setAttribute("viewBox", x + " " + y + " " + w + " " + h);
	}
}
