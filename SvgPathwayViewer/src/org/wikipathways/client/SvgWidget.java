package org.wikipathways.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Widget;

public class SvgWidget extends Widget {
	String svgRef;
	String svgId;
	SvgModel svgModel;
	List<SvgLoadListener> listeners = new ArrayList<SvgLoadListener>();
	
	public SvgWidget(String svgRef) {
		this.svgRef = svgRef;
		svgId = System.currentTimeMillis() + "";
		Element div = DOM.createDiv();
		setElement(div);
	}

	public String getSvgId() {
		return svgId;
	}

	public SvgModel getSvgModel() {
		if(svgModel == null) throw new IllegalArgumentException("SVG is not yet loaded");
		return svgModel;
	}

	public void addLoadListener(SvgLoadListener l) {
		if(!listeners.contains(l)) listeners.add(l);
	}
	
	public void removeLoadListener(SvgLoadListener l) {
		listeners.remove(l);
	}
	
	@SuppressWarnings("unused") //Called from jsni
	private void svgLoaded() {
		Log.firebug("SVG Loaded, calling listeners");
		svgModel = new SvgModel(svgId);
		for(SvgLoadListener l : listeners) {
			l.onSvgLoaded(this);
		}
		
		svgModel.addEventListener(new EventListener() {
			public void onBrowserEvent(Event event) {
				Log.firebug("Clicked at: " + event.getClientX() + ", " + event.getClientY());
			}
		});
	}
	
	protected void onAttach() {
		super.onAttach();
		Element svgElement = createObject(svgRef, svgId);
		svgElement.setId(svgId);
		svgElement.setAttribute("type", "image/svg+xml");
		svgElement.setAttribute("data", svgRef);
		svgElement.setAttribute("width", "100%");
		svgElement.setAttribute("height", "100%");
		addLoadListener(this, svgElement);
		attachObject(svgElement, getElement());
	}

	private native void addLoadListener(SvgWidget x, Element elm) /*-{
		elm.addEventListener("load", function() {
			x.@org.wikipathways.client.SvgWidget::svgLoaded()();
		}, false);
	}-*/;

	private int suspendId = Integer.MIN_VALUE;
	
	private void suspendRedraw() {
		if(suspendId != Integer.MIN_VALUE) {
			suspendId = suspendRedrawImpl(getSvgModel().getSvgNode(svgId), 5000);
		}
	}
	
	private void unsuspendRedraw() {
		unsuspendRedrawImpl(getSvgModel().getSvgNode(svgId), suspendId);
	}
	
	private native int suspendRedrawImpl(Element svg, int milliseconds) /*-{
		return svg.suspendRedraw(milliseconds);
	}-*/;
	
	private native void unsuspendRedrawImpl(Element svg, int id) /*-{
		svg.unsuspendRedraw(id);
	}-*/;
	
	private native void attachObject(Element object, Element parent)/*-{
		var sw = $wnd['svgweb'];
		sw.appendChild(object, parent);
	}-*/;

	private native Element createObject(String svgRef, String svgId)/*-{
		return $doc.createElement('object', true);
	}-*/;

	public String getSvgRef() {
		return svgRef;
	}
}