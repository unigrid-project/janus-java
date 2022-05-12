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

package org.unigrid.janus;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import org.unigrid.janus.model.service.Daemon;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.WindowService;
import org.unigrid.janus.view.MainWindow;
import javafx.scene.control.ListView;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import org.unigrid.janus.model.JanusModel;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.rpc.entity.GetBlockCount;
import org.unigrid.janus.model.rpc.entity.GetBootstrappingInfo;
import org.unigrid.janus.model.rpc.entity.GetWalletInfo;
import org.unigrid.janus.model.rpc.entity.Info;

@ApplicationScoped
public class Janus extends BaseApplication implements PropertyChangeListener {

	@Inject
	private Daemon daemon;

	@Inject
	private RPCService rpc;

	@Inject
	private DebugService debug;

	@Inject
	private WindowService window;

	@Inject
	private MainWindow mainWindow;

	@Inject
	private JanusPreloader preloader;

	@Inject
	private JanusModel janusModel;

	@Inject
	private Wallet wallet;

	private BooleanProperty ready = new SimpleBooleanProperty(false);
	private int block = -1;
	private String status = "inactive";
	private String walletStatus = "none";
	private String startupStatus;
	private String walletVersion;
	private String progress = "0";
	private Info info = new Info();
	private GetWalletInfo walletInfo = new GetWalletInfo();
	private GetBlockCount blockCount = new GetBlockCount();
	private GetBootstrappingInfo boostrapInfo = new GetBootstrappingInfo();
	private Boolean checkForStatus = true;

	@PostConstruct
	@SneakyThrows
	private void init() {
		startDaemon();
		janusModel.addPropertyChangeListener(this);
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals(janusModel.APP_RESTARTING)) {
			this.restartDaemon();
		}
	}

	public void startDaemon() {
		//TODO: should this change to spalshScreenInsted
		try {
			daemon.start();
		} catch (Exception e) {
			Alert a = new Alert(AlertType.ERROR,
				e.getMessage(),
				ButtonType.OK);
			a.showAndWait();
		}
		System.out.println("Daemon start done.");
	}

	@PreDestroy
	@SneakyThrows
	private void destroy() {
		daemon.stop();
	}

	@Override
	public void start(Stage stage, Application.Parameters parameters, HostServices hostServices) throws Exception {

		startSplashScreen();
		window.setHostServices(hostServices);

		ready.addListener(
			new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> ov, Boolean t,
					Boolean t1
				) {
					if (t1) {
						ready.setValue(Boolean.FALSE);
						Platform.runLater(new Runnable() {
							public void run() {
								rpc.stopPolling();
								wallet.setOffline(Boolean.FALSE);
								rpc.pollForInfo(5 * 1000);
								startMainWindow();
								janusModel.setAppState(JanusModel.AppState.LOADED);
								preloader.stopSpinner();
								preloader.hide();
							}
						});
					}
				}
			}
		);

	}

	private void startMainWindow() {
		try {
			mainWindow.show();

			mainWindow.bindDebugListViewWidth(0.98);
			debug.setListView((ListView) window.lookup("lstDebug"));

		} catch (Exception e) {
			Alert a = new Alert(AlertType.ERROR,
				e.getMessage(),
				ButtonType.OK);
			a.showAndWait();
		}
	}

	@SneakyThrows
	private void startSplashScreen() {
		janusModel.setAppState(JanusModel.AppState.STARTING);

		preloader.initText();
		preloader.show();
		startUp();
	}

	private void startUp() {
		Task task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				do {
					try {
						walletInfo = rpc.call(new GetWalletInfo.Request(),
							GetWalletInfo.class);
					} catch (Exception e) {
						System.out.println("walletInfo: "
							+ e.getMessage());
					}
					boostrapInfo = rpc.call(new GetBootstrappingInfo.Request(),
						GetBootstrappingInfo.class);
					System.out.println("boostrapInfo: " + boostrapInfo.getResult().getStatus());
					walletStatus = boostrapInfo.getResult().getWalletstatus();
					progress = boostrapInfo.getResult().getProgress();
					status = boostrapInfo.getResult().getStatus();
					Thread.sleep(1000);
					System.out.println("walletInfo.hasError() " + walletInfo.hasError());
					if (status.equals("downloading")) {
						Platform.runLater(
								() -> {
								float f = Float.parseFloat(progress);
								window.getSplashScreenController().
										showProgressBar();
								window.getSplashScreenController().
									setText("Downloading blockchain");
								window.getSplashScreenController().
									updateProgress((float) (f / 100));
							});
					}
					if (status.equals("unarchiving")) {
						Platform.runLater(
								() -> {
								float f = Float.parseFloat(progress);
								window.getSplashScreenController().
									showProgressBar();
								window.getSplashScreenController().
									setText("Unarchiving blockchain");
								window.getSplashScreenController().
									updateProgress((float) (f / 100));
							});
					}
				} while (walletInfo.hasError());

				ready.setValue(Boolean.TRUE);

				return null;
			}
		};
		new Thread(task).start();

	}

	public void restartDaemon() {
		startDaemon();
		try {
			// need to wait a few seconds for the daemon to start
			// otherwise walletInfo will not give us a response
			Thread.sleep(1000);
		} catch (InterruptedException ex) {
			System.out.println("error on sleep");
		}
		ready.setValue(Boolean.FALSE);
		startSplashScreen();
		mainWindow.hide();
		janusModel.addPropertyChangeListener(this);
	}
}
