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
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.beans.PropertyChangeEvent;
import java.util.function.UnaryOperator;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
import org.unigrid.janus.model.Address;
import org.unigrid.janus.model.BootstrapModel;
import org.unigrid.janus.model.Address.Amount;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.BrowserService;
import org.unigrid.janus.model.Transaction;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.rpc.entity.ListTransactions;
import org.unigrid.janus.model.rpc.entity.SendTransaction;
import org.unigrid.janus.model.rpc.entity.ValidateAddress;
import org.unigrid.janus.model.service.PollingService;
import org.unigrid.janus.model.signal.Navigate;
import static org.unigrid.janus.model.signal.Navigate.Location.*;
import org.unigrid.janus.model.signal.OverlayRequest;
import org.unigrid.janus.model.signal.UnlockRequest;
import org.unigrid.janus.model.signal.WalletRequest;
import org.unigrid.janus.view.backing.TransactionList;

@ApplicationScoped
public class WalletController implements Initializable, PropertyChangeListener {
	@Inject private BrowserService browser;
	@Inject private DebugService debug;
	@Inject private PollingService polling;
	@Inject private RPCService rpc;
	@Inject private TransactionList transactionList;
	@Inject private Wallet wallet;

	@Inject private Event<Navigate> navigateEvent;
	@Inject private Event<OverlayRequest> overlayRequest;
	@Inject private Event<UnlockRequest> unlockRequestEvent;

