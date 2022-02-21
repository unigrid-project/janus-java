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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javafx.beans.value.ObservableValue;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.WindowService;
import org.unigrid.janus.model.Transaction;
import org.unigrid.janus.model.rpc.entity.ListTransactions;
import org.unigrid.janus.model.TransactionList;
import org.unigrid.janus.model.TransactionList.LoadReport;

public class TransactionsController implements Initializable, PropertyChangeListener {
	private static DebugService debug = new DebugService();
	private static RPCService rpc = new RPCService();
	private static TransactionList transList = new TransactionList();
	private static WindowService window = new WindowService();

	// transactions table
	@FXML private TableView tblTransactions;
	@FXML private TableColumn colTransDate;
	@FXML private TableColumn colTransType;
	@FXML private TableColumn colTransAddress;
	@FXML private TableColumn colTransAmount;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		/* Empty on purpose */
		debug.log("Initializing transactions");
		window.setTransactionsController(this);
		transList.addPropertyChangeListener(this);
		setupTransactions();
	}

	public void onShown() {
		try {
			debug.log("Loading transactions");
			loadTransactions(0);
			ScrollBar bar = getVerticalScrollbar(tblTransactions);
			debug.log(String.format("Was scrollbar found: %b", (bar != null)));
			if (bar != null) {
				bar.valueProperty().addListener(this::scrolled);
			}
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
			colTransAddress.setCellValueFactory(
				new Callback<CellDataFeatures<Transaction, Hyperlink>, ObservableValue<Hyperlink>>() {
					public ObservableValue<Hyperlink> call(CellDataFeatures<Transaction, Hyperlink> t) {
						Hyperlink link = new Hyperlink();
						Transaction trans = t.getValue();
						link.setText(trans.getAddress());
						link.setOnAction(new EventHandler<ActionEvent>() {
							@Override
							public void handle(ActionEvent e) {
								window.browseURL("https://explorer"
												+ ".unigrid.org/address/"
												+ trans.getAddress());
							}
						});
						return new ReadOnlyObjectWrapper(link);
					}
				});
			colTransAmount.setCellValueFactory(
				new PropertyValueFactory<Transaction, Double>("amount"));
		} catch (Exception e) {
			debug.log(String.format("ERROR: (setup wallet table) %s", e.getMessage()));
		}
	}

	public void loadTransactions(int page) {
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
		debug.log("Scrolled to " + value);
		ScrollBar bar = getVerticalScrollbar(tblTransactions);
		if (value == bar.getMax()) {
			debug.log("Adding new transactions.");
			// TODO: put logic in to modify scroll position
			LoadReport report = transList.loadTransactions(40);
			bar.setValue(value * report.getOldSize() / report.getNewSize());
		}
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals(transList.TRANSACTION_LIST)) {
			debug.log("Transactions list changed");
			tblTransactions.setItems(transList.getTransactions());
		}
	}
}
