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

package org.unigrid.janus;

//import com.dustinredmond.fxtrayicon.FXTrayIcon;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
//import java.awt.SystemTray;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import org.unigrid.janus.model.service.Daemon;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.BrowserService;
import org.unigrid.janus.view.MainWindow;
import org.update4j.OS;
import org.unigrid.janus.model.cdi.Eager;
import org.unigrid.janus.controller.SplashScreenController;
import org.unigrid.janus.model.JanusModel;
import org.unigrid.janus.model.UpdateWallet;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.producer.HostServicesProducer;
import org.unigrid.janus.model.rpc.entity.GetBootstrappingInfo;
import org.unigrid.janus.model.rpc.entity.GetWalletInfo;
//import org.unigrid.janus.model.rpc.entity.Info;
import org.unigrid.janus.model.service.Hedgehog;
import org.unigrid.janus.view.AlertDialog;
//import org.unigrid.janus.model.service.TrayService;

@Eager
@ApplicationScoped
public class Janus extends BaseApplication implements PropertyChangeListener {
	@Inject
	private Daemon daemon;
	@Inject
	private Hedgehog hedgehog;
	@Inject
	private RPCService rpc;
	@Inject
	private DebugService debug;
	@Inject
	private BrowserService window;
	@Inject
	private MainWindow mainWindow;
	@Inject
	private JanusPreloader preloader;
	@Inject
	private JanusModel janusModel;
	@Inject
	private UpdateWallet updateWallet;
	@Inject
	private SplashScreenController splashController;
	@Inject
	private Wallet wallet;
	// @Inject private TrayService tray;

	private BooleanProperty ready = new SimpleBooleanProperty(false);
	private int block = -1;
	private String status = "inactive";
	private String walletStatus = "none";
	private String startupStatus;
	private String walletVersion;
	private String progress = "0";
	private Boolean checkForStatus = true;

