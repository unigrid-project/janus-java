/*
    The Janus Wallet
    Copyright © 2021-2022 The Unigrid Foundation, UGD Software AB

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
import jakarta.enterprise.event.Observes;
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
import org.unigrid.janus.model.Wallet;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.unigrid.janus.model.UpdateWallet;
import org.unigrid.janus.model.service.PollingService;
import org.unigrid.janus.model.signal.State;
import org.unigrid.janus.view.component.WindowBarButton;
import org.unigrid.janus.controller.MainWindowController;
import org.unigrid.janus.controller.Showable;
import org.unigrid.janus.model.service.Hedgehog;

@Dependent
public class WindowBarController implements Decoratable, Initializable, PropertyChangeListener, Showable {
	private static final Set<WindowBarController> CONTROLLERS = Collections.synchronizedSet(new HashSet<>());

	private Decorator movableWindowDecorator;
	private RotateTransition rt;
	@Getter private Stage stage;

	@FXML private FontIcon spinner;
	@FXML private WindowBarButton updateButton;

	@Inject private DebugService debug;
	@Inject private PollingService pollingService;
	@Inject private RPCService rpc;
	@Inject private UpdateWallet update;
	@Inject private Wallet wallet;
	@Inject private MainWindowController mainWindow;
	@Inject private Hedgehog hedgehog;
	// @Inject private TrayService tray;

	private int testTimeInterval = 10000;
	private int liveTimeInterval = 21600000;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		pollingService = CDI.current().select(PollingService.class).get();
		update.addPropertyChangeListener(this);
		wallet.addPropertyChangeListener(this);
		updateButton.setVisible(false);

		Tooltip t = new Tooltip("A new update is ready. Please restart the wallet");
		t.install(updateButton, t);
		if (!pollingService.getUpdateTimerRunning()) {
			pollingService.pollForUpdate(liveTimeInterval);
		}
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals(update.getUPDATE_PROPERTY())) {
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
	private void onExit(MouseEvent event) throws Exception {
		// TODO setting splash screen to visible is too slow
		mainWindow.showSplashScreen();

		((Node) event.getSource()).getScene().getWindow().hide();

		// TODO: find a place to do this that is guaranteed to be called when
		// application is closed
		rpc.stopPolling();

		// final Window window = ((Node) event.getSource()).getScene().getWindow();
		// window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
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

	public void showUpdateButton() {
		System.out.println("Update button visable");
		// tray.updateNewEventImage();

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				updateButton.setVisible(true);
			}
		});
	}

	@FXML
	public void onUpdate(MouseEvent event) {
		System.out.println("onUpdate clicked???");
		updateButton.setVisible(false);
		update.doUpdate();
		// TODO: move this code into UpdateWallet.java
		// linux the Unigrid app is not executable
	}

	private void startSpinner() {
		spinner.setVisible(true);
		rt = new RotateTransition(Duration.millis(50000), spinner);
		rt.setByAngle(20000);
		rt.setCycleCount(Animation.INDEFINITE);
		rt.setAutoReverse(true);
		rt.setInterpolator(Interpolator.LINEAR);
		rt.play();
	}

	private void stopSpinner() {
		rt.stop();
		spinner.setVisible(false);
	}

	/* Because of the dependant scope, we have to save the controllers locally and loop through them */
	public void eventState(@Observes State state) {
		for (WindowBarController c : CONTROLLERS) {
			if (state.isWorking()) {
				c.startSpinner();
			} else {
				c.stopSpinner();
			}
		}
	}

	@Override
	public void onShow(Stage stage) {
		CONTROLLERS.add(this);
	}

	@Override
	public void onHide(Stage stage) {
		CONTROLLERS.remove(this);
	}
}
