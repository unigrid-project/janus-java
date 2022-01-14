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
import java.util.Date;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseEvent;
import javafx.stage.WindowEvent;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.beans.value.ObservableValue;
// import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.WindowService;
// import org.unigrid.janus.model.rpc.entity.NewAddress;
import org.unigrid.janus.model.Transaction;
import org.unigrid.janus.model.rpc.entity.ListTransactions;
import javafx.scene.control.Label;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import org.unigrid.janus.model.Wallet;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import java.text.SimpleDateFormat;
import javafx.application.Platform;
import javafx.scene.paint.Color;

public class MainWindowController implements Initializable, PropertyChangeListener {
	private static DebugService debug = new DebugService();
	private static RPCService rpc = new RPCService();
	private static Wallet wallet = new Wallet();
	private static WindowService window = new WindowService();

	private static final int TAB_SETTINGS_GENERAL = 1;
	private static final int TAB_SETTINGS_DISPLAY = 2;
	private static final int TAB_SETTINGS_PASSPHRASE = 3;
	private static final int TAB_SETTINGS_EXPORT = 4;
	private static final int TAB_SETTINGS_DEBUG = 5;

	/* Injected fx:id from FXML */
	@FXML private Label lblBalance;
	@FXML private Label lblBlockCount;
	@FXML private Label lblConnection;
	// wallet table
	@FXML private TableView tblWalletTrans;
	@FXML private TableColumn colWalletTransDate;
	@FXML private TableColumn colWalletTransType;
	@FXML private TableColumn colWalletTransAddress;
	@FXML private TableColumn colWalletTransAmount;
	// transactions table
	@FXML private TableView tblTransactions;
	@FXML private TableColumn colTransDate;
	@FXML private TableColumn colTransType;
	@FXML private TableColumn colTransAddress;
	@FXML private TableColumn colTransAmount;
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

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		/* Empty on purpose */
		wallet.addPropertyChangeListener(this);
		setupWalletTransactions();
		setupTransactions();
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

	private void setupTransactions() {
		try {
			colTransDate.setCellValueFactory(
				new Callback<CellDataFeatures<Transaction, String>, ObservableValue<String>>() {
					public ObservableValue<String> call(CellDataFeatures<Transaction, String> t) {
						long time = t.getValue().getTime();
						Date date = new Date(time * 1000L);
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						return new ReadOnlyStringWrapper(sdf.format(date));
						// return new ReadOnlyStringWrapper("n/a");
					}
				});
			colTransType.setCellValueFactory(
				new Callback<CellDataFeatures<Transaction, String>, ObservableValue<String>>() {
					public ObservableValue<String> call(CellDataFeatures<Transaction, String> t) {
						Transaction trans = t.getValue();
						if (trans.isGenerated()) {
							return new ReadOnlyStringWrapper(String.format("%s:%s",
								                             trans.getCategory(),
								                             trans.getGeneratedfrom()));
						} else {
							return new ReadOnlyStringWrapper(trans.getCategory());
						}
					}
				});
			colTransAddress.setCellValueFactory(
				new PropertyValueFactory<Transaction, String>("account"));
			colTransAmount.setCellValueFactory(
				new PropertyValueFactory<Transaction, Double>("amount"));
		} catch (Exception e) {
			debug.log(String.format("ERROR: (setup wallet table) %s", e.getMessage()));
		}
	}

	private void loadTransactions(int page) {
		ListTransactions trans = rpc.call(new ListTransactions.Request(page * 100, 100),
			                                     ListTransactions.class);
		ObservableList<Transaction> transactions = FXCollections.observableArrayList();

		for (Transaction t : trans.getResult()) {
			transactions.add(t);
		}

		tblTransactions.setItems(transactions);
	}

	@FXML
	private void onShown(WindowEvent event) {
		debug.log("Shown event fired!");
		Platform.runLater(() -> {
			try {
				debug.log("Shown event executing.");
				loadWalletPreviewTrans();
				loadTransactions(1);
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

	@FXML
	private void onWalletTap(MouseEvent event) {
		try {
			btnTransactions.setSelected(false);
			btnNodes.setSelected(false);
			btnSettings.setSelected(false);
			btnWallet.setSelected(true);
			pnlTransactions.setVisible(false);
			pnlNodes.setVisible(false);
			pnlSettings.setVisible(false);
			pnlWallet.setVisible(true);
			debug.log("Wallet clicked!");
		} catch (Exception e) {
			debug.log(String.format("ERROR: (wallet click) %s", e.getMessage()));
		}
	}

	@FXML
	private void onTransactionsTap(MouseEvent event) {
		try {
			btnNodes.setSelected(false);
			btnWallet.setSelected(false);
			btnSettings.setSelected(false);
			btnTransactions.setSelected(true);
			pnlWallet.setVisible(false);
			pnlNodes.setVisible(false);
			pnlSettings.setVisible(false);
			pnlTransactions.setVisible(true);
			debug.log("Transactions clicked!");
		} catch (Exception e) {
			debug.log(String.format("ERROR: (transactions click) %s", e.getMessage()));
		}
	}

	@FXML
	private void onNodesTap(MouseEvent event) {
		try {
			btnWallet.setSelected(false);
			btnTransactions.setSelected(false);
			btnSettings.setSelected(false);
			btnNodes.setSelected(true);
			pnlWallet.setVisible(false);
			pnlTransactions.setVisible(false);
			pnlSettings.setVisible(false);
			pnlNodes.setVisible(true);
			debug.log("Nodes clicked!");
		} catch (Exception e) {
			debug.log(String.format("ERROR: (nodes click) %s", e.getMessage()));
		}
	}

	@FXML
	private void onSettingsTap(MouseEvent event) {
		try {
			btnWallet.setSelected(false);
			btnTransactions.setSelected(false);
			btnNodes.setSelected(false);
			btnSettings.setSelected(true);
			pnlWallet.setVisible(false);
			pnlTransactions.setVisible(false);
			pnlNodes.setVisible(false);
			pnlSettings.setVisible(true);
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
