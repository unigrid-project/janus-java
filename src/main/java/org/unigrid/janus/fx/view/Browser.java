package org.unigrid.janus.fx.view;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.layout.Background;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import lombok.Getter;

public class Browser extends Region {
	private static final String DEFAULT_URL = "http://localhost:8080/index.xhtml";

	@Getter private final WebView webView = new WebView();
	private final WebEngine webEngine = webView.getEngine();

	public Browser() {
		webEngine.load(DEFAULT_URL);
		getChildren().add(webView);
		webView.setContextMenuEnabled(false);
		this.setBackground(Background.EMPTY);
	}

	@Override
	protected void layoutChildren() {
		layoutInArea(webView, 0, 0, getWidth(), getHeight(), 0, HPos.CENTER, VPos.CENTER);
	}

	@Override
	protected double computePrefWidth(double height) {
		return 750;
	}

	@Override
	protected double computePrefHeight(double width) {
		return 500;
	}
}
