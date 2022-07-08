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
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Date;
import java.text.SimpleDateFormat;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.layout.FlowPane;
import javafx.util.Callback;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
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
import javafx.scene.control.Tooltip;
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
import org.unigrid.janus.model.rpc.entity.ListTransactions;
import org.unigrid.janus.model.rpc.entity.SendTransaction;
import org.unigrid.janus.model.rpc.entity.ValidateAddress;

@ApplicationScoped
public class WalletController implements Initializable, PropertyChangeListener {
	private static final DebugService DEBUG = new DebugService();
	private static final RPCService RPC = new RPCService();
	private Wallet wallet;
	private TransactionList transList = new TransactionList();
	private static final WindowService WINDOW = WindowService.getInstance();

	@FXML private Label lblBalance;
	@FXML private Label lblBalanceSend;
	@FXML private FlowPane pnlBalance;
	@FXML private VBox sendTransactionPnl;
	@FXML private Text sendWarnMsg;
	@FXML private TextField ugdAddressTxt;

	@FXML private TableView tblWalletTrans;
	@FXML private TableColumn colWalletTransDate;
	@FXML private TableColumn colWalletTransType;
	@FXML private TableColumn colWalletTransAddress;
	@FXML private TableColumn colWalletTransAmount;
	@FXML private BorderPane pnlUnlock;
	@FXML private TextField amountToSend;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		/* Empty on purpose */
		DEBUG.log("Initializing wallet transactions");
		wallet = WINDOW.getWallet();
		wallet.addPropertyChangeListener(this);
		transList.addPropertyChangeListener(this);
		WINDOW.setWalletController(this);
		setupWalletTransactions();
	}

	@FXML
	private void setupFormatter(MouseEvent event) {
		// need a way to remove this
		amountToSend.setTextFormatter(new TextFormatter<Double>(new DoubleStringConverter(), 0.0, integerFilter));
		amountToSend.setPromptText("UGD TO SEND");
	}

	private void setupLockScreen() {
		pnlUnlock.setVisible(false);
	}

	private void setupWalletTransactions() {
		try {
			colWalletTransDate.setCellValueFactory(new Callback<CellDataFeatures<Transaction, String>,
				ObservableValue<String>>() {

				public ObservableValue<String> call(CellDataFeatures<Transaction, String> t) {
					long time = t.getValue().getTime();
					Date date = new Date(time * 1000L);
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

					return new ReadOnlyStringWrapper(sdf.format(date));
				}
			});

			colWalletTransType.setCellValueFactory(new Callback<CellDataFeatures<Transaction, Hyperlink>,
				ObservableValue<Hyperlink>>() {

				public ObservableValue<Hyperlink> call(CellDataFeatures<Transaction, Hyperlink> t) {
					Transaction trans = t.getValue();
					int confrimations = trans.getConfirmations();
					Button btn = new Button();

					btn.setTooltip(new Tooltip(trans.getCategory()));

					btn.setOnAction(e -> {
						WINDOW.browseURL("https://explorer.unigrid.org/tx/" + trans.getTxid());
					});

					FontIcon fontIcon = new FontIcon("fas-wallet");

					if (trans.isGenerated()) {
						if (trans.getGeneratedfrom().equals("stake")) {
							fontIcon = new FontIcon("fas-coins");
							fontIcon.setIconColor(setColor(255, 140, 0, confrimations));
						} else {
							fontIcon = new FontIcon("fas-cubes");
							fontIcon.setIconColor(setColor(104, 197, 255, confrimations));
						}
						btn.setTooltip(new Tooltip(trans.getGeneratedfrom()));
					} else if (trans.getCategory().equals("send")
						|| trans.getCategory().equals("fee")) {
						fontIcon = new FontIcon("fas-arrow-right");
						fontIcon.setIconColor(setColor(255, 0, 0, confrimations));
					} else if (trans.getCategory().equals("receive")) {
						fontIcon = new FontIcon("fas-arrow-left");
						fontIcon.setIconColor(setColor(48, 186, 69, confrimations));
					}
					btn.setGraphic(fontIcon);
					//debug.print(trans.getCategory(), WalletController.class.getSimpleName());
					return new ReadOnlyObjectWrapper(btn);
				}
			});

			colWalletTransAddress.setCellValueFactory(new Callback<CellDataFeatures<Transaction, Hyperlink>,
				ObservableValue<Hyperlink>>() {

				public ObservableValue<Hyperlink> call(CellDataFeatures<Transaction, Hyperlink> t) {
					Hyperlink link = new Hyperlink();
					Transaction trans = t.getValue();
					link.setText(trans.getAddress());

					link.setOnAction(e -> {
						if (e.getTarget().equals(link)) {
							WINDOW.browseURL("https://explorer"
								+ ".unigrid.org/address/"
								+ trans.getAddress());
						}
					});

					Button btn = new Button();
					FontIcon fontIcon = new FontIcon("fas-clipboard");
					fontIcon.setIconColor(Paint.valueOf("#FFFFFF"));
					btn.setGraphic(fontIcon);

					btn.setOnAction(e -> {
						final Clipboard cb = Clipboard.getSystemClipboard();
						final ClipboardContent content = new ClipboardContent();

						content.putString(trans.getTxid());
						cb.setContent(content);

						if (SystemUtils.IS_OS_MAC_OSX) {
							Notifications
								.create()
								.title("Transaction copied to clipboard")
								.text(trans.getTxid())
								.position(Pos.TOP_RIGHT)
								.showInformation();
						} else {
							Notifications
								.create()
								.title("Transaction copied to clipboard")
								.text(trans.getTxid())
								.showInformation();
						}
					});

					link.setGraphic(btn);
					link.setAlignment(Pos.CENTER_RIGHT);
					return new ReadOnlyObjectWrapper(link);
				}
			});

			colWalletTransAmount.setCellValueFactory(new Callback<CellDataFeatures<Transaction, String>,
				ObservableValue<String>>() {

				public ObservableValue<String> call(CellDataFeatures<Transaction, String> t) {
					Transaction trans = t.getValue();
					double amount = trans.getAmount();

					if (trans.getCategory().equals("send")) {
						amount += trans.getFee();
					}

					return new ReadOnlyStringWrapper(String.format("%.8f", amount));
				}
			});
		} catch (Exception e) {
			DEBUG.log(String.format("ERROR: (setup wallet table) %s", e.getMessage()));
		}
	}

	private Color setColor(int r, int g, int b, int confirmations) {
		return Color.rgb(r, g, b, Math.min(1.0f, Math.max(0.8f, 0.8f))); //confirmations * 0.1f)));
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals(wallet.BALANCE_PROPERTY)) {
			DEBUG.log("Value: " + event.getNewValue().toString());
			lblBalance.setText(((BigDecimal) event.getNewValue()).toPlainString());
			lblBalanceSend.setText(((BigDecimal) event.getNewValue()).toPlainString());
		}

		if (event.getPropertyName().equals(wallet.LOCKED_PROPERTY)) {
			boolean locked = (boolean) event.getNewValue();
			// can determine from this if a send transaction needs a passphrase
		}

		if (event.getPropertyName().equals(transList.TRANSACTION_LIST)) {
			tblWalletTrans.setItems(transList.getTransactions());
		}

		if (event.getPropertyName().equals(wallet.TRANSACTION_COUNT)) {
			final ListTransactions trans = RPC.call(new ListTransactions.Request(0, 10),
				ListTransactions.class
			);

			transList.setTransactions(trans, 0);
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
			|| Double.parseDouble(amountToSend.getText()) == 0) {

			onErrorMessage("Please enter an amount of Unigrid to send.");
			return;
		} else {
			onErrorMessage("> 0");
		}

		if (ugdAddressTxt.getText().equals("") && ugdAddressTxt.getText() != null) {
			onErrorMessage("Please enter a valid Unigrid address.");
			return;
		}

		final ValidateAddress call = RPC.call(new ValidateAddress.Request(ugdAddressTxt.getText()),
			ValidateAddress.class
		);

		if (call.getError() != null) {
			DEBUG.log(String.format("ERROR: %s", call.getError()));
			onErrorMessage("Please enter a valid Unigrid address.");
		} else {
			if (!call.getResult().getValid()) {
				ugdAddressTxt.setText("");
				onErrorMessage("Please enter a valid Unigrid address.");
			} else {
				wallet.setSendArgs(new Object[]{ugdAddressTxt.getText(),
					Double.parseDouble(amountToSend.getText())}
				);

				if (wallet.getLocked()) {
					onErrorMessage("Locked wallet");
					WINDOW.getMainWindowController().unlockForSending();
					return;
				} else {
					//Object[] sendArgs = new Object[]{ugdAddressTxt.getText(),
					//Integer.parseInt(amountToSend.getText())};

					final SendTransaction send = RPC.call(new SendTransaction.Request(
						wallet.getSendArgs()),
						SendTransaction.class
					);

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
		final SendTransaction send = RPC.call(new SendTransaction.Request(wallet.getSendArgs()),
			SendTransaction.class
		);

		if (send.getError() != null) {
			onErrorMessage(send.getError().getMessage());
		} else {
			onSuccessMessage("TRANSACTION SENT!");
			DEBUG.log(send.getResult());
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

	@FXML
	private void onReceiveClicked(MouseEvent event) {
		WINDOW.getMainWindowController().tabSelect(4);
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
