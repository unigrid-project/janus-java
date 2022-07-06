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

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
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
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.unigrid.janus.model.UpdateWallet;
import org.unigrid.janus.model.service.PollingService;
import org.unigrid.janus.view.component.WindowBarButton;

//@Dependent
public class WindowBarController implements Decoratable, Initializable, PropertyChangeListener {

	private Decorator movableWindowDecorator;

	@Getter
	private Stage stage;

	private static RPCService rpc = new RPCService();

	private Wallet wallet;
	
	@Inject
	private UpdateWallet update;

	private static DebugService debug = new DebugService();
	private static WindowService window = WindowService.getInstance();
	@FXML
	private FontIcon spinner;
	private RotateTransition rt;
	
	@FXML
	private WindowBarButton updateButton;
	
	@Inject
	private PollingService pollingService;
	
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		//TODO: Remove when FX integration is done
		System.out.println("Initilizing window bar");
		update = CDI.current().select(UpdateWallet.class).get();
		pollingService = CDI.current().select(PollingService.class).get();
		update.addPropertyChangeListener(this);
		wallet = window.getWallet();
		wallet.addPropertyChangeListener(this);
		window.setWindowBarController(this);
		updateButton.setVisible(false);
		Tooltip t = new Tooltip("A new update is ready. Pleas restart the wallet");
		t.install(updateButton, t);

		//TODO: 2 minuts set for testing purpeses change to every 6 hours after testing is done
		pollingService.pollForUpdate(7200000);
	}

	public void propertyChange(PropertyChangeEvent event) {
		/*if (event.getPropertyName().equals(wallet.MONEYSUPPLY_PROPERTY)) {
			String sValue = String.format("%.8f", (double) event.getNewValue());
			debug.log(String.format("Money supply: %s", sValue));
			Label supply = (Label) window.lookup("txtSupply");
			if (supply != null) {
				supply.setText(sValue);
			}
		}*/
		if (event.getPropertyName().equals(wallet.PROCESSING_PROPERTY)) {
			if (wallet.getProcessingStatus()) {
				String status = String.format("processing status %s",
					(boolean) wallet.getProcessingStatus());
				//debug.log(status);
			}
		}
		if(event.getPropertyName().equals(update.getUPDATE_PROPERTY())) {
			showUpdateButton();
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
		//final Window window = ((Node) event.getSource()).getScene().getWindow();
		//window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
		System.exit(0);
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

	public void startSpinner() {
		spinner.setVisible(true);
		rt = new RotateTransition(Duration.millis(50000), spinner);
		rt.setByAngle(20000);
		rt.setCycleCount(Animation.INDEFINITE);
		rt.setAutoReverse(true);
		rt.setInterpolator(Interpolator.LINEAR);
		rt.play();
	}

	public void stopSpinner() {
		rt.stop();
		spinner.setVisible(false);
	}
	
	public void showUpdateButton() {
		System.out.println("Update button visable");
		updateButton.setVisible(true);
	}
	
	@FXML
	public void onUpdate(MouseEvent event) {
		updateButton.setVisible(false);
	}
}
