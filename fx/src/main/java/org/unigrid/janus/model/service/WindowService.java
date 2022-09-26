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

package org.unigrid.janus.model.service;

import java.awt.Desktop;
import java.net.URI;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.application.HostServices;

import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.unigrid.janus.controller.component.WindowBarController;
import org.unigrid.janus.controller.AddressController;
import org.unigrid.janus.controller.DocumentationController;
import org.unigrid.janus.controller.MainWindowController;
import org.unigrid.janus.controller.NodesController;
import org.unigrid.janus.controller.WalletController;
import org.unigrid.janus.controller.TransactionsController;
import org.unigrid.janus.controller.OverlayController;
import org.unigrid.janus.controller.SettingsController;
import org.unigrid.janus.controller.SplashScreenController;
import org.unigrid.janus.controller.WarningController;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.cdi.Eager;
import org.unigrid.janus.model.rpc.entity.BaseResult;
import org.unigrid.janus.view.SplashScreen;

@Eager
@ApplicationScoped
@RequiredArgsConstructor
public class WindowService {
	@Inject private DebugService debug;

	private static Stage stage;
	private static WindowBarController wbController;
	private static MainWindowController mwController;
	private static WalletController wController;
	private static OverlayController olController;
	private static NodesController noController;
	private static TransactionsController transController;
	private static SettingsController settingsController;
	private static AddressController addrController;
	private static DocumentationController docsController;
	private static WarningController warnController;
	private static SplashScreenController splashController;
	private static SplashScreen splashScreen;

	private static WindowService serviceInstance = null;

	public static WindowService getInstance() {
		if (serviceInstance == null) {
			serviceInstance = new WindowService();
		}
		return serviceInstance;
	}

	public Stage getStage() {
		return this.stage;
	}

	public void setStage(Stage value) {
		this.stage = value;
	}

	public Node lookup(String id) {
		if (stage != null) {
			return stage.getScene().lookup("#" + id);
		} else {
			return null;
		}
	}

	public void browseURL(String url) {
		try {
			String os = System.getProperty("os.name").toLowerCase();
			if (os.indexOf("win") >= 0) {
				if (Desktop.isDesktopSupported()
					&& Desktop.getDesktop().isSupported(
						Desktop.Action.BROWSE)) {
					Desktop.getDesktop().browse(
						new URI(url));
				}
			} else if (os.indexOf("mac") >= 0) {
				Runtime rt = Runtime.getRuntime();
				rt.exec("open " + url);
			} else { // linux
				new ProcessBuilder("x-www-browser", url).start();
			}
		} catch (Exception ex) {
			debug.log(String.format(
				"ERROR: (browse url) %s",
				ex.getMessage()));
		}
	}

	public WindowBarController getWindowBarController() {
		return this.wbController;
	}

	public void setWindowBarController(WindowBarController controller) {
		this.wbController = controller;
	}

	public OverlayController getOverlayController() {
		return this.olController;
	}

	public void setOverlayController(OverlayController controller) {
		this.olController = controller;
	}

	public MainWindowController getMainWindowController() {
		return this.mwController;
	}

	public void setMainWIndowController(MainWindowController controller) {
		this.mwController = controller;
	}

	public WalletController getWalletController() {
		return this.wController;
	}

	public void setWalletController(WalletController controller) {
		this.wController = controller;
	}

	public NodesController getNodeController() {
		return this.noController;
	}

	public void setNodeController(NodesController controller) {
		this.noController = controller;
	}

	public TransactionsController getTransactionsController() {
		return this.transController;
	}

	public void setTransactionsController(TransactionsController controller) {
		this.transController = controller;
	}

	public AddressController getAddressController() {
		return this.addrController;
	}

	public void setAddressController(AddressController controller) {
		this.addrController = controller;
	}

	public DocumentationController getDocsController() {
		return this.docsController;
	}

	public void setDocsController(DocumentationController controller) {
		this.docsController = controller;
	}

	public SettingsController getSettingsController() {
		return this.settingsController;
	}

	public void setSettingsController(SettingsController controller) {
		this.settingsController = controller;
	}

	public void setSplashScreenController(SplashScreenController controller) {
		this.splashController = controller;
	}

	public SplashScreenController getSplashScreenController() {
		return this.splashController;
	}

	public void setSplashScreen(SplashScreen screen) {
		this.splashScreen = screen;
	}

	public SplashScreen getSplashScreen() {
		return this.splashScreen;
	}

	public void notifyIfError(BaseResult result) {
		if (result.hasError()) {
			Alert a = new Alert(AlertType.ERROR,
				String.format("Daemon Error: %s", result.getError().getMessage()),
				ButtonType.OK);
			a.showAndWait();
		}
	}

	public void setWarnController(WarningController controller) {
		this.warnController = controller;
	}

	public WarningController getWarningController() {
		return this.warnController;
	}
}
