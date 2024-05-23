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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Optional;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
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
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.stage.Stage;

import org.apache.commons.lang3.SystemUtils;
import org.unigrid.janus.model.BootstrapModel;
import org.unigrid.janus.model.DataDirectory;
import org.unigrid.janus.model.ExternalVersion;
import org.unigrid.janus.model.JanusModel;
import org.unigrid.janus.model.Preferences;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.rpc.entity.DumpWallet;
import org.unigrid.janus.model.rpc.entity.BackupWallet;
import org.unigrid.janus.model.rpc.entity.CreateRawTransaction;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.rpc.entity.EncryptWallet;
import org.unigrid.janus.model.rpc.entity.ImportWallet;
import org.unigrid.janus.model.rpc.entity.UpdatePassphrase;
import org.unigrid.janus.model.rpc.entity.ValidateAddress;
import org.unigrid.janus.model.signal.DebugMessage;
import org.unigrid.janus.model.signal.Navigate;
import static org.unigrid.janus.model.signal.Navigate.Location.*;
import org.unigrid.janus.model.signal.OverlayRequest;
import org.unigrid.janus.model.signal.UnlockRequest;
import org.unigrid.janus.model.signal.WalletRequest;
import org.unigrid.janus.view.AlertDialog;
import org.unigrid.janus.view.backing.OsxUtils;
import org.unigrid.janus.model.rpc.entity.ListUnspent;
import org.unigrid.janus.model.rpc.entity.SignRawTransaction;

import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.unigrid.janus.model.rpc.entity.SendRawTransaction;
import org.unigrid.janus.model.signal.MergeInputsRequest;

