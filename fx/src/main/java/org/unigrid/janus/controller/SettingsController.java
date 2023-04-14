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

package org.unigrid.janus.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Optional;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.stage.Stage;

import org.apache.commons.lang3.SystemUtils;
import org.unigrid.janus.model.DataDirectory;
import org.unigrid.janus.model.JanusModel;
import org.unigrid.janus.model.Preferences;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.rpc.entity.DumpWallet;
import org.unigrid.janus.model.rpc.entity.BackupWallet;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.rpc.entity.EncryptWallet;
import org.unigrid.janus.model.rpc.entity.ImportWallet;
import org.unigrid.janus.model.rpc.entity.UpdatePassphrase;
import org.unigrid.janus.model.signal.DebugMessage;
import org.unigrid.janus.model.signal.Navigate;
import static org.unigrid.janus.model.signal.Navigate.Location.*;
import org.unigrid.janus.model.signal.OverlayRequest;
import org.unigrid.janus.model.signal.UnlockRequest;
import org.unigrid.janus.model.signal.WalletRequest;
import org.unigrid.janus.view.AlertDialog;
import org.unigrid.janus.view.backing.OsxUtils;

@ApplicationScoped
public class SettingsController implements Initializable, PropertyChangeListener, Showable {
	private Stage stage;
	private ObservableList<String> debugItems = FXCollections.observableArrayList();

	@Inject private DebugService debug;
	@Inject private HostServices hostServices;
	@Inject private JanusModel janusModel;
	@Inject private RPCService rpc;
	@Inject private Wallet wallet;

	@Inject private Event<Navigate> navigateEvent;
	@Inject private Event<OverlayRequest> overlayRequest;
	@Inject private Event<UnlockRequest> unlockRequestEvent;
	@Inject private Event<WalletRequest> walletRequestEvent;

	private static final int TAB_SETTINGS_GENERAL = 1;
	private static final int TAB_SETTINGS_DISPLAY = 2;
	private static final int TAB_SETTINGS_PASSPHRASE = 3;
	private static final int TAB_SETTINGS_EXPORT = 4;
	private static final int TAB_SETTINGS_DEBUG = 5;

	private OsxUtils osxUtils = new OsxUtils();

	@FXML private ListView lstDebug;

	@FXML private Label verLbl;
	// settings navigation
	@FXML private VBox pnlSetGeneral;
	@FXML private VBox pnlSetDisplay;
	@FXML private VBox pnlSetPassphrase;
	@FXML private VBox pnlSetExport;
	@FXML private VBox pnlSetDebug;
	// passphrase
	@FXML private Button btnUpdatePassphrase;
	@FXML private PasswordField taPassphrase;
	@FXML private PasswordField taRepeatPassphrase;
	@FXML private Label txtPassphraseOne;
	@FXML private Label txtPassphraseTwo;
	@FXML private Label txtPassWarningOne;
	@FXML private Label txtPassWarningTwo;
	@FXML private Label txtErrorMessage;
	@FXML private CheckBox chkNotifications;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		lstDebug.setItems(debugItems);
		lstDebug.setPrefWidth(500);
		lstDebug.setPrefHeight(500); //TODO: Put these constants in a model perhaps?
		lstDebug.scrollTo(debugItems.size());
		verLbl.setText("version: ".concat(janusModel.getVersion()));

