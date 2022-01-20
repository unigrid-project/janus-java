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

package org.unigrid.janus.controller.view;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Optional;
import java.util.Date;
import java.text.SimpleDateFormat;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyEvent;
import javafx.stage.WindowEvent;
import javafx.scene.paint.Color;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.stage.Stage;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import javafx.util.Callback;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javafx.beans.value.ObservableValue;
// import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.WindowService;
// import org.unigrid.janus.model.rpc.entity.NewAddress;
import org.unigrid.janus.model.Transaction;
import org.unigrid.janus.model.Passphrase;
import org.unigrid.janus.model.rpc.entity.ListTransactions;
import org.unigrid.janus.model.Wallet;

public class MainWindowController implements Initializable, PropertyChangeListener {
	private static DebugService debug = new DebugService();
	private static RPCService rpc = new RPCService();
	private static Wallet wallet = new Wallet();
	private static WindowService window = new WindowService();

	private static final int TAB_WALLET = 1;
	private static final int TAB_TRANSACTIONS = 2;
	private static final int TAB_NODES = 3;
	private static final int TAB_SETTINGS = 4;

	private static final int TAB_SETTINGS_GENERAL = 1;
	private static final int TAB_SETTINGS_DISPLAY = 2;
	private static final int TAB_SETTINGS_PASSPHRASE = 3;
	private static final int TAB_SETTINGS_EXPORT = 4;
	private static final int TAB_SETTINGS_DEBUG = 5;