@ApplicationScoped
public class SettingsController
	implements Initializable, PropertyChangeListener, Showable {
	private Stage stage;
	private ObservableList<String> debugItems = FXCollections.observableArrayList();

	@Inject
	private DebugService debug;
	@Inject
	private HostServices hostServices;
	@Inject
	private JanusModel janusModel;
	@Inject
	private RPCService rpc;
	@Inject
	private Wallet wallet;
	private BootstrapModel bootStrap;
	@Inject
	private ExternalVersion externalVersion;

	@Inject
	private Event<Navigate> navigateEvent;
	@Inject
	private Event<OverlayRequest> overlayRequest;
	@Inject
	private Event<UnlockRequest> unlockRequestEvent;
	@Inject
	private Event<WalletRequest> walletRequestEvent;
	@Inject
	private Event<MergeInputsRequest> mergeInputsEvent;
	@FXML
	private AnchorPane pnlOverlay;

	private static final int TAB_SETTINGS_GENERAL = 1;
	private static final int TAB_SETTINGS_DISPLAY = 2;
	private static final int TAB_SETTINGS_PASSPHRASE = 3;
	private static final int TAB_SETTINGS_EXPORT = 4;
	private static final int TAB_SETTINGS_DEBUG = 5;
	private static final int TAB_SETTINGS_MAINTENANCE = 6;

	private OsxUtils osxUtils = new OsxUtils();

	@FXML
	private ListView lstDebug;

	@FXML
	private Label verLbl;
	// settings navigation
	@FXML
	private VBox pnlSetGeneral;
	@FXML
	private VBox pnlSetDisplay;
	@FXML
	private VBox pnlSetPassphrase;
	@FXML
	private VBox pnlSetExport;
	@FXML
	private VBox pnlSetDebug;
	@FXML
	private StackPane maintenanceStack;
	// passphrase
	@FXML
	private Button btnUpdatePassphrase;
	@FXML
	private PasswordField taPassphrase;
	@FXML
	private PasswordField taRepeatPassphrase;
	@FXML
	private Label txtPassphraseOne;
	@FXML
	private Label txtPassphraseTwo;
	@FXML
	private Label txtPassWarningOne;
	@FXML
	private Label txtPassWarningTwo;
	@FXML
	private Label txtErrorMessage;
	@FXML
	private CheckBox chkNotifications;
	@FXML
	private Label txtFxVersion;
	@FXML
	private Label txtBootstrapVersion;
	@FXML
	private Label txtDaemonVersion;
	@FXML
	private Label txtHedgehogVersion;
	@FXML
	private TextField txtAddress;

	@FXML
	private TableView<ListUnspent.Result> unspentTable;
	@FXML
	private TableColumn<ListUnspent.Result, String> txidColumn;
	@FXML
	private TableColumn<ListUnspent.Result, Integer> voutColumn;
	@FXML
	private TableColumn<ListUnspent.Result, String> addressColumn;
	@FXML
	private TableColumn<ListUnspent.Result, String> scriptPubKeyColumn;
	@FXML
	private TableColumn<ListUnspent.Result, Double> amountColumn;
	@FXML
	private TableColumn<ListUnspent.Result, Integer> confirmationsColumn;
	@FXML
	private Button btnListUnspent;
	@FXML
	private Button btnMergeInputs;
	private TextArea progressLog = new TextArea();
	private Button closeButton = new Button("Close");
	private ProgressIndicator progressIndicator = new ProgressIndicator();

	private String address;
	private double amount;
	private List<ListUnspent.Result> utxos;
	private BooleanProperty userConfirmed = new SimpleBooleanProperty(false);
	private Region placeholder = new Region();
	private Separator separator;
	private TextArea txidArea;
	private Button copyButton;
	private Label completionLabel;
	@FXML
	private VBox pnlSetMaintenance;

	@FXML
	private VBox mergeInputsOverlay;
	@FXML
	private TextField txtMergeAmount;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		System.out.println("Initilize settingsController!!!");
		lstDebug.setItems(debugItems);
		lstDebug.setPrefWidth(500);
		lstDebug.setPrefHeight(500); // TODO: Put these constants in a model perhaps?
		lstDebug.scrollTo(debugItems.size());
		verLbl.setText("version: ".concat(janusModel.getVersion()));
		txtFxVersion.setText(janusModel.getVersion());
		txtBootstrapVersion.setText(bootStrap.getInstance().getBootstrapVer());
		wallet.addPropertyChangeListener(this);
		chkNotifications.setSelected(Preferences.get().getBoolean("notifications", true));

		txidColumn.setCellValueFactory(new PropertyValueFactory<>("txid"));
		voutColumn.setCellValueFactory(new PropertyValueFactory<>("vout"));
		addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
		scriptPubKeyColumn
			.setCellValueFactory(new PropertyValueFactory<>("scriptPubKey"));
		amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
		confirmationsColumn
			.setCellValueFactory(new PropertyValueFactory<>("confirmations"));
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals(wallet.ENCRYPTED_STATUS)) {
			if (wallet.getEncrypted()) {
				debug.log(String.format("wallet.getEncrypted(): %s",
					wallet.getEncrypted()));
				txtPassphraseOne.setText("Old Passphrase");
				txtPassphraseTwo.setText("New Passphrase");
				txtPassWarningOne.setText("Update and change your wallets passphrase.");
				txtPassWarningTwo.setText("Please be sure to backup "
					+ "your passphrase in a safe location.");
			} else {
				txtPassphraseOne.setText("Passphrase");
				txtPassphraseTwo.setText("Repeat passphrase");
				txtPassWarningOne.setText(
					"Warning! This will encrypt your " + "wallet with a passphrase. "
					+ "Write down your passphrase and keep it safe.");
				txtPassWarningTwo.setText("If you have not backed up your "
					+ "wallet yet please do so first. An automatic wallet restart "
					+ "will also be performed.");
			}
		}
	}

	private void settingSelected(int tab) {
		System.out.println("Setting tab: " + tab);
		pnlSetGeneral.setVisible(false);
		pnlSetDisplay.setVisible(false);
		pnlSetPassphrase.setVisible(false);
		pnlSetExport.setVisible(false);
		pnlSetDebug.setVisible(false);
		maintenanceStack.setVisible(false);
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
				System.out.println("debug visable");
				break;
			case TAB_SETTINGS_MAINTENANCE:
				maintenanceStack.setVisible(true);
				System.out.println("maintenance visable");
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
	private void onSetMaintenanceTap(MouseEvent event) {
		System.out.println("Maintenance tab clicked");
		settingSelected(TAB_SETTINGS_MAINTENANCE);
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
	private void onOpenUnigrid(MouseEvent event)
		throws NullPointerException, IOException {
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
			dialog.setHeaderText(
				"Be sure that you have saved the passphrase somewhere secure.\n"
				+ "A wallet restart will be performed to encrypt your wallet.\n"
				+ "This cannot be undone without your passphrase.");
			ButtonType btnYes = new ButtonType("Yes", ButtonData.YES);
			ButtonType btnNo = new ButtonType("No", ButtonData.NO);
			dialog.getDialogPane().getButtonTypes().add(btnYes);
			dialog.getDialogPane().getButtonTypes().add(btnNo);
			dialog.getDialogPane().getStylesheets()
				.add("/org/unigrid/janus/view/main.css");
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
							UpdatePassphrase.class);

					} else {
						final EncryptWallet encrypt = rpc.call(
							new EncryptWallet.Request(
								new Object[]{taPassphrase.getText()}),
							EncryptWallet.class);

						// TODO: Fix this section
						// THIS IS ONLY NEEDED FOR THE INITIAL ENCRYPTION
						// SHOW LOAD SCREEN WHILE DAEMON STOPS
						// PAUSE CALLS TO THE DAEMON
						// CALL unigridd TO RESTART AGAIN
						// window.getMainWindowController().showSplash();
					}
					taPassphrase.setText("");
					taRepeatPassphrase.setText("");

					taRepeatPassphrase.setBorder(new Border(
						new BorderStroke(Color.TRANSPARENT, BorderStrokeStyle.SOLID,
							new CornerRadii(3), new BorderWidths(1))));

					navigateEvent.fire(Navigate.builder().location(WALLET_TAB).build());
					janusModel.setAppState(JanusModel.AppState.RESTARTING);
					// wallet.setLocked(true);
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
					taRepeatPassphrase
						.setBorder(new Border(new BorderStroke(Color.web("#1dab00"),
							BorderStrokeStyle.SOLID, new CornerRadii(3),
							new BorderWidths(1))));
					btnUpdatePassphrase.setDisable(false);
				} else {
					taRepeatPassphrase.setBorder(new Border(
						new BorderStroke(Color.RED, BorderStrokeStyle.SOLID,
							new CornerRadii(3), new BorderWidths(1))));
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
		fileChooser.getExtensionFilters()
			.addAll(new ExtensionFilter("Wallet file", "*.txt"));
		fileChooser.setInitialFileName("wallet.txt");
		File file = fileChooser.showOpenDialog(stage);
		debug.log(String.format("File chosen: %s", file.getAbsolutePath()));
		rpc.call(new ImportWallet.Request(file.getAbsolutePath()), ImportWallet.class);
	}

	@FXML
	private void onDumpWallet(MouseEvent event) {
		debug.log("Dump wallet clicked!");

		if (wallet.getLocked()) {
			unlockRequestEvent.fire(
				UnlockRequest.builder().type(UnlockRequest.Type.FOR_DUMP).build());
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
		fileChooser.getExtensionFilters()
			.addAll(new ExtensionFilter("Walet file", "*.dat"));
		fileChooser.setInitialFileName("wallet.dat");
		File file = fileChooser.showSaveDialog(stage);
		if (file == null) {
			debug.log("No file was choosen");
			return;
		}
		debug.log(String.format("File chosen: %s", file.getAbsolutePath()));
		// debug.log(rpc.callToJson(new BackupWallet.Request(file.getAbsolutePath())));

		final BackupWallet result = rpc.call(
			new BackupWallet.Request(file.getAbsolutePath()), BackupWallet.class);
		AlertDialog.open(result, Alert.AlertType.ERROR);
		debug.log(String.format("Backup wallet result: %s", rpc.resultToJson(result)));
	}

	@FXML
	private void onNotificationsShown(MouseEvent event) {
		Preferences.get().put("notifications",
			String.valueOf(chkNotifications.isSelected()));
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
			fileChooser.getExtensionFilters()
				.addAll(new ExtensionFilter("Walet file", "*.txt"));
			fileChooser.setInitialFileName("wallet.txt");
			File file = fileChooser.showSaveDialog(stage);

			debug.log(String.format("File chosen: %s", file.getAbsolutePath()));

			final DumpWallet result = rpc.call(
				new DumpWallet.Request(file.getAbsolutePath()), DumpWallet.class);

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

	public void eventDaemonVersion(@Observes ExternalVersion externalVersion) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				txtDaemonVersion.setText(externalVersion.getDaemonVersion());
				txtHedgehogVersion.setText(externalVersion.getHedgehogVersion());
			}
		});
	}

	@FXML
	private void fetchAndDisplayUnspent() {
		ListUnspent listUnspent = rpc.call(new ListUnspent.Request(), ListUnspent.class);
		List<ListUnspent.Result> utxos = listUnspent.getResult();

		unspentTable.setItems(FXCollections.observableArrayList(utxos));
	}

	@FXML
	private void mergeUnspentTokens() {
		ListUnspent listUnspent = rpc.call(new ListUnspent.Request(), ListUnspent.class);
		List<ListUnspent.Result> utxos = listUnspent.getResult();

		amount = Double.parseDouble(txtMergeAmount.getText());
		if (wallet.getLocked()) {
			unlockRequestEvent.fire(UnlockRequest.builder()
				.type(UnlockRequest.Type.FOR_MERGING).address(txtAddress.getText())
				.amount(amount)
				.utxos(utxos)
				.build());
			overlayRequest.fire(OverlayRequest.OPEN);
		} else {
			// fire event to merge inputs
			mergeInputsEvent.fire(MergeInputsRequest.builder()
				.type(MergeInputsRequest.Type.MERGE).address(txtAddress.getText())
				.amount(amount)
				.utxos(utxos)
				.build());
		}
	}

	/* MERGE UTXO LOGIC */
	public boolean isMyAddress(String address) {
		ValidateAddress validateAddress = rpc.call(new ValidateAddress.Request(address),
			ValidateAddress.class);
		return validateAddress.getResult().isValid();
	}

	private void initProgressDisplay() {
		separator = new Separator(Orientation.HORIZONTAL);
		// Add a label to inform the user
		completionLabel = new Label("Transactions have been completed. You can check each TXID on the explorer:");
		completionLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

		txidArea = new TextArea();
		txidArea.setEditable(false);
		txidArea.setWrapText(true);

		copyButton = new Button("Copy to Clipboard");
		copyButton.setOnAction(e -> {
			final Clipboard clipboard = Clipboard.getSystemClipboard();
			final ClipboardContent content = new ClipboardContent();
			content.putString(txidArea.getText());
			clipboard.setContent(content);
		});
		mergeInputsOverlay.getChildren().clear();
		progressLog.clear();
		progressLog.setEditable(false);
		progressLog.setPrefHeight(350);
		progressLog.setPrefWidth(500);

		ScrollPane scrollPane = new ScrollPane(progressLog);
		scrollPane.setFitToWidth(true);
		scrollPane.setFitToHeight(true);
		scrollPane.setStyle("-fx-background-color: transparent;");
		closeButton.setDisable(true);
		closeButton.setOnAction(e -> hideOverlay());

		mergeInputsOverlay.getChildren().addAll(scrollPane, progressIndicator, closeButton);
		progressIndicator.setVisible(true);
		mergeInputsOverlay.setSpacing(10);
		mergeInputsOverlay.setAlignment(Pos.CENTER);

		// Add a confirmation button and label to the mergeInputsOverlay
		Label confirmationLabel = new Label();
		confirmationLabel.setWrapText(true);
		confirmationLabel.setStyle("-fx-text-fill: white;");
		// Create an HBox to hold the buttons
		HBox buttonBox = new HBox(10);
		buttonBox.setAlignment(Pos.CENTER); // Center the buttons horizontally
		VBox.setVgrow(buttonBox, Priority.ALWAYS);
		Button confirmButton = new Button("Confirm");
		Button cancelButton = new Button("Cancel");
		confirmButton.setCursor(Cursor.HAND); // Add pointer cursor
		cancelButton.setCursor(Cursor.HAND); // Add pointer cursor
		// Set the preferred height of the placeholder
		placeholder.setPrefHeight(confirmationLabel.getHeight() + buttonBox.getHeight());

		// Initially set the visibility of confirmationLabel and buttonBox to true
		confirmationLabel.setVisible(true);
		buttonBox.setVisible(true);

		confirmButton.setOnAction(e -> {
			userConfirmed.set(true);
			// Remove the confirmationLabel and buttons
			mergeInputsOverlay.getChildren().removeAll(confirmationLabel, buttonBox);
			mergeUTXOs(address, utxos);
		});

		cancelButton.setOnAction(e -> {
			// Add back the confirmationLabel and buttons
			if (!mergeInputsOverlay.getChildren().contains(confirmationLabel)) {
				mergeInputsOverlay.getChildren().addAll(confirmationLabel, buttonBox);
			}
			hideOverlay();
		});

		if (isMyAddress(address)) {
			confirmationLabel.setText("The address " + address
				+ " belongs to your wallet. Are you positive you want to merge "
				+ "these inputs to this address?");
		} else {
			confirmationLabel.setText("WARNING: The address " + address
				+ " does not belong to the wallet. Are you certain you want to "
				+ "merge inputs to this address outside this wallet?");
		}
		// Add confirmationLabel and buttons to the HBox
		buttonBox.getChildren().addAll(confirmButton, cancelButton);
		// Add confirmationLabel, confirmButton, and cancelButton to the VBox
		HBox.setMargin(buttonBox, new Insets(0, 10, 0, 0));
		VBox.setMargin(confirmationLabel, new Insets(0, 20, 0, 20));
		mergeInputsOverlay.getChildren().addAll(confirmationLabel, buttonBox);
	}

	private void logProgress(String message, boolean isComplete) {
		Platform.runLater(() -> {
			progressLog.appendText(message + "\n");
			if (isComplete) {
				closeButton.setDisable(false);
				progressIndicator.setVisible(false);
			}
		});
	}

	private void eventMergeInputsRequest(@Observes MergeInputsRequest mergeRequest) {
		debug.print("Merge request fired", SettingsController.class.getSimpleName());

		address = mergeRequest.getAddress();
		amount = mergeRequest.getAmount();
		utxos = mergeRequest.getUtxos();

		// Initialize progress display
		initProgressDisplay();

		showOverlay();
	}

	public void mergeUTXOs(String destinationAddress, List<ListUnspent.Result> utxos) {
		if (!userConfirmed.get()) {
			return; // Exit if the user does not confirm
		}

		final double maxAmount = amount;
		List<List<ListUnspent.Result>> groups = new ArrayList<>();
		double currentTotal = 0;
		List<ListUnspent.Result> currentGroup = new ArrayList<>();
		utxos.sort(Comparator.comparingDouble(ListUnspent.Result::getAmount));

		for (int i = 0; i < utxos.size(); i++) {
			ListUnspent.Result utxo = utxos.get(i);
			double amount = utxo.getAmount();
			currentTotal += amount;

			if (currentTotal <= maxAmount || currentGroup.isEmpty()) {
				currentGroup.add(utxo);
			} else {
				groups.add(new ArrayList<>(currentGroup));
				currentGroup.clear();
				currentTotal = amount;
				currentGroup.add(utxo);
			}

			// If it's the last UTXO, add the currentGroup to groups
			if (i == utxos.size() - 1 && !currentGroup.isEmpty()) {
				groups.add(currentGroup);
			}
		}

		List<String> allTxids = new ArrayList<>();

		for (List<ListUnspent.Result> group : groups) {
			List<Map<String, Object>> inputs = new ArrayList<>();
			for (ListUnspent.Result utxo : group) {
				Map<String, Object> input = new HashMap<>();
				input.put("txid", utxo.getTxid());
				input.put("vout", utxo.getVout());
				inputs.add(input);
			}

			// Calculate the total amount for the current group
			double groupTotal = group.stream().mapToDouble(ListUnspent.Result::getAmount)
				.sum();

			Map<String, Double> outputs = new HashMap<>();
			outputs.put(destinationAddress, groupTotal);

			// Use groupTotal in the logProgress method instead of currentTotal
			logProgress(String.format("Processing group %d of %d with total value: %.2f",
				groups.indexOf(group) + 1, groups.size(), groupTotal), false);

			logProgress("Constructing raw transaction...", false);
			CreateRawTransaction rawTransaction = rpc.call(
				new CreateRawTransaction.Request(new Object[]{inputs, outputs}),
				CreateRawTransaction.class);
			String rawTx = rawTransaction.getResult();

			logProgress("Signing raw transaction...", false);
			SignRawTransaction signedTransaction = rpc.call(
				new SignRawTransaction.Request(new Object[]{rawTx}),
				SignRawTransaction.class);
			String signedTx = signedTransaction.getResult().getHex();

			logProgress("Broadcasting transaction...", false);
			SendRawTransaction txHash = rpc.call(
				new SendRawTransaction.Request(new Object[]{signedTx}),
				SendRawTransaction.class);
			allTxids.add(txHash.getResult());
			logProgress("Transaction broadcasted with hash: ", true);
		}

		txidArea = new TextArea(String.join("\n", allTxids));
		copyButton.setOnAction(e -> {
			final Clipboard clipboard = Clipboard.getSystemClipboard();
			final ClipboardContent content = new ClipboardContent();
			content.putString(String.join("\n", allTxids));
			clipboard.setContent(content);
		});
		copyButton.setCursor(Cursor.HAND);
		VBox.setMargin(copyButton, new Insets(0, 0, 10, 0));
		mergeInputsOverlay.getChildren().addAll(separator, completionLabel, txidArea, copyButton);
		mergeInputsOverlay.setVisible(true);
	}

	public void showOverlay() {
		pnlSetMaintenance.setVisible(false); // Hide the maintenance view
		mergeInputsOverlay.setVisible(true); // Show the overlay
	}

	public void hideOverlay() {
		mergeInputsOverlay.getChildren().removeAll(separator, txidArea, copyButton, completionLabel);

		mergeInputsOverlay.setVisible(false); // Hide the overlay
		pnlSetMaintenance.setVisible(true); // Show the maintenance view
	}
	/* MERGE UTXO LOGIC */
}
