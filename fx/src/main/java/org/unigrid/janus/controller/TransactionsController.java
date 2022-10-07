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
import jakarta.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Date;
import java.text.SimpleDateFormat;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.geometry.Orientation;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.ScrollBar;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.apache.commons.lang3.SystemUtils;
import org.controlsfx.control.Notifications;
import org.kordamp.ikonli.javafx.FontIcon;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.BrowserService;
import org.unigrid.janus.model.Transaction;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.rpc.entity.ListTransactions;
import org.unigrid.janus.model.TransactionList;
import org.unigrid.janus.model.TransactionList.LoadReport;

@ApplicationScoped
public class TransactionsController implements Initializable, PropertyChangeListener {
	@Inject private BrowserService browser;
	@Inject private DebugService debug;
	@Inject private RPCService rpc;
	@Inject private TransactionList transactionList;
	@Inject private Wallet wallet;

	@FXML private TableView tblTransactions;
	@FXML private TableColumn colTransDate;
	@FXML private TableColumn colTransType;
	@FXML private TableColumn colTransAddress;
	@FXML private TableColumn colTransAmount;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		debug.log("Initializing transactions");

		transactionList.addPropertyChangeListener(this);
		wallet.addPropertyChangeListener(this);
		setupTransactions();
	}

	private void setupTransactions() {
		try {
			colTransDate.setCellValueFactory(cell -> {
				long time = ((CellDataFeatures<Transaction, String>) cell).getValue().getTime();
				Date date = new Date(time * 1000L);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

				return new ReadOnlyStringWrapper(sdf.format(date));
			});

			colTransType.setCellValueFactory(cell -> {
				final Transaction transaction = ((CellDataFeatures<Transaction, Hyperlink>) cell).getValue();
				Button btn = new Button();
				int confrimations = transaction.getConfirmations();

				btn.setTooltip(new Tooltip(transaction.getCategory()));

				btn.setOnAction(e -> {
					browser.navigateTransaction(transaction.getTxId());
				});

				FontIcon fontIcon = new FontIcon("fas-wallet");

				if (transaction.isGenerated()) {
					if (transaction.getGeneratedfrom().equals("stake")) {
						fontIcon = new FontIcon("fas-coins");
						fontIcon.setIconColor(setColor(255, 140, 0, confrimations));
					} else {
						fontIcon = new FontIcon("fas-cubes");
						fontIcon.setIconColor(setColor(104, 197, 255, confrimations));
					}
					btn.setTooltip(new Tooltip(transaction.getGeneratedfrom()));
				} else if (transaction.getCategory().equals("send")
					|| transaction.getCategory().equals("fee")) {
					fontIcon = new FontIcon("fas-arrow-right");
					fontIcon.setIconColor(setColor(255, 0, 0, confrimations));
				} else if (transaction.getCategory().equals("receive")) {
					fontIcon = new FontIcon("fas-arrow-left");
					fontIcon.setIconColor(setColor(48, 186, 69, confrimations));
				}

				btn.setGraphic(fontIcon);
				return new ReadOnlyObjectWrapper(btn);
			});

			colTransAddress.setCellValueFactory(cell -> {
				final Hyperlink link = new Hyperlink();
				final Transaction transaction = ((CellDataFeatures<Transaction, Hyperlink>) cell).getValue();

				link.setText(transaction.getAddress());

				link.setOnAction(e -> {
					if (e.getTarget().equals(link)) {
						final String baseUrl = "https://explorer.unigrid.org/address/";
						browser.navigate(baseUrl + transaction.getAddress());
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

			colTransAmount.setCellValueFactory(cell -> {
				final Transaction transaction = ((CellDataFeatures<Transaction, String>) cell).getValue();
				double amount = transaction.getAmount();

				if (transaction.getCategory().equals("send")) {
					amount += transaction.getFee();
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

	public void loadTransactions(int page) {
		debug.log("Loading transactions");
		ListTransactions trans = rpc.call(new ListTransactions.Request(page * 100, 100),
			ListTransactions.class);
		transactionList.setTransactions(trans, 0);
	}

	private ScrollBar getVerticalScrollbar(TableView<?> table) {
		ScrollBar result = null;

		for (Node n : table.lookupAll(".scroll-bar")) {
			if (n instanceof ScrollBar) {
				ScrollBar bar = (ScrollBar) n;

				if (bar.getOrientation().equals(Orientation.VERTICAL)) {
					result = bar;
				}
			}
		}

		return result;
	}

	private void scrolled(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
		double value = newValue.doubleValue();
		ScrollBar bar = getVerticalScrollbar(tblTransactions);

		if (value == bar.getMax()) {
			debug.log("Adding new transactions.");
			LoadReport report = transactionList.loadTransactions(40);
			bar.setValue(value * report.getOldSize() / report.getNewSize());
		}
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals(transactionList.TRANSACTION_LIST)) {
			debug.log("Transactions list changed");

			tblTransactions.setItems(transactionList.getTransactions());
			ScrollBar bar = getVerticalScrollbar(tblTransactions);

			debug.log(String.format("Was scrollbar found: %b", (bar != null)));

			if (bar != null) {
				bar.valueProperty().addListener(this::scrolled);
			}
		}
		// if balance changes, load the transactions.
		if (event.getPropertyName().equals(wallet.BALANCE_PROPERTY)) {
			if (transactionList.getTransactions().size() == 0) {
				loadTransactions(0);
			}
		}

		if (event.getPropertyName().equals(wallet.TRANSACTION_COUNT)) {
			loadTransactions(0);
		}
	}
}
