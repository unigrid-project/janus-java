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
import java.text.SimpleDateFormat;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import javafx.util.Callback;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.util.function.UnaryOperator;
import javafx.animation.PauseTransition;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.util.converter.IntegerStringConverter;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.WindowService;
import org.unigrid.janus.model.Transaction;
import org.unigrid.janus.model.rpc.entity.ListTransactions;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.rpc.entity.SendTransaction;
import org.unigrid.janus.model.rpc.entity.ValidateAddress;

public class WalletController implements Initializable, PropertyChangeListener {

	private static DebugService debug = new DebugService();
	private static RPCService rpc = new RPCService();
	private static Wallet wallet = new Wallet();
	private static WindowService window = new WindowService();


	/* Injected fx:id from FXML */
	@FXML
	private Label lblBalance;
	@FXML
	private Label lblBalanceSend;
	@FXML
	private FlowPane pnlBalance;
	@FXML
	private VBox sendTransactionPnl;
	@FXML
	private Text sendWarnMsg;
	@FXML
	private TextField ugdAddressTxt;
	// wallet table
	@FXML
	private TableView tblWalletTrans;
	@FXML
	private TableColumn colWalletTransDate;
	@FXML
	private TableColumn colWalletTransType;
	@FXML
	private TableColumn colWalletTransAddress;
	@FXML
	private TableColumn colWalletTransAmount;
	@FXML
	private BorderPane pnlUnlock;
	@FXML
	private TextField amountToSend;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		/* Empty on purpose */
		debug.log("Initializing wallet transactions");
		wallet.addPropertyChangeListener(this);
		window.setWalletController(this);
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

	@FXML
	private void setupFormatter(MouseEvent event) {
		// need a way to remove this
		amountToSend.setTextFormatter(
			new TextFormatter<Integer>(new IntegerStringConverter(), 0, integerFilter));
		amountToSend.setPromptText("UGD TO SEND");
	}

	private void setupLockScreen() {
		pnlUnlock.setVisible(false);
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
				}
			);
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

	public void propertyChange(PropertyChangeEvent event) {
		debug.log("Wallet property change fired!");
		debug.log(event.getPropertyName());
		if (event.getPropertyName().equals(wallet.BALANCE_PROPERTY)) {
			debug.log(String.format("Value: %.8f", (double) event.getNewValue()));
			lblBalance.setText(String.format("%.8f", (double) event.getNewValue()));
			lblBalanceSend.setText(String.format("%.8f", (double) event.getNewValue()));
		}
		if (event.getPropertyName().equals(wallet.LOCKED_PROPERTY)) {
			boolean locked = (boolean) event.getNewValue();
			// can determine from this if a send transaction needs a passphrase
		}
	}

	private UnaryOperator<Change> integerFilter = change -> {
		String newText = change.getControlNewText();

		if (newText.matches("\\d*|\\d+\\.\\d*")) {
			return change;
		}
		return null;
	};

	@FXML
	private void onSendTransactionClicked(MouseEvent event) {
		if (amountToSend.getText() == "" || amountToSend.getText() == null
			|| Integer.parseInt(amountToSend.getText()) == 0) {
			onErrorMessage("Please enter an amount of Unigrid to send.");
			return;
		} else {
			onErrorMessage("> 0");
		}

		//check if address is valid
		if (ugdAddressTxt.getText() == "" && ugdAddressTxt.getText() != null) {
			onErrorMessage("Please enter a valid Unigrid address.");
			return;
		}
		final ValidateAddress call = rpc.call(
			new ValidateAddress.Request(ugdAddressTxt.getText()), ValidateAddress.class);
		if (call.getError() != null) {
			debug.log(String.format("ERROR: %s", call.getError()));
			onErrorMessage("Please enter a valid Unigrid address.");
		} else {
			if (!call.getResult().getValid()) {
				ugdAddressTxt.setText("");
				onErrorMessage("Please enter a valid Unigrid address.");
			} else {
				wallet.setSendArgs(new Object[]{ugdAddressTxt.getText(),
						Integer.parseInt(amountToSend.getText())});
				if (wallet.getLocked()) {
					onErrorMessage("Locked wallet");
					window.getMainWindowController().unlockForSending();
					return;
				} else {
					//Object[] sendArgs = new Object[]{ugdAddressTxt.getText(),
						//Integer.parseInt(amountToSend.getText())};
					final SendTransaction send = rpc.call(
						new SendTransaction.Request(wallet.getSendArgs()), SendTransaction.class);
					if (send.getError() != null) {
						onErrorMessage(send.getError().getMessage());
					} else {
						onSuccessMessage("TRANSACTION SENT!");
						resetText();
					}
				}
			}
		}
	}

	public void sendTransactionAfterUnlock() {
		final SendTransaction send = rpc.call(
				new SendTransaction.Request(wallet.getSendArgs()), SendTransaction.class);
		if (send.getError() != null) {
			onErrorMessage(send.getError().getMessage());
		} else {
			onSuccessMessage("TRANSACTION SENT!");
			debug.log(send.getResult());
			resetText();
			wallet.setSendArgs(null);	
		}
	}

	@FXML
	private void onCloseSendClicked(MouseEvent event) {
		sendTransactionPnl.setVisible(false);
		resetText();
	}

	private void resetText() {
		amountToSend.setText("");
		amountToSend.setPromptText("UGD TO SEND");
		ugdAddressTxt.setText("");
		ugdAddressTxt.setPromptText("UGD ADDRESS");
	}

	@FXML
	private void onOpenSendClicked(MouseEvent event) {
		sendTransactionPnl.setVisible(true);
	}

	private void onErrorMessage(String message) {
		sendWarnMsg.setFill(Color.RED);
		sendWarnMsg.setText(message);
		sendWarnMsg.setVisible(true);
		PauseTransition pause = new PauseTransition(Duration.seconds(3));
		pause.setOnFinished(e -> {
			sendWarnMsg.setVisible(false);
			sendWarnMsg.setText("");
		});
		pause.play();
	}

	private void onSuccessMessage(String message) {
		sendWarnMsg.setFill(Color.GREEN);
		sendWarnMsg.setText(message);
		sendWarnMsg.setVisible(true);
		PauseTransition pause = new PauseTransition(Duration.seconds(3));
		pause.setOnFinished(e -> {
			sendWarnMsg.setVisible(false);
			sendWarnMsg.setText("");
		});
		pause.play();
	}
}