	private int syncIntervalShort = 30000;
	private int syncIntervalLong = 3600000;

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
		debug.log("Initializing wallet transactions");
		wallet.addPropertyChangeListener(this);
		transactionList.addPropertyChangeListener(this);
		setupWalletTransactions();
	}

	public void compareBlockHeights() {
		if (BootstrapModel.isTestnet()) {
			// FIRE STOP SYNCING EVENT
			wallet.setSyncStatus(Wallet.SyncStatus.from("synced"));
			return;
		}
		int explorerHeight;
		try {
			explorerHeight = wallet.getExplorerHeight();
		} catch (Exception e) {
			explorerHeight = 0;
		}
		//System.out.println("EXPLORER HEIGHT: " + explorerHeight);
		if (wallet.getBlocks() < (explorerHeight - 100)) {
			// STOP LONG POLL IF RUNNING
			if (polling.getLongSyncTimerRunning()) {
				polling.stopLongSyncPoll();
			}
			System.out.println("BLOCK HEIGHT IS LOWER");
			//wallet.setCheckExplorer(Boolean.TRUE);
			if (!polling.getSyncTimerRunning()) {
				polling.pollForSync(syncIntervalShort);
				System.out.println("STARTING SHORT SYNC POLL");
			}
			// FIRE SYNCING EVENT
			wallet.setSyncStatus(Wallet.SyncStatus.from("syncing"));
		} else {
			//wallet.setCheckExplorer(Boolean.FALSE);
			if (polling.getSyncTimerRunning()) {
				// TODO call refresh on the gridnode list here
				// on load if the wallet is not synced they will appear missing
				polling.stopSyncPoll();
			}
			// FIRE STOP SYNCING EVENT
			wallet.setSyncStatus(Wallet.SyncStatus.from("synced"));
			// START LONG SYNC POLL
			if (!polling.getLongSyncTimerRunning()) {
				polling.longPollForSync(syncIntervalLong);
				System.out.println("STARTING LONG SYNC POLL");
			}
			System.out.println("BLOCK HEIGHT IS OK: " + wallet.getBlocks());
			System.out.println("EXPLORER HEIGHT: " + explorerHeight);
		}
		//}
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
			colWalletTransDate.setCellValueFactory(cell -> {
				long time = ((CellDataFeatures<Transaction, String>) cell).getValue().getTime();
				Date date = new Date(time * 1000L);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

				return new ReadOnlyStringWrapper(sdf.format(date));
			});

			colWalletTransType.setCellValueFactory(cell -> {
				final Transaction transaction = ((CellDataFeatures<Transaction, Hyperlink>) cell).getValue();
				int confirmations = transaction.getConfirmations();
				Button btn = new Button();

				btn.setTooltip(new Tooltip(transaction.getCategory()));

				btn.setOnAction(e -> {
					browser.navigateTransaction(transaction.getTxId());
				});

				FontIcon fontIcon = new FontIcon("fas-wallet");

				if (transaction.isGenerated()) {
					if (transaction.getGeneratedfrom().equals("stake")) {
						fontIcon = new FontIcon("fas-coins");
						fontIcon.setIconColor(setColor(255, 140, 0, confirmations));
					} else {
						fontIcon = new FontIcon("fas-cubes");
						fontIcon.setIconColor(setColor(104, 197, 255, confirmations));
					}
					btn.setTooltip(new Tooltip(transaction.getGeneratedfrom()));
				} else if (transaction.getCategory().equals("send")
					|| transaction.getCategory().equals("fee")) {
					fontIcon = new FontIcon("fas-arrow-right");
					fontIcon.setIconColor(setColor(255, 0, 0, confirmations));
				} else if (transaction.getCategory().equals("receive")) {
					fontIcon = new FontIcon("fas-arrow-left");
					fontIcon.setIconColor(setColor(48, 186, 69, confirmations));
				}

				btn.setGraphic(fontIcon);
				return new ReadOnlyObjectWrapper(btn);
			});

			colWalletTransAddress.setCellValueFactory(cell -> {
				final Hyperlink link = new Hyperlink();
				final Transaction transaction = ((CellDataFeatures<Transaction, Hyperlink>) cell).getValue();
				link.setText(transaction.getAddress());

				link.setOnAction(e -> {
					if (e.getTarget().equals(link)) {
						browser.navigateAddress(transaction.getAddress());
					}
				});

				Button btn = new Button();
				FontIcon fontIcon = new FontIcon("fas-clipboard");
				fontIcon.setIconColor(Paint.valueOf("#FFFFFF"));
				btn.setGraphic(fontIcon);

				btn.setOnAction(e -> {
					final Clipboard cb = Clipboard.getSystemClipboard();
					final ClipboardContent content = new ClipboardContent();

					content.putString(transaction.getTxId());
					cb.setContent(content);

					if (SystemUtils.IS_OS_MAC_OSX) {
						Notifications
							.create()
							.title("Transaction copied to clipboard")
							.text(transaction.getTxId())
							.position(Pos.TOP_RIGHT)
							.showInformation();
					} else {
						Notifications
							.create()
							.title("Transaction copied to clipboard")
							.text(transaction.getTxId())
							.showInformation();
					}
				});

				link.setGraphic(btn);
				link.setAlignment(Pos.CENTER_RIGHT);
				return new ReadOnlyObjectWrapper(link);
			});

			colWalletTransAmount.setCellValueFactory(cell -> {
				Transaction trans = ((CellDataFeatures<Transaction, String>) cell).getValue();
				double amount = trans.getAmount();

				if (trans.getCategory().equals("send")) {
					amount += trans.getFee();
				}

				return new ReadOnlyStringWrapper(String.format("%.8f", amount));
			});
		} catch (Exception e) {
			debug.log(String.format("ERROR: (setup wallet table) %s", e.getMessage()));
		}
	}

	private Color setColor(int r, int g, int b, int confirmations) {
		return Color.rgb(r, g, b, Math.min(1.0f, Math.max(0.8f, 0.8f))); //confirmations * 0.1f)));
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals(Wallet.BALANCE_PROPERTY)) {
			debug.log("Value: " + event.getNewValue().toString());
			lblBalance.setText(((BigDecimal) event.getNewValue()).toPlainString());
			lblBalanceSend.setText(((BigDecimal) event.getNewValue()).toPlainString());
		}

		if (event.getPropertyName().equals(Wallet.BLOCKS_PROPERTY)) {
			if (!polling.getSyncTimerRunning() && !polling.getLongSyncTimerRunning()) {
				this.compareBlockHeights();
			}
		}

		if (event.getPropertyName().equals(Wallet.LOCKED_PROPERTY)) {
			boolean locked = (boolean) event.getNewValue();
			// can determine from this if a send transaction needs a passphrase
		}

		if (event.getPropertyName().equals(TransactionList.TRANSACTION_LIST)) {
			tblWalletTrans.setItems(transactionList.getTransactions());
		}

		if (event.getPropertyName().equals(Wallet.TRANSACTION_COUNT)) {
			final ListTransactions trans = rpc.call(new ListTransactions.Request(0, 10),
				ListTransactions.class
			);

			transactionList.setTransactions(trans, 0);
		}
	}

	private final UnaryOperator<Change> integerFilter = change -> {
		String newText = change.getControlNewText();

		if (newText.matches("\\d*|\\d+\\.\\d*")) {
			return change;
		}

		return null;
	};

	@FXML
	private void onSendTransactionClicked(MouseEvent event) {
		submit();
	}

	private void submit() {
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

		final ValidateAddress call = rpc.call(new ValidateAddress.Request(ugdAddressTxt.getText()),
			ValidateAddress.class
		);

		if (call.getError() != null) {
			debug.log(String.format("ERROR: %s", call.getError()));
			onErrorMessage("Please enter a valid Unigrid address.");
		} else {
			if (call.getResult().isValid()) {
				if (wallet.getLocked()) {
					onErrorMessage("Locked wallet");

					unlockRequestEvent.fire(
						UnlockRequest.builder().type(UnlockRequest.Type.FOR_SEND).build()
					);

					overlayRequest.fire(OverlayRequest.OPEN);
				} else {
					eventWalletRequest(WalletRequest.SEND_TRANSACTION);
				}
			} else {
				ugdAddressTxt.setText("");
				onErrorMessage("Please enter a valid Unigrid address.");
			}
		}
	}

	@FXML
	private void onCloseSendClicked(MouseEvent event) {
		close();
	}

	private void close() {
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
	private void onKeyPressed(KeyEvent ke) {
		if (ke.getCode() == KeyCode.ENTER) {
			submit();
		} else if (ke.getCode() == KeyCode.ESCAPE) {
			close();
		}
	}

	@FXML
	private void onOpenSendClicked(MouseEvent event) {
		sendTransactionPnl.setVisible(true);
		Platform.runLater(() -> amountToSend.requestFocus());
	}

	@FXML
	private void onReceiveClicked(MouseEvent event) {
		navigateEvent.fire(Navigate.builder().location(ADDRESS_TAB).build());
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

	private void eventWalletRequest(@Observes WalletRequest walletRequest) {
		if (walletRequest == WalletRequest.SEND_TRANSACTION) {
			final Address address = new Address(ugdAddressTxt.getText(), new Amount(amountToSend.getText()));
			final SendTransaction send = rpc.call(new SendTransaction.Request(address), SendTransaction.class);

			if (send.getError() == null) {
				onSuccessMessage("TRANSACTION SENT!");
				debug.log(send.getResult());
				resetText();
			} else {
				onErrorMessage(send.getError().getMessage());
			}
		}
	}
}
