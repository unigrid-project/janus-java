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
import javafx.scene.paint.Color;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import javafx.util.Callback;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.WindowService;
import org.unigrid.janus.model.Transaction;
import org.unigrid.janus.model.rpc.entity.ListTransactions;
import org.unigrid.janus.model.Passphrase;
import org.unigrid.janus.model.Wallet;

public class WalletController implements Initializable, PropertyChangeListener {
	private static DebugService debug = new DebugService();
	private static RPCService rpc = new RPCService();
	private static Wallet wallet = new Wallet();
	private static WindowService window = new WindowService();


	/* Injected fx:id from FXML */
	@FXML private Label lblBalance;
	@FXML private FlowPane pnlBalance;
	@FXML private FlowPane pnlLocked;
	// wallet table
	@FXML private TableView tblWalletTrans;
	@FXML private TableColumn colWalletTransDate;
	@FXML private TableColumn colWalletTransType;
	@FXML private TableColumn colWalletTransAddress;
	@FXML private TableColumn colWalletTransAmount;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		/* Empty on purpose */
		debug.log("Initializing wallet transactions");
		wallet.addPropertyChangeListener(this);
		setupWalletTransactions();
		Platform.runLater(() -> {
			try {
				debug.log("Loading wallet transactions");
				loadWalletPreviewTrans();
			} catch (Exception e) {
				debug.log(String.format("ERROR: (wallet trans init) %s", e.getMessage()));
			}
		});
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
					repeat.setBorder(new Border(
						new BorderStroke(Color.web("#1dab00"),
							BorderStrokeStyle.SOLID,
							new CornerRadii(3),
							new BorderWidths(1))));
					dialog.getDialogPane().lookupButton(buttonTypeOk).setDisable(false);
				} else {
					repeat.setBorder(new Border(
						new BorderStroke(Color.RED,
							BorderStrokeStyle.SOLID,
							new CornerRadii(3),
							new BorderWidths(1))));
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
				wallet.setLocked(false);
			}
		} catch (Exception e) {
			debug.log(String.format("ERROR: (passphrase unlock) %s", e.getMessage()));
		}
	}

	public void propertyChange(PropertyChangeEvent event) {
		debug.log("Wallet property change fired!");
		debug.log(event.getPropertyName());
		if (event.getPropertyName().equals(wallet.BALANCE_PROPERTY)) {
			debug.log(String.format("Value: %.8f", (double) event.getNewValue()));
			lblBalance.setText(String.format("%.8f", (double) event.getNewValue()));
		}
		if (event.getPropertyName().equals(wallet.LOCKED_PROPERTY)) {
			boolean locked = (boolean) event.getNewValue();
			debug.log(String.format("Lock changed: %b", locked));
			pnlLocked.setVisible(locked);
			pnlBalance.setVisible(!locked);
		}
	}

}
