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
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import lombok.Getter;
import org.unigrid.janus.view.decorator.Decoratable;
import org.unigrid.janus.view.decorator.Decorator;
import org.unigrid.janus.view.decorator.MovableWindowDecorator;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.WindowService;
import org.unigrid.janus.model.Wallet;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

public class WindowBarController implements Decoratable, Initializable, PropertyChangeListener {
	private Decorator movableWindowDecorator;
	@Getter private Stage stage;
	private static RPCService rpc = new RPCService();
	private static Wallet wallet = new Wallet();
	private static DebugService debug = new DebugService();
	private static WindowService window = new WindowService();

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		/* Empty on purpose */
		wallet.addPropertyChangeListener(this);
	}

	public void propertyChange(PropertyChangeEvent event) {
		debug.log("Window Bar change fired!");
		if (event.getPropertyName().equals(wallet.MONEYSUPPLY_PROPERTY)) {
			Label supply = (Label) window.lookup("txtSupply");
			if (supply != null) {
				supply.setText(String.format("%.8f", (double) event.getNewValue()));
			}
		}
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
		// TODO: find a place to do this that is guaranteed to be called when
		// application is closed
		rpc.stopPolling();
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
