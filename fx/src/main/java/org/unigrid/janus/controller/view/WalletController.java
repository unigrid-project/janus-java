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

import jakarta.enterprise.context.ApplicationScoped;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Date;
import java.text.SimpleDateFormat;
import javafx.event.EventHandler;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.collections.ObservableList;
import javafx.util.Callback;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.function.UnaryOperator;
import javafx.animation.PauseTransition;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.util.converter.DoubleStringConverter;
import org.apache.commons.lang3.SystemUtils;
import org.controlsfx.control.Notifications;
import org.kordamp.ikonli.javafx.FontIcon;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.WindowService;
import org.unigrid.janus.model.Transaction;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.TransactionList;
import org.unigrid.janus.model.rpc.entity.SendTransaction;
import org.unigrid.janus.model.rpc.entity.ValidateAddress;

@ApplicationScoped
public class WalletController implements Initializable, PropertyChangeListener {

	private static DebugService debug = new DebugService();
	private static RPCService rpc = new RPCService();

	private Wallet wallet;

	private static TransactionList transList = new TransactionList();
	private static WindowService window = WindowService.getInstance();


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
		wallet = window.getWallet();
		wallet.addPropertyChangeListener(this);
		transList.addPropertyChangeListener(this);
		window.setWalletController(this);
		setupWalletTransactions();
	}

	@FXML
	private void setupFormatter(MouseEvent event) {
		// need a way to remove this
		amountToSend.setTextFormatter(
			new TextFormatter<Double>(new DoubleStringConverter(), 0.0, integerFilter));
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
				new Callback<CellDataFeatures<Transaction, Hyperlink>, ObservableValue<Hyperlink>>() {
					public ObservableValue<Hyperlink> call(CellDataFeatures<Transaction, Hyperlink> t) {
						Transaction trans = t.getValue();
						String text = trans.getCategory();
						if (trans.getCategory().equals("multipart")) {
							text = "More details";
						} else if (trans.isGenerated()) {
							text = String.format("%s:%s",
								trans.getCategory(),
								trans.getGeneratedfrom());
						}
						Hyperlink link = new Hyperlink();
						link.setText(text);
						link.setOnAction(new EventHandler<ActionEvent>() {
							@Override
							public void handle(ActionEvent e) {
								window.browseURL("https://explorer"
												+ ".unigrid.org/tx/"
												+ trans.getTxid());
							}
						});
						return new ReadOnlyObjectWrapper(link);
					}
				});
			colWalletTransAddress.setCellValueFactory(
				new Callback<CellDataFeatures<Transaction, Hyperlink>, ObservableValue<Hyperlink>>() {
					public ObservableValue<Hyperlink> call(CellDataFeatures<Transaction, Hyperlink> t) {
						Hyperlink link = new Hyperlink();
						Transaction trans = t.getValue();
						link.setText(trans.getAddress());
						link.setOnAction(new EventHandler<ActionEvent>() {
							@Override
							public void handle(ActionEvent e) {
								if (e.getTarget().equals(link)) {
									window.browseURL("https://explorer"
										+ ".unigrid.org/address/"
										+ trans.getAddress());
								}
							}
						});
						Button btn = new Button();
						FontIcon fontIcon = new FontIcon("fas-clipboard");
						fontIcon.setIconColor(Paint.valueOf("#FFFFFF"));
						btn.setGraphic(fontIcon);
						btn.setOnAction((ActionEvent event) -> {
							final Clipboard cb = Clipboard.getSystemClipboard();
							final ClipboardContent content = new  ClipboardContent();
							content.putString(trans.getAddress());
							cb.setContent(content);
							if (SystemUtils.IS_OS_MAC_OSX) {
								Notifications
									.create()
									.title("Address copied to clipboard")
									.text(trans.getAddress())
									.position(Pos.TOP_RIGHT)
									.showInformation();
							} else {
								Notifications
									.create()
									.title("Address copied to clipboard")
									.text(trans.getAddress())
									.showInformation();
							}
						});
						link.setGraphic(btn);
						link.setAlignment(Pos.CENTER_RIGHT);
						return new ReadOnlyObjectWrapper(link);
					}
				});
			colWalletTransAmount.setCellValueFactory(
				new PropertyValueFactory<Transaction, Double>("amount"));
		} catch (Exception e) {
			debug.log(String.format("ERROR: (setup wallet table) %s", e.getMessage()));
		}
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals(wallet.BALANCE_PROPERTY)) {
			debug.log(String.format("Value: %.8f", (double) event.getNewValue()));
			lblBalance.setText(String.format("%.8f", (double) event.getNewValue()));
			lblBalanceSend.setText(String.format("%.8f", (double) event.getNewValue()));
		}
		if (event.getPropertyName().equals(wallet.LOCKED_PROPERTY)) {
			boolean locked = (boolean) event.getNewValue();
			// can determine from this if a send transaction needs a passphrase
		}
		if (event.getPropertyName().equals(transList.TRANSACTION_LIST)) {
			ObservableList<Transaction> list = transList.getLatestTransactions(10);
			tblWalletTrans.setItems(list);
		}
		if (event.getPropertyName().equals(wallet.TRANSACTION_COUNT)) {
			ObservableList<Transaction> list = transList.getLatestTransactions(10);
			tblWalletTrans.setItems(list);
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
		if (amountToSend.getText().equals("") || amountToSend.getText() == null
			|| Integer.parseInt(amountToSend.getText()) == 0) {
			onErrorMessage("Please enter an amount of Unigrid to send.");
			return;
		} else {
			onErrorMessage("> 0");
		}

		//check if address is valid
		if (ugdAddressTxt.getText().equals("") && ugdAddressTxt.getText() != null) {
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