		wallet.addPropertyChangeListener(this);
		chkNotifications.setSelected(Preferences.get().getBoolean("notifications", true));
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals(wallet.ENCRYPTED_STATUS)) {
			if (wallet.getEncrypted()) {
				debug.log(String.format("wallet.getEncrypted(): %s", wallet.getEncrypted()));
				txtPassphraseOne.setText("Old Passphrase");
				txtPassphraseTwo.setText("New Passphrase");
				txtPassWarningOne.setText("Update and change your wallets passphrase.");
				txtPassWarningTwo.setText("Please be sure to backup "
					+ "your passphrase in a safe location.");
			} else {
				txtPassphraseOne.setText("Passphrase");
				txtPassphraseTwo.setText("Repeat passphrase");
				txtPassWarningOne.setText("Warning! This will encrypt your "
					+ "wallet with a passphrase. "
					+ "Write down your passphrase and keep it safe.");
				txtPassWarningTwo.setText("If you have not backed up your "
					+ "wallet yet please do so first. An automatic wallet restart "
					+ "will also be performed.");
			}
		}
	}

	private void settingSelected(int tab) {
		pnlSetGeneral.setVisible(false);
		pnlSetDisplay.setVisible(false);
		pnlSetPassphrase.setVisible(false);
		pnlSetExport.setVisible(false);
		pnlSetDebug.setVisible(false);
		switch (tab) {
			case TAB_SETTINGS_GENERAL:
				pnlSetGeneral.setVisible(true);
				System.out.println("general visable");
				break;
			case TAB_SETTINGS_DISPLAY:
				pnlSetDisplay.setVisible(true);
				System.out.println("settings visable");
				break;
			case TAB_SETTINGS_PASSPHRASE:
				pnlSetPassphrase.setVisible(true);
				System.out.println("passphrase visable");
				break;
			case TAB_SETTINGS_EXPORT:
				pnlSetExport.setVisible(true);
				System.out.println("export visable");
				break;
			case TAB_SETTINGS_DEBUG:
				pnlSetDebug.setVisible(true);
				break;
			default:
				pnlSetDebug.setVisible(true);
				System.out.println("debug visable");
				break;
		}

	}

	@FXML
	private void onSetGeneralTap(MouseEvent event) {
		settingSelected(TAB_SETTINGS_GENERAL);
	}

	@FXML
	private void onSetDisplayTap(MouseEvent event) {
		settingSelected(TAB_SETTINGS_DISPLAY);
	}

	@FXML
	private void onSetPassphraseTap(MouseEvent event) {
		settingSelected(TAB_SETTINGS_PASSPHRASE);
	}

	@FXML
	private void onSetExportTap(MouseEvent event) {
		settingSelected(TAB_SETTINGS_EXPORT);
	}

	@FXML
	private void onSetDebugTap(MouseEvent event) {
		settingSelected(TAB_SETTINGS_DEBUG);
	}

	@FXML
	private void onOpenConf(MouseEvent event) throws NullPointerException {
		File conf = DataDirectory.getConfigFile();
		try {
			if (SystemUtils.IS_OS_MAC_OSX) {
				osxUtils.openFileOsx(DataDirectory.CONFIG_FILE);
			} else {
				hostServices.showDocument(conf.getAbsolutePath());
			}
		} catch (NullPointerException e) {
			debug.print(e.getMessage(), SettingsController.class.getSimpleName());
		}
	}

	@FXML
	private void onOpenGridnode(MouseEvent event) throws NullPointerException {
		File gridnode = DataDirectory.getGridnodeFile();
		try {
			if (SystemUtils.IS_OS_MAC_OSX) {
				osxUtils.openFileOsx(DataDirectory.GRIDNODE_FILE);
			} else {
				hostServices.showDocument(gridnode.getAbsolutePath());
			}
		} catch (NullPointerException e) {
			debug.print(e.getMessage(), SettingsController.class.getSimpleName());
		}
	}

	@FXML
	private void onOpenUnigrid(MouseEvent event) throws NullPointerException, IOException {
		String directory = DataDirectory.get();
		try {
			if (SystemUtils.IS_OS_MAC_OSX) {
				osxUtils.openDirectory(directory);
			} else {
				hostServices.showDocument(directory);
			}
		} catch (NullPointerException | IOException e) {
			System.out.println("open data directory " + e.getMessage());
		}
	}

	@FXML
	private void onLock(MouseEvent event) {
		try {
			Dialog<ButtonType> dialog = new Dialog<ButtonType>();
			dialog.setTitle("Confirmation");
			dialog.setHeaderText("Be sure that you have saved the passphrase somewhere secure.\n"
				+ "A wallet restart will be performed to encrypt your wallet.\n"
				+ "This cannot be undone without your passphrase.");
			ButtonType btnYes = new ButtonType("Yes", ButtonData.YES);
			ButtonType btnNo = new ButtonType("No", ButtonData.NO);
			dialog.getDialogPane().getButtonTypes().add(btnYes);
			dialog.getDialogPane().getButtonTypes().add(btnNo);
			dialog.getDialogPane().getStylesheets().add("/org/unigrid/janus/view/main.css");
			Optional<ButtonType> response = dialog.showAndWait();
			if (response.isPresent()) {
				if (response.get() == btnYes) {

					// IF WALLET IS ALREADY ENCRYPTED THE CALL MUST BE
					// MADE TO walletpassphrasechange
					// walletpassphrasechange "oldpassphrase" "newpassphrase"
					if (wallet.getEncrypted()) {
						final UpdatePassphrase update = rpc.call(
							new UpdatePassphrase.Request(taPassphrase.getText(),
								taRepeatPassphrase.getText()),
							UpdatePassphrase.class
						);

					} else {
						final EncryptWallet encrypt = rpc.call(
							new EncryptWallet.Request(new Object[]{taPassphrase.getText()}),
							EncryptWallet.class
						);

						//TODO: Fix this section
						//THIS IS ONLY NEEDED FOR THE INITIAL ENCRYPTION
						//SHOW LOAD SCREEN WHILE DAEMON STOPS
						//PAUSE CALLS TO THE DAEMON
						//CALL unigridd TO RESTART AGAIN
						//window.getMainWindowController().showSplash();
					}
					taPassphrase.setText("");
					taRepeatPassphrase.setText("");

					taRepeatPassphrase.setBorder(new Border(new BorderStroke(Color.TRANSPARENT,
						BorderStrokeStyle.SOLID,
						new CornerRadii(3),
						new BorderWidths(1)
					)));

					navigateEvent.fire(Navigate.builder().location(WALLET_TAB).build());
					janusModel.setAppState(JanusModel.AppState.RESTARTING);
					//wallet.setLocked(true);
				}
			}
		} catch (Exception e) {
			debug.log(String.format("ERROR: (passphrase update) %s", e.getMessage()));
		}
	}

	@FXML
	private void onRepeatPassphraseChange(KeyEvent event) {
		try {
			if (wallet.getEncrypted() && taRepeatPassphrase.getText() != "") {
				btnUpdatePassphrase.setDisable(false);
			} else {
				if (taPassphrase.getText().equals(taRepeatPassphrase.getText())) {
					taRepeatPassphrase.setBorder(new Border(
						new BorderStroke(Color.web("#1dab00"),
							BorderStrokeStyle.SOLID,
							new CornerRadii(3),
							new BorderWidths(1))));
					btnUpdatePassphrase.setDisable(false);
				} else {
					taRepeatPassphrase.setBorder(new Border(
						new BorderStroke(Color.RED,
							BorderStrokeStyle.SOLID,
							new CornerRadii(3),
							new BorderWidths(1))));
					btnUpdatePassphrase.setDisable(true);
				}
			}
		} catch (Exception e) {
			debug.log(String.format("ERROR: (passphrase change) %s", e.getMessage()));
		}
	}

	@FXML
	private void onImportWallet(MouseEvent event) {
		debug.log("Import wallet clicked!");
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Import");
		fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Wallet file", "*.txt"));
		fileChooser.setInitialFileName("wallet.txt");
		File file = fileChooser.showOpenDialog(stage);
		debug.log(String.format("File chosen: %s", file.getAbsolutePath()));
		rpc.call(new ImportWallet.Request(file.getAbsolutePath()), ImportWallet.class);
	}

	@FXML
	private void onDumpWallet(MouseEvent event) {
		debug.log("Dump wallet clicked!");

		if (wallet.getLocked()) {
			unlockRequestEvent.fire(UnlockRequest.builder().type(UnlockRequest.Type.FOR_DUMP).build());
			overlayRequest.fire(OverlayRequest.OPEN);
		} else {
			eventWalletRequest(WalletRequest.DUMP_KEYS);
		}
	}

	@FXML
	private void onBackupWallet(MouseEvent event) {
		debug.log("Backup wallet clicked!");
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Backup");
		fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Walet file", "*.dat"));
		fileChooser.setInitialFileName("wallet.dat");
		File file = fileChooser.showSaveDialog(stage);
		debug.log(String.format("File chosen: %s", file.getAbsolutePath()));
		// debug.log(rpc.callToJson(new BackupWallet.Request(file.getAbsolutePath())));

		final BackupWallet result = rpc.call(new BackupWallet.Request(file.getAbsolutePath()), BackupWallet.class);
		AlertDialog.open(result, Alert.AlertType.ERROR);
		debug.log(String.format("Backup wallet result: %s", rpc.resultToJson(result)));
	}

	@FXML
	private void onNotificationsShown(MouseEvent event) {
		Preferences.get().put("notifications", String.valueOf(chkNotifications.isSelected()));
	}

	@Override
	public void onShow(Stage stage) {
		this.stage = stage;
	}

	@Override
	public void onHide(Stage stage) {
		/* Empty on purpose */
	}

	public void eventWalletRequest(@Observes WalletRequest walletRequest) {
		if (walletRequest == WalletRequest.DUMP_KEYS) {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Export");
			fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Walet file", "*.txt"));
			fileChooser.setInitialFileName("wallet.txt");
			File file = fileChooser.showSaveDialog(stage);

			debug.log(String.format("File chosen: %s", file.getAbsolutePath()));

			final DumpWallet result = rpc.call(new DumpWallet.Request(file.getAbsolutePath()),
				DumpWallet.class
			);

			AlertDialog.open(result, Alert.AlertType.ERROR);
			debug.log(String.format("Dump wallet result: %s", rpc.resultToJson(result)));
		}
	}

	public void eventDebugMessage(@Observes DebugMessage debugMessage) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				debugItems.add(debugMessage.getMessage());

				if (Objects.nonNull(lstDebug)) {
					lstDebug.scrollTo(debugItems.size());
				}
			}
		});
	}
}
