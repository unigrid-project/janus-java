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

package org.unigrid.janus.controller.component;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import lombok.Getter;
import org.unigrid.janus.view.decorator.Decoratable;
import org.unigrid.janus.view.decorator.Decorator;
import org.unigrid.janus.view.decorator.MovableWindowDecorator;

public class WindowBarController implements Decoratable, Initializable {
	private Decorator movableWindowDecorator;
	@Getter private Stage stage;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		/* Empty on purpose */
	}

	@FXML
	private void onDecorateMove(MouseEvent event) {
		if (movableWindowDecorator == null) {
			movableWindowDecorator = new MovableWindowDecorator();
			stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			movableWindowDecorator.decorate(this, (Node) event.getSource());
		}
	}

	@FXML
	private void onExit(MouseEvent event) {
		final Window window = ((Node) event.getSource()).getScene().getWindow();
		window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
	}

	@FXML
	private void onMaximize(MouseEvent event) {
		final Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
		stage.setMaximized(!stage.isMaximized());
	}

	@FXML
	private void onMinimize(MouseEvent event) {
		final Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
		stage.setIconified(!stage.isIconified());
	}
}
