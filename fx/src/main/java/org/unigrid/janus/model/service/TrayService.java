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
// TODO: Re-enable when Bootstrap is updated with the Swing FX dependency
package org.unigrid.janus.model.service;

import dorkbox.systemTray.Menu;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.Separator;
import dorkbox.systemTray.SystemTray;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.util.Random;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.unigrid.janus.model.DataDirectory;
//import org.update4j.OS;
import org.unigrid.janus.model.JanusModel;

@ApplicationScoped
public class TrayService {
	@Inject private BrowserService browser;
	@Inject private HostServices hostServices;
	@Inject private JanusModel janusModel;
	@Inject private RPCService rpc;
	//private FXTrayIcon tray = null;
	private SystemTray systemTray = null;
	private Stage stage = null;

	public void initTrayService(Stage stage) {
		this.stage = stage;
		//System.out.println("Is systemTray supported " + SystemTray.isSupported());
		SystemTray.DEBUG = true;
		this.systemTray = SystemTray.get("SysTrayExample");
		if (systemTray == null) {
			throw new RuntimeException("Unable to load SystemTray!");
		}
		systemTray.setTooltip("Help");
		systemTray.setImage(getClass().getResource("/org/unigrid/janus/view/images/unigrid-round_77x77.png"));
		systemTray.setStatus("Starting");

		Menu mainMenu = systemTray.getMenu();
		mainMenu.add(new MenuItem("About", e -> {
			browser.navigate("https://unigrid.org");
		}));
		mainMenu.add(new Separator());
		MenuItem driveEntry = new MenuItem("Open Drive", e -> {
			final MenuItem entry = (MenuItem) e.getSource();
			systemTray.setStatus("Running");
			systemTray.setImage(getClass().getResource("/org/unigrid/janus/view/images/unigrid-round_77x77.png"));
			String location = DataDirectory.getDriveLocation();
			hostServices.showDocument(location);
			entry.setImage(getClass().getResource("/org/unigrid/janus/view/images/ugd_cloud.png"));
			entry.setText("Unigrid Drive");
			entry.setTooltip(null); // remove the tooltip
//                systemTray.remove(menuEntry);
		});
		MenuItem unigridEntry = new MenuItem("Show Unigrid", e -> {
			final MenuItem entry = (MenuItem) e.getSource();
			Platform.runLater(() -> {
				janusModel.setAppState(JanusModel.AppState.SHOW);
			});

			//entry.setImage(getClass().getResource("/org/unigrid/janus/view/images/unigrid-round_77x77.png"));
			entry.setText("Show Unigrid");
		});
		unigridEntry.setShortcut('o');
		unigridEntry.setImage(getClass().getResource("/org/unigrid/janus/view/images/unigrid-round_77x77.png"));
		driveEntry.setImage(getClass().getResource("/org/unigrid/janus/view/images/ugd_cloud.png"));
		// case does not matter
		driveEntry.setShortcut('d');
		driveEntry.setTooltip("Open your Unigrid drive");
		mainMenu.add(driveEntry);
		mainMenu.add(unigridEntry);

//		Menu submenu = new Menu("Options", getClass().getResource("/org/unigrid/janus/view/images/unigrid-round_77x77.png"));
//		submenu.setShortcut('t');
//
//		submenu.add(new MenuItem("Remove menu", getClass().getResource("/org/unigrid/janus/view/images/unigrid-round_77x77.png"), e -> {
//			MenuItem source = (MenuItem) e.getSource();
//			source.getParent().remove();
//		}));
//
//		submenu.add(new MenuItem("Add new entry to tray",
//			e -> systemTray.getMenu().add(new MenuItem("Random " + new Random().nextInt(10)))));
//		mainMenu.add(submenu);
		mainMenu.add(new Separator());
		systemTray.getMenu().add(new MenuItem("Quit", new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				systemTray.shutdown();
				System.out.println("Quit");
				Platform.runLater(() -> {
					janusModel.setAppState(JanusModel.AppState.HIDE);
				});
				// TODO: find a place to do this that is guaranteed to be called when
				// application is closed
				rpc.stopPolling();
				Platform.exit();
				// final Window window = ((Node) event.getSource()).getScene().getWindow();
				// window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
				System.exit(0);
			}
		})).setShortcut('q'); // case does not matter
//		if (SystemTray.isSupported()) {// && OS.CURRENT == OS.WINDOWS) {
//			System.out.println("Init tray icon");
//			tray = new FXTrayIcon(stage,
//				getClass().getResource("/org/unigrid/janus/view/images/unigrid-round_77x77.png"));
//			tray.show();
//			tray.addExitItem("Exit");
//			tray.setTooltip("Unigrid");
//		}
	}

	//TODO: Implement the blend of the images
	public void updateNewEventImage() {
//		if (!SystemTray.isSupported()) {
//			return;
//		}
//		Image image = manipulateImage(getClass()
//			.getResourceAsStream("/org/unigrid/janus/view/images/unigrid-round_77x77.png"));
//		if (image != null) {
//			tray.setGraphic(image);
//		}
	}

	public void updateBlockCount(int blocks) {
		// add menu item to systemTray
//		Menu infoMenu = null;
//		Menu mainMenu = systemTray.getMenu();
//		mainMenu.remove(infoMenu);
//		infoMenu = systemTray.getMenu();
//		infoMenu.add(new MenuItem("Block count: " + blocks, e -> {
//			// get the block count
//			browser.navigate("https://explorer.unigrid.org");
//		}));
//		
		systemTray.setStatus("Block count: " + blocks);
		systemTray.setImage(getClass().getResource("/org/unigrid/janus/view/images/unigrid-round_77x77.png"));
	}

	private void updateStandardImage() {
//		tray.setGraphic(getClass().getResource("/org/unigrid/janus/view/images/unigrid-round_77x77.png"));
	}

	private Image manipulateImage(InputStream path) {
		try {
			Image base = new Image(path);
			Image redDot = new Image(getClass()
				.getResourceAsStream("/org/unigrid/janus/view/images/red-dot.png"));
			ImageView bottom = new ImageView(base);
			ImageView top = new ImageView(redDot);
			top.setBlendMode(BlendMode.OVERLAY);

			Group blend = new Group(
				bottom,
				top
			);
			return blend.snapshot(new SnapshotParameters(), null);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
}
