package org.wikipathways.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.widgetideas.client.SliderBar;
import com.google.gwt.widgetideas.client.SliderBar.LabelFormatter;

public class SvgBrowser extends Composite implements SvgLoadListener {
	SvgWidget svgWidget;
	int zoomStep = 10;
	ScrollPanel scroll;
	SliderBar zoomSlider;
	DockPanel dockPanel;
	PopupPanel loadingPanel;
	
	boolean zoomIsDragging = false;
	
	public SvgBrowser(String svgRef) {
		dockPanel = new DockPanel();
		svgWidget = new SvgWidget(svgRef);
		svgWidget.addLoadListener(this);
		scroll = new ScrollPanel(svgWidget);
		dockPanel.add(scroll, DockPanel.CENTER);
		
		HorizontalPanel buttonPanel = new HorizontalPanel();
		zoomSlider = new SliderBar(10, 200, new LabelFormatter() {
			public String formatLabel(SliderBar slider, double value) {
				return (int)value + "%";
			}
		});
		buttonPanel.add(zoomSlider);
		
		zoomSlider.setStepSize(zoomStep);
		zoomSlider.setWidth("10em");
		zoomSlider.setNumTicks(4);
		zoomSlider.setNumLabels(2);
		zoomSlider.setCurrentValue(100);
		
		zoomSlider.addMouseDownHandler(new MouseDownHandler() {
			public void onMouseDown(MouseDownEvent event) {
				zoomIsDragging = true;
			}
		});
		zoomSlider.addMouseUpHandler(new MouseUpHandler() {
			double prevValue = 100;
			public void onMouseUp(MouseUpEvent event) {
				zoomIsDragging = false;
				if(zoomSlider.getCurrentValue() != prevValue) {
					prevValue = zoomSlider.getCurrentValue();
					zoom(prevValue);
				}
			}
		});
		zoomSlider.addChangeListener(new ChangeListener() {
			public void onChange(Widget sender) {
				if(!zoomIsDragging) {
					zoom(zoomSlider.getCurrentValue());
				}
			}
		});
		
		Button zoomFit = new Button("Fit");
		buttonPanel.add(zoomFit);
		
		zoomFit.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				zoomFit();
			} 
		});
		
		dockPanel.add(buttonPanel, DockPanel.NORTH);
		dockPanel.setCellHorizontalAlignment(buttonPanel, HasHorizontalAlignment.ALIGN_CENTER);
		dockPanel.setCellHorizontalAlignment(scroll, HasHorizontalAlignment.ALIGN_CENTER);
		initWidget(dockPanel);
	}
	
	void showLoading() {
		Log.firebug("Showing loading panel");
		if(loadingPanel == null) {
			loadingPanel = new PopupPanel(false, true);
			VerticalPanel p = new VerticalPanel();
			p.setSize("100%", "100%");
			p.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
			p.setVerticalAlignment(VerticalPanel.ALIGN_MIDDLE);
			Label l = new Label("Loading...");
			p.add(l);
			loadingPanel.setWidget(p);
		}
		Log.firebug(dockPanel.getAbsoluteLeft() + ", " + dockPanel.getAbsoluteTop());
		Log.firebug(dockPanel.getOffsetWidth() + ", " + dockPanel.getOffsetHeight());
		loadingPanel.setPopupPosition(dockPanel.getAbsoluteLeft(), dockPanel.getAbsoluteTop());
		loadingPanel.setSize(dockPanel.getOffsetWidth() + "px", dockPanel.getOffsetHeight() + "px");
		loadingPanel.show();
	}
	
	void hideLoading() {
		Log.firebug("Hiding loading panel");
		loadingPanel.hide();
	}
	
	protected void onLoad() {
		refreshScrollSize();
		showLoading();
	}
	
	public void onSvgLoaded(SvgWidget svgWidget) {
		zoomFit();
		hideLoading();
	}
	
	void refreshScrollSize() {
		scroll.setSize(getParent().getOffsetWidth() + "px", getParent().getOffsetHeight() + "px");
	}
	
	void setSlider(double percent) {
		try {
			zoomSlider.setCurrentValue(percent, false);
		} catch(Throwable e) {
			Log.firebug(e.getMessage());
		}
	}
	
	double getZoomFactor() {
		return svgWidget.getSvgModel().getZoomFactor();
	}
	
	int getZoomPercent() {
		return (int)(getZoomFactor() * 100);
	}
	
	void zoom(double percent) {
		Log.firebug("Zooming to " + percent);
		SvgModel svg = svgWidget.getSvgModel();
		double oWidth = svg.getWidth();
		double oHeight = svg.getHeight();
		int hscroll = scroll.getHorizontalScrollPosition();
		int vscroll = scroll.getScrollPosition();
		
		svg.setZoomFactor(
			(double)percent / 100
		);
		
		setSlider(getZoomPercent());
		
		//Set scrollbar position so the center stays in place
		double nWidth = svg.getWidth();
		double nHeight = svg.getHeight();
		double dx = nWidth - oWidth;
		double dy = nHeight - oHeight;
		dx *= scroll.getOffsetWidth() / nWidth;
		dy *= scroll.getOffsetHeight() / nHeight;
		int hscrollNew = hscroll + (int)dx;
		int vscrollNew = vscroll + (int)dy;
		scroll.setHorizontalScrollPosition(hscrollNew);
		scroll.setScrollPosition(vscrollNew);
	}
	
	void zoomFit() {
		SvgModel svg = svgWidget.getSvgModel();
		svg.zoomToFit(scroll.getOffsetWidth(), scroll.getOffsetHeight());
		Log.firebug(getZoomPercent() + "");
		setSlider(getZoomPercent());
	}
}