	/* Injected fx:id from FXML */
	@FXML private Label lblBalance;
	@FXML private Label lblBlockCount;
	@FXML private Label lblConnection;
	@FXML private FlowPane pnlBalance;
	@FXML private FlowPane pnlLocked;
	// wallet table
	@FXML private TableView tblWalletTrans;
	@FXML private TableColumn colWalletTransDate;
	@FXML private TableColumn colWalletTransType;
	@FXML private TableColumn colWalletTransAddress;
	@FXML private TableColumn colWalletTransAmount;
	// main navigation
	@FXML private ToggleButton btnWallet;
	@FXML private ToggleButton btnTransactions;
	@FXML private ToggleButton btnNodes;
	@FXML private ToggleButton btnSettings;
	@FXML private VBox pnlWallet;
	@FXML private VBox pnlTransactions;
	@FXML private VBox pnlNodes;
	@FXML private VBox pnlSettings;
	// settings navigation
	@FXML private VBox pnlSetGeneral;
	@FXML private VBox pnlSetDisplay;
	@FXML private VBox pnlSetPassphrase;
	@FXML private VBox pnlSetExport;
	@FXML private VBox pnlSetDebug;
	// passphrase
	@FXML private Button btnUpdatePassphrase;
	@FXML private TextArea taPassphrase;
	@FXML private TextArea taRepeatPassphrase;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		/* Empty on purpose */
		wallet.addPropertyChangeListener(this);
		setupWalletTransactions();
	}

	private void setupWalletTransactions() {
		try {
			colWalletTransDate.setCellValueFactory(
				new Callback<CellDataFeatures<Transaction, String>, ObservableValue<String>>() {
					public ObservableValue<String> call(CellDataFeatures<Transaction, String> t) {
						long time = t.getValue().getTime();
						Date date = new Date(time * 1000L);
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						return new ReadOnlyStringWrapper(sdf.format(date));
						// return new ReadOnlyStringWrapper("n/a");
					}
				});
			colWalletTransType.setCellValueFactory(
				new PropertyValueFactory<Transaction, String>("category"));
			colWalletTransAddress.setCellValueFactory(
				new PropertyValueFactory<Transaction, String>("account"));
			colWalletTransAmount.setCellValueFactory(
				new PropertyValueFactory<Transaction, Double>("amount"));
		} catch (Exception e) {
			debug.log(String.format("ERROR: (setup wallet table) %s", e.getMessage()));
		}
	}

	private void loadWalletPreviewTrans() {
		ListTransactions transactions = rpc.call(new ListTransactions.Request(0, 10),
			                                     ListTransactions.class);
		ObservableList<Transaction> walletTransactions = FXCollections.observableArrayList();

		for (Transaction t : transactions.getResult()) {
			walletTransactions.add(t);
		}

		tblWalletTrans.setItems(walletTransactions);
	}

	@FXML
	private void onShown(WindowEvent event) {
		debug.log("Shown event fired!");
		Platform.runLater(() -> {
			try {
				debug.log("Shown event executing.");
				loadWalletPreviewTrans();
			} catch (Exception e) {
				debug.log(String.format("ERROR: (onShown) %s", e.getMessage()));
			}
		});
	}

	@FXML
	private void onGetAddress(MouseEvent event) {
		debug.log("Get address clicked!");
		// debug.log(rpc.callToJson(new NewAddress.Request("Wilcokat007")));
	}

	private void tabSelect(int tab) {
		btnWallet.setSelected(false);
		btnTransactions.setSelected(false);
		btnNodes.setSelected(false);
		btnSettings.setSelected(false);
		pnlWallet.setVisible(false);
		pnlTransactions.setVisible(false);
		pnlNodes.setVisible(false);
		pnlSettings.setVisible(false);
		switch (tab) {
			case TAB_WALLET: pnlWallet.setVisible(true);
						btnWallet.setSelected(true);
						break;
			case TAB_TRANSACTIONS: pnlTransactions.setVisible(true);
						btnTransactions.setSelected(true);
						break;
			case TAB_NODES: pnlNodes.setVisible(true);
						btnNodes.setSelected(true);
						break;
			case TAB_SETTINGS: pnlSettings.setVisible(true);
						btnSettings.setSelected(true);
						break;
			default: pnlWallet.setVisible(true);
					 	btnWallet.setSelected(true);
						break;
		}

	}

	@FXML
	private void onWalletTap(MouseEvent event) {
		try {
			tabSelect(TAB_WALLET);
			loadWalletPreviewTrans();
			debug.log("Wallet clicked!");
		} catch (Exception e) {
			debug.log(String.format("ERROR: (wallet click) %s", e.getMessage()));
		}
	}

	@FXML
	private void onTransactionsTap(MouseEvent event) {
		try {
			tabSelect(TAB_TRANSACTIONS);
			// loadTransactions(1);
			debug.log("Transactions clicked!");
		} catch (Exception e) {
			debug.log(String.format("ERROR: (transactions click) %s", e.getMessage()));
		}
	}

	@FXML
	private void onNodesTap(MouseEvent event) {
		try {
			tabSelect(TAB_NODES);
			debug.log("Nodes clicked!");
		} catch (Exception e) {
			debug.log(String.format("ERROR: (nodes click) %s", e.getMessage()));
		}
	}

	@FXML
	private void onSettingsTap(MouseEvent event) {
		try {
			tabSelect(TAB_SETTINGS);
			debug.log("Settings clicked!");
		} catch (Exception e) {
			debug.log(String.format("ERROR: (settings click) %s", e.getMessage()));
		}
	}

	private void settingSelected(int tab) {
		pnlSetGeneral.setVisible(false);
		pnlSetDisplay.setVisible(false);
		pnlSetPassphrase.setVisible(false);
		pnlSetExport.setVisible(false);
		pnlSetDebug.setVisible(false);
		switch (tab) {
			case TAB_SETTINGS_GENERAL: pnlSetGeneral.setVisible(true);
						break;
			case TAB_SETTINGS_DISPLAY: pnlSetDisplay.setVisible(true);
						break;
			case TAB_SETTINGS_PASSPHRASE: pnlSetPassphrase.setVisible(true);
						break;
			case TAB_SETTINGS_EXPORT: pnlSetExport.setVisible(true);
						break;
			case TAB_SETTINGS_DEBUG: pnlSetDebug.setVisible(true);
						break;
			default: pnlSetDebug.setVisible(true);
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
	private void onUnlock(MouseEvent event) {
		debug.log("Unlock clicked!");
		try {
			Dialog<Passphrase> dialog = new Dialog<>();
			dialog.setTitle("Unlock Wallet");
			dialog.setHeaderText("Enter your pass phrase to unlock the wallet.\n"
				                 + "This will be the same as you used to lock it.");
			Label label1 = new Label("Passphrase:");
			Label label2 = new Label("Repeat Passphrase:");
			TextArea phrase = new TextArea();
			TextArea repeat = new TextArea();
			phrase.setPrefHeight(40);
			repeat.setPrefHeight(40);

			GridPane grid = new GridPane();
			grid.add(label1, 1, 1);
			grid.add(phrase, 1, 2);
			grid.add(label2, 1, 3);
			grid.add(repeat, 1, 4);
			dialog.getDialogPane().setContent(grid);

			ButtonType buttonTypeOk = new ButtonType("Unlock", ButtonData.OK_DONE);
			dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);
			dialog.getDialogPane().lookupButton(buttonTypeOk).setDisable(true);

			repeat.setOnKeyTyped((KeyEvent evt) -> {
				String sPhrase = phrase.getText();
				String sRepeat = repeat.getText();
				if (sPhrase.equals(sRepeat)) {
					dialog.getDialogPane().lookupButton(buttonTypeOk).setDisable(false);
				} else {
					dialog.getDialogPane().lookupButton(buttonTypeOk).setDisable(true);
				}
			});

			dialog.setResultConverter(new Callback<ButtonType, Passphrase>() {
				@Override
				public Passphrase call(ButtonType b) {
					if (b == buttonTypeOk) {
						return new Passphrase(phrase.getText(), repeat.getText());
					}

					return null;
				}
			});

			dialog.getDialogPane().getStylesheets().add("/org/unigrid/janus/view/main.css");

			Optional<Passphrase> result = dialog.showAndWait();

			if (result.isPresent()) {
				debug.log(String.format("Unlock dialog result: %s", result.get()));
				pnlLocked.setVisible(false);
				pnlBalance.setVisible(true);
			}
		} catch (Exception e) {
			debug.log(String.format("ERROR: (passphrase unlock) %s", e.getMessage()));
		}
	}

	@FXML
	private void onLock(MouseEvent event) {
		debug.log("Update passphrase clicked!");
		try {
			Dialog<ButtonType> dialog = new Dialog<ButtonType>();
			dialog.setTitle("Confirmation");
			dialog.setHeaderText("Be sure that you have saved the passphrase.\n"
								  + "Are you sure you're ready to lock your wallet now?\n"
				 				  + "This cannot be undone without your passphrase.");
			ButtonType btnYes = new ButtonType("Yes", ButtonData.YES);
			ButtonType btnNo = new ButtonType("No", ButtonData.NO);
			dialog.getDialogPane().getButtonTypes().add(btnYes);
			dialog.getDialogPane().getButtonTypes().add(btnNo);
			dialog.getDialogPane().getStylesheets().add("/org/unigrid/janus/view/main.css");
			Optional<ButtonType> response = dialog.showAndWait();
			debug.log(String.format("Response: %s", response.get()));
			if (response.isPresent()) {
				if (response.get() == btnYes) {
					taPassphrase.setText("");
					taRepeatPassphrase.setText("");
					taRepeatPassphrase.setBorder(new Border(
						new BorderStroke(Color.TRANSPARENT,
							BorderStrokeStyle.SOLID,
							new CornerRadii(3),
							new BorderWidths(1))));
					pnlBalance.setVisible(false);
					pnlLocked.setVisible(true);
					tabSelect(TAB_WALLET);
				}
			}
		} catch (Exception e) {
			debug.log(String.format("ERROR: (passphrase update) %s", e.getMessage()));
		}
	}

	@FXML
	private void onRepeatPassphraseChange(KeyEvent event) {
		// debug.log("passphrase change event fired!");
		try {
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
		} catch (Exception e) {
			debug.log(String.format("ERROR: (passphrase change) %s", e.getMessage()));
		}
	}

	public void propertyChange(PropertyChangeEvent event) {
		Stage stage = window.getStage();
		debug.log("Main Window change fired!");
		debug.log(event.getPropertyName());
		if (event.getPropertyName().equals(wallet.BALANCE_PROPERTY)) {
			debug.log(String.format("Value: %.8f", (double) event.getNewValue()));
			lblBalance.setText(String.format("%.8f", (double) event.getNewValue()));
		}
		if (event.getPropertyName().equals(wallet.BLOCKS_PROPERTY)) {
			debug.log(String.format("blocks: %d", (int) event.getNewValue()));
			int blocks = (int) event.getNewValue();
			if (blocks > 0) {
				lblBlockCount.setText(String.format("Block Count:%d", blocks));
				lblBlockCount.setTextFill(Color.web("#e72"));
			} else {
				lblBlockCount.setText("Block Count: (out of sync)");
				lblBlockCount.setTextFill(Color.RED);
			}
		}
		if (event.getPropertyName().equals(wallet.CONNECTIONS_PROPERTY)) {
			debug.log(String.format("connections: %d", (int) event.getNewValue()));
			int connections = (int) event.getNewValue();
			if (connections > 0) {
				lblConnection.setText(String.format("Connected (%d)", connections));
				lblConnection.setTextFill(Color.web("#1dab00"));
			} else {
				lblConnection.setText("Disconnected");
				lblConnection.setTextFill(Color.RED);
			}
		}
	}
}
