/*
    The Janus Wallet
    Copyright © 2021 The Unigrid Foundation

    This program is free software: you can redistribute it and/or modify it under the terms of the
    addended GNU Affero General Public License as published by the Free Software Foundation, version 3
    of the License (see COPYING and COPYING.addendum).

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received an addended copy of the GNU Affero General Public License with this program.
    If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
*/

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
