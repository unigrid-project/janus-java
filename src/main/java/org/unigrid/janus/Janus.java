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
import javafx.application.Application;
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
import org.unigrid.janus.model.rpc.entity.Info;

@ApplicationScoped
public class Janus extends BaseApplication {

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

	private BooleanProperty ready = new SimpleBooleanProperty(false);
	private int block = -1;
	private String status = "inactive";
	private String walletStatus = "none";
	private String startupStatus;
	private String progress = "0";
	private Info info = new Info();
	private Boolean checkForStatus = true;

	@PostConstruct
	@SneakyThrows
	private void init() {
		startDaemon();
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
		debug.log("Daemon start done.");
	}

	@PreDestroy
	@SneakyThrows
	private void destroy() {
		//TODO: should this change to spalshScreenInsted
		daemon.stop();
	}

	@Override
	public void start(Stage stage, Application.Parameters parameters) throws Exception {

		startSplashScreen();

		ready.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
				Platform.runLater(new Runnable() {
					public void run() {
						rpc.stopPolling();

						rpc.pollForInfo(10 * 1000);
						startMainWindow();
						preloader.stopSpinner();
						preloader.hide();
					}
				});

			}

		});

	}

	private void startMainWindow() {
		try {
			mainWindow.show();

			mainWindow.bindDebugListViewWidth(0.98);
			debug.setListView((ListView) window.lookup("lstDebug"));

			/*final Info info = rpc.call(new Info.Request(), Info.class);
			Jsonb jsonb = JsonbBuilder.create();
			String result = String.format("Info result: %s", jsonb.toJson(info.getResult()));
			debug.log(result);
			 */
		} catch (Exception e) {
			Alert a = new Alert(AlertType.ERROR,
				e.getMessage(),
				ButtonType.OK);
			a.showAndWait();
		}
	}

	@SneakyThrows
	private void startSplashScreen() {

		preloader.show();

		preloader.initText();

		rpc.pollForInfo(10 * 1000);

		startUp();

	}

	private void startUp() {
		Task task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				//while (block <= 0) {
				do {
					info = rpc.call(new Info.Request(), Info.class);
					block = info.getResult().getBlocks();
					walletStatus = info.getResult().getBootstrapping().getWalletstatus();
					status = info.getResult().getBootstrapping().getStatus();
					progress = info.getResult().getBootstrapping().getProgress();
					startupStatus = info.getResult().getStatus();
					System.out.println(startupStatus);
					if (checkForStatus) {
						if (status == "downloading") {
							// fire property to show progres bar
							preloader.setText("Downloading blockchain");
							checkForStatus = false;
						}
					}
					/*if (!checkForStatus && progress == "none") {

					}*/
				} while (!status.equals("inactive") || (status.equals("inactive") && startupStatus != null)); //&& !progress.equals("none")))
				ready.setValue(Boolean.TRUE);

				return null;
			}
		};
		new Thread(task).start();

	}

	public void restartDaemon() {
		destroy();
		startDaemon();
	}
}