	@PostConstruct
	private void init() {
		System.out.println("getting to init");
		startDaemon();
		// janusModel.getAppState().addObserver
		janusModel.addPropertyChangeListener(this);
		// PropertyConfigurator.configure(getClass().getResource("log4j.properties"));
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals(janusModel.APP_RESTARTING)) {
			this.restartDaemon(true);
		}
	}

	public void startDaemon() {
		debug.print("Janus starting daemon...", Janus.class.getSimpleName());
		try {
			hedgehog.startHedgehog();
			//AlertDialog.open(AlertType.ERROR, "Something fucked up!");
		} catch (Exception e) {
			AlertDialog.open(AlertType.ERROR, e.getMessage());
		}
		try {
			
			daemon.start();
		} catch (Exception e) {
			AlertDialog.open(AlertType.ERROR, e.getMessage());
		}

		debug.print("Daemon start done", Janus.class.getSimpleName());
	}

	@PreDestroy
	@SneakyThrows
	private void destroy() {
		// TODO: should this change to spalshScreenInsted
		hedgehog.stopHedgehog();
		daemon.stop();
	}

	@Override
	public void start(Stage stage, Application.Parameters parameters,
			HostServices hostServices) throws Exception {
		Platform.setImplicitExit(false);

		debug.print("start", Janus.class.getSimpleName());
		// tray.initTrayService(stage);
		HostServicesProducer.setHostServices(hostServices);
		startSplashScreen();

		ready.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> ov, Boolean t,
					Boolean t1) {
				if (t1) {
					ready.setValue(Boolean.FALSE);

					Platform.runLater(new Runnable() {
						public void run() {
							debug.print("run poll", Janus.class.getSimpleName());
							// rpc.stopPolling();
							wallet.setOffline(Boolean.FALSE);
							startMainWindow();
							rpc.pollForInfo(10 * 1000);
							janusModel.setAppState(JanusModel.AppState.LOADED);
							preloader.stopSpinner();
							preloader.hide();
						}
					});
				}
			}
		});

	}

	public void startFromBootstrap(Stage stage) throws Exception {
		System.out.println(CDI.current());
		// tray.initTrayService(stage);
		debug.print("start", Janus.class.getSimpleName());
		System.out.println("start from bootstrap");
		startSplashScreen();

		ready.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
				if (t1) {
					ready.setValue(Boolean.FALSE);

					Platform.runLater(new Runnable() {
						public void run() {
							debug.print("run poll", Janus.class.getSimpleName());
							// rpc.stopPolling();
							wallet.setOffline(Boolean.FALSE);

							startMainWindow();
							rpc.pollForInfo(10 * 1000);
							janusModel.setAppState(JanusModel.AppState.LOADED);
							preloader.stopSpinner();
							preloader.hide();
						}
					});
				}
			}
		});

	}

	private void startMainWindow() {
		try {
			mainWindow.show();
		} catch (Exception e) {
			System.out.print("error: " + e.getMessage());

			if (Objects.nonNull(e.getCause())) {
				System.err.print("error: " + e.getCause().toString());
			}

			AlertDialog.open(AlertType.ERROR, e.getMessage());
		}
	}

	@SneakyThrows
	private void startSplashScreen() {
		debug.print("opening splash screen...", Janus.class.getSimpleName());
		janusModel.setAppState(JanusModel.AppState.STARTING);
		preloader.initText();
		preloader.show();
		startUp();
	}

	private void startUp() {
		debug.print("startup called...", Janus.class.getSimpleName());
		Task task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				debug.print("started while loop and calling unigridd RPC...",
						Janus.class.getSimpleName());
				GetWalletInfo walletInfo = null;
				Thread.sleep(1000);

				do {
					try {
						debug.print("startUp try loop...",
								Janus.class.getSimpleName());
						walletInfo = rpc.call(new GetWalletInfo.Request(),
								GetWalletInfo.class);

						final GetBootstrappingInfo boostrapInfo = rpc.call(
								new GetBootstrappingInfo.Request(),
								GetBootstrappingInfo.class);
						try {
							walletStatus = boostrapInfo.getResult().getWalletstatus();
							progress = boostrapInfo.getResult().getProgress();
							status = boostrapInfo.getResult().getStatus();
						} catch (Exception e) {
							// TODO: handle exception
							debug.print("boostrapInfo null: " + e.getMessage().toString(),
									Janus.class.getSimpleName());
						}
					} catch (Exception e) {
						// if we are in here this likely means unigridd is not responding
						// there needs to be a better way to handle this
						// display a message to the user that unigridd is not responding
						// add a button to the splash screen to retry
						// can we force quite the app and restart it?
						if (walletInfo != null) {
							debug.print(walletInfo.getResult().toString(),
									Janus.class.getSimpleName());
							if (walletInfo.hasError()) {
								debug.print(walletInfo.getError().toString(),
										Janus.class.getSimpleName());
								debug.print(walletInfo.getError().toString(),
										Janus.class.getSimpleName());
							}
						} else {
							debug.print("walletInfo null",
									Janus.class.getSimpleName());
						}

						debug.print("RPC call error: " + e.getMessage().toString(),
								Janus.class.getSimpleName());

						for (var x : e.getSuppressed()) {
							debug.print("RPC call error: " + x.getCause().getMessage(),
									Janus.class.getSimpleName());
						}

						debug.print("RPC call error: " + e.getCause().getMessage(),
								Janus.class.getSimpleName());

						for (var x : e.getStackTrace()) {
							debug.print("RPC call error: " + x.toString(),
									Janus.class.getSimpleName());
						}
						// forceQuitDaemon();
					}

					// TODO: Remove - model layer should not directly call these
					if (status.equals("downloading")) {
						Platform.runLater(() -> {
							float f = Float.parseFloat(progress);
							splashController.showProgressBar();
							splashController.setText("Downloading blockchain");
							splashController.updateProgress((float) (f / 100));
						});
					}

					// TODO: Remove - model layer should not directly call these
					if (status.equals("unarchiving")) {
						Platform.runLater(() -> {
							float f = Float.parseFloat(progress);
							splashController.showProgressBar();
							splashController.setText("Unarchiving blockchain");
							splashController.updateProgress((float) (f / 100));
						});
					}

					// TODO: Remove - model layer should not directly call these
					if (status.equals("complete")) {
						Platform.runLater(() -> {
							splashController.hideProgBar();
							splashController.showSpinner();
							splashController.setText("Starting unigrid backend");
						});
					}
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// Handle interruption, if necessary
					}
				} while (Objects.isNull(walletInfo) || walletInfo.hasError());

				debug.print("startup completed should load main screen..." + walletStatus,
						Janus.class.getSimpleName());

				// TODO: Remove - model layer should not directly call this
				splashController.hideProgBar();
				ready.setValue(Boolean.TRUE);
				return null;
			}
		};

		new Thread(task).start();
	}

	public void restartDaemon(Boolean shoulHideMain) {
		startDaemon();
		try {
			// need to wait a few seconds for the daemon to start
			// otherwise walletInfo will not give us a response
			Thread.sleep(1000);
		} catch (InterruptedException ex) {
			System.out.println("error on sleep");
		}

		ready.setValue(Boolean.FALSE);
		if (shoulHideMain) {
			startSplashScreen();
			mainWindow.hide();
		}

		janusModel.addPropertyChangeListener(this);
	}

	public void forceQuitDaemon() {
		try {
			String daemonName = "unigridd";
			OS os = OS.CURRENT;
			List<String> command = new ArrayList<>();

			if (os.equals(OS.WINDOWS)) {
				command.add("taskkill");
				command.add("/IM");
				command.add("unigridd.exe");
				command.add("/F");
			} else if (os.equals(OS.MAC) || os.equals(OS.LINUX)) {
				command.add("pkill");
				command.add("-9");
				command.add("-f");
				command.add(daemonName);
			} else {
				throw new UnsupportedOperationException("Unsupported operating system.");
			}

			ProcessBuilder processBuilder = new ProcessBuilder(command);
			Process process = processBuilder.start();

			// Read the output of the process to prevent hanging
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			boolean processNotFound = false;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
				if (os.equals(OS.WINDOWS) && line.contains("ERROR: The process")) {
					processNotFound = true;
				}
			}

			process.waitFor();

			if (processNotFound) {
				debug.print("unigridd.exe not found, attempting to restart now.",
						Janus.class.getSimpleName());
				// Handle the case when the process is not found
			} else {
				debug.print("Daemon process killed.", Janus.class.getSimpleName());
			}
			this.restartDaemon(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
