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
import javafx.util.Callback;
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
import org.unigrid.janus.model.service.WindowService;
import org.unigrid.janus.model.Transaction;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.rpc.entity.ListTransactions;
import org.unigrid.janus.model.TransactionList;
import org.unigrid.janus.model.TransactionList.LoadReport;

@ApplicationScoped
public class TransactionsController implements Initializable, PropertyChangeListener {
	private static DebugService debug = new DebugService();
	private static RPCService rpc = new RPCService();
	private static TransactionList transList = new TransactionList();

	private Wallet wallet;

	private static WindowService window = WindowService.getInstance();

	// transactions table
	@FXML private TableView tblTransactions;
	@FXML private TableColumn colTransDate;
	@FXML private TableColumn colTransType;
	@FXML private TableColumn colTransAddress;
	@FXML private TableColumn colTransAmount;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		/* Empty on purpose */
		wallet = window.getWallet();
		debug.log("Initializing transactions");
		window.setTransactionsController(this);
		transList.addPropertyChangeListener(this);
		wallet.addPropertyChangeListener(this);
		setupTransactions();
	}

	public void onShown() {
		try {
			// TODO: anything to render after the app is shown (not transactions tab)
			debug.log("Transactions shown called.");
		} catch (Exception e) {
			debug.log(String.format("ERROR: (transactions shown) %s", e.getMessage()));
		}
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
				new Callback<CellDataFeatures<Transaction, Hyperlink>, ObservableValue<Hyperlink>>() {
					public ObservableValue<Hyperlink> call(CellDataFeatures<Transaction, Hyperlink> t) {
						Transaction trans = t.getValue();
						Button btn = new Button();
						btn.setTooltip(new Tooltip(trans.getCategory()));
						btn.setOnAction(new EventHandler<ActionEvent>() {
							@Override
							public void handle(ActionEvent e) {
								window.browseURL("https://explorer"
												+ ".unigrid.org/tx/"
												+ trans.getTxid());
							}
						});
						FontIcon fontIcon = new FontIcon("fas-wallet");
						if (trans.isGenerated()) {
							if (trans.getGeneratedfrom().equals("stake")) {
								fontIcon = new FontIcon("fas-coins");
								fontIcon.setIconColor(Color.ORANGE);
							} else {
								fontIcon = new FontIcon("fas-cubes");
								fontIcon.setIconColor(Paint.valueOf("#68C5FF"));
							}
							btn.setTooltip(new Tooltip(trans.getGeneratedfrom()));
						} else if (trans.getCategory().equals("send") ||
								trans.getCategory().equals("fee")) {
							fontIcon = new FontIcon("fas-arrow-right");
							fontIcon.setIconColor(Paint.valueOf("#FF0000"));
						} else if (trans.getCategory().equals("receive")) {
							fontIcon = new FontIcon("fas-arrow-left");
							fontIcon.setIconColor(Paint.valueOf("#00FF00"));
						}
						btn.setGraphic(fontIcon);

						return new ReadOnlyObjectWrapper(btn);
					}
				});
			colTransAddress.setCellValueFactory(
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
						// link.setContentDisplay(ContentDisplay.RIGHT);
						return new ReadOnlyObjectWrapper(link);
					}
				});
			colTransAmount.setCellValueFactory(
				new Callback<CellDataFeatures<Transaction, String>, ObservableValue<String>>() {
					public ObservableValue<String> call(CellDataFeatures<Transaction, String> t) {
						Transaction trans = t.getValue();
						double amount = trans.getAmount();
						if (trans.getCategory().equals("send")) {
							amount += trans.getFee();
						}
						return new ReadOnlyStringWrapper(String.format("%.8f", amount));
					}
				}
			);
		} catch (Exception e) {
			debug.log(String.format("ERROR: (setup wallet table) %s", e.getMessage()));
		}
	}

	public void loadTransactions(int page) {
		debug.log("Loading transactions");
		ListTransactions trans = rpc.call(new ListTransactions.Request(page * 100, 100),
												 ListTransactions.class);
		transList.setTransactions(trans, 0);
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

	private void scrolled(ObservableValue<? extends Number> observable,
		Number oldValue,
		Number newValue) {
		double value = newValue.doubleValue();
		ScrollBar bar = getVerticalScrollbar(tblTransactions);
		if (value == bar.getMax()) {
			debug.log("Adding new transactions.");
			LoadReport report = transList.loadTransactions(40);
			bar.setValue(value * report.getOldSize() / report.getNewSize());
		}
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals(transList.TRANSACTION_LIST)) {
			debug.log("Transactions list changed");
			tblTransactions.setItems(transList.getTransactions());
			ScrollBar bar = getVerticalScrollbar(tblTransactions);
			debug.log(String.format("Was scrollbar found: %b", (bar != null)));
			if (bar != null) {
				bar.valueProperty().addListener(this::scrolled);
			}
		}
		// if balance changes, load the transactions.
		if (event.getPropertyName().equals(wallet.BALANCE_PROPERTY)) {
			if (transList.getTransactions().size() == 0) {
				loadTransactions(0);
			}
		}
		if (event.getPropertyName().equals(wallet.TRANSACTION_COUNT)) {
			loadTransactions(0);
		}
	}
}
