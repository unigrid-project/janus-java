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

import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import org.unigrid.janus.fx.view.decorator.DecoratableWindow;
import org.unigrid.janus.fx.view.decorator.MovableWindow;
import org.unigrid.janus.fx.view.decorator.ResizableWindow;
import org.unigrid.janus.model.Direction;

public class MainWindow extends Application implements DecoratableWindow {
	@Getter private static MainWindow instance;
	@Getter private Stage stage;

	private final MovableWindow movableWindow = new MovableWindow();
	private final ResizableWindow resizableWindow = new ResizableWindow();

	public void move() {
		movableWindow.move();
	}

	public void maximize() {
		stage.setMaximized(!stage.isMaximized());
	}

	public void minimize() {
		stage.setIconified(true);
	}

	public void resize(Direction direction) {
		resizableWindow.resize(direction);
	}

	@Override
	public void start(Stage stage) {
		instance = this;
		this.stage = stage;

		stage.setScene(new Scene(new Browser()));
		stage.setTitle("Janus Not Electron Wallet Proof of Concept");

		SimpleDoubleProperty stageWidthProperty = new SimpleDoubleProperty(stage.getScene().getWindow().getWidth());
		stageWidthProperty.addListener((ObservableValue<? extends Number> observableValue,
			Number oldStageWidth, Number newStageWidth) -> {
			stage.getScene().getWindow().setWidth(newStageWidth.doubleValue());
			System.out.println("Stage width = " + newStageWidth);
		});

		movableWindow.decorate(this);
		resizableWindow.decorate(this);

		stage.centerOnScreen();
		stage.initStyle(StageStyle.UNDECORATED);
		stage.setResizable(true);
		stage.show();
	}

	@Override
	public void stop() {
		stage.close();
	}
}
