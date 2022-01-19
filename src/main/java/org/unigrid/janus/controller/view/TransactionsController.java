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
import javafx.util.Callback;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.ReadOnlyStringWrapper;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javafx.beans.value.ObservableValue;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.WindowService;
import org.unigrid.janus.model.Transaction;
import org.unigrid.janus.model.rpc.entity.ListTransactions;
import org.unigrid.janus.model.TransactionList;

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
		transList.addPropertyChangeListener(this);
		setupTransactions();
		Platform.runLater(() -> {
			try {
				debug.log("Loading transactions");
				loadTransactions(0);
			} catch (Exception e) {
				debug.log(String.format("ERROR: (trans init) %s", e.getMessage()));
			}
		});
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

	public void loadTransactions(int page) {
		ListTransactions trans = rpc.call(new ListTransactions.Request(page * 100, 100),
			                                     ListTransactions.class);
		transList.setTransactions(trans, 0);
		/* ObservableList<Transaction> transactions = FXCollections.observableArrayList();

		for (Transaction t : trans.getResult()) {
			transactions.add(t);
		}

		tblTransactions.setItems(transactions); */
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals(transList.TRANSACTION_LIST)) {
			debug.log("Transactions list changed");
			tblTransactions.setItems(transList.getTransactions());
		}
	}
}
