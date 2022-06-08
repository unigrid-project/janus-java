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
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;

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

import org.unigrid.janus.controller.view.SplashScreenController;
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
    private SplashScreenController splashController;

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
        // janusModel.getAppState().addObserver
        janusModel.addPropertyChangeListener(this);
        // PropertyConfigurator.configure(getClass().getResource("log4j.properties"));
    }

    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals(janusModel.APP_RESTARTING)) {
            this.restartDaemon();
        }
    }

    public void startDaemon() {
        debug.print("Janus starting daemon...", Janus.class.getSimpleName());
        // TODO: should this change to spalshScreenInsted
        try {
            daemon.start();
        } catch (Exception e) {
            Alert a = new Alert(AlertType.ERROR,
                    e.getMessage(),
                    ButtonType.OK);
            a.showAndWait();
        }

        debug.print("Daemon start done", Janus.class.getSimpleName());
    }

    @PreDestroy
    @SneakyThrows
    private void destroy() {
        // TODO: should this change to spalshScreenInsted
        daemon.stop();
    }

    @Override
    public void start(Stage stage, Application.Parameters parameters, HostServices hostServices) throws Exception {
        debug.print("start", Janus.class.getSimpleName());
        startSplashScreen();
        window.setHostServices(hostServices);

        ready.addListener(
                new ChangeListener<Boolean>() {
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
                                    rpc.pollForInfo(5 * 1000);
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

            mainWindow.bindDebugListViewWidth(0.98);
            debug.setListView((ListView) window.lookup("lstDebug"));

        } catch (Exception e) {
            System.out.print("error: " + e.getMessage());
            System.out.print("error: " + e.getCause().toString());
            Alert a = new Alert(AlertType.ERROR,
                    e.getMessage(),
                    ButtonType.OK);
            a.showAndWait();
        }
    }

    @SneakyThrows
    private void startSplashScreen() {
        debug.print("opening splash screen...", Janus.class.getSimpleName());
        Properties myProperties = new Properties();
        try {
            myProperties.load(getClass().getResourceAsStream("application.properties"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        String fullVer = Objects.requireNonNull((String) myProperties.get("proj.ver"));
        String filteredVer = fullVer.replace("-SNAPSHOT", "");
        janusModel.setVersion(filteredVer);

        //System.out.println("version: " + filteredVer);
        janusModel.setAppState(JanusModel.AppState.STARTING);

        preloader.initText();
        preloader.setVersion(filteredVer);
        preloader.show();
        startUp();
    }

    private void startUp() {
        debug.print("startup called...", Janus.class.getSimpleName());
        Task task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                debug.print("started while loop and calling unigridd RPC...", Janus.class.getSimpleName());
                Thread.sleep(2000);
                do {
                    try {
                        walletInfo = rpc.call(new GetWalletInfo.Request(),
                                GetWalletInfo.class);
                        boostrapInfo = rpc.call(new GetBootstrappingInfo.Request(),
                                GetBootstrappingInfo.class);
                        walletStatus = boostrapInfo.getResult().getWalletstatus();
                        // System.out.println("walletStatus: " + walletStatus);
                        progress = boostrapInfo.getResult().getProgress();
                        // System.out.println("progress: " + progress);
                        status = boostrapInfo.getResult().getStatus();
                        // System.out.println("status: " + status);
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        debug.print("RPC call error: " + e.getMessage().toString(), Janus.class.getSimpleName());
                        // debug.print("RPC call error: " + e., Janus.class.getSimpleName());

                        for (var x : e.getSuppressed())
                            debug.print("RPC call error: " + x.getCause().getMessage(), Janus.class.getSimpleName());

                        debug.print("RPC call error: " + e.getCause().getMessage(), Janus.class.getSimpleName());

                        for (var x : e.getStackTrace())
                            debug.print("RPC call error: " + x.toString(), Janus.class.getSimpleName());
                    }

                    if (status.equals("downloading")) {
                        Platform.runLater(
                                () -> {
                                    float f = Float.parseFloat(progress);
                                    window.getSplashScreenController().showProgressBar();
                                    window.getSplashScreenController().setText("Downloading blockchain");
                                    window.getSplashScreenController().updateProgress((float) (f / 100));
                                });
                    }
                    if (status.equals("unarchiving")) {
                        Platform.runLater(
                                () -> {
                                    float f = Float.parseFloat(progress);
                                    window.getSplashScreenController().showProgressBar();
                                    window.getSplashScreenController().setText("Unarchiving blockchain");
                                    window.getSplashScreenController().updateProgress((float) (f / 100));
                                });
                    }
                    if (status.equals("complete")) {
                        Platform.runLater(
                                () -> {
                                    window.getSplashScreenController().hideProgBar();
                                    window.getSplashScreenController().showSpinner();
                                    window.getSplashScreenController().setText("Starting unigrid backend");
                                });
                    }
                } while (walletInfo.hasError());
                debug.print("startup completed should load main screen..." + walletStatus, Janus.class.getSimpleName());
                window.getSplashScreenController().hideProgBar();
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
