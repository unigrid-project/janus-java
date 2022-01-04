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
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.WindowService;
// import org.unigrid.janus.model.rpc.entity.NewAddress;
import org.unigrid.janus.model.rpc.entity.ListTransactions.Transaction;
import javafx.scene.control.Label;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import org.unigrid.janus.model.Wallet;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;

public class MainWindowController implements Initializable, PropertyChangeListener {
	private static DebugService debug = new DebugService();
	private static RPCService rpc = new RPCService();
	private static Wallet wallet = new Wallet();
	private static WindowService window = new WindowService();
	private final ObservableList<Transaction> walletTransactionData = 
		FXCollections.observableArrayList(
			new Transaction("Test1", "blah", "received", 1.0, 1234567),
			new Transaction("Test2", "blah", "received", 1.0, 1234567),
			new Transaction("Test3", "blah", "received", 1.0, 1234567)
		);

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		/* Empty on purpose */
		wallet.addPropertyChangeListener(this);
		setupWalletTransactions();
	}

	private void setupWalletTransactions() {
		try {
			TableView tblWalletTrans = (TableView) window.lookup("tblWalletTrans");
			TableColumn colWalletTransDate = (TableColumn) window.lookup("colWalletTransDate");
			TableColumn colWalletTransType = (TableColumn) window.lookup("colWalletTransType");
			TableColumn colWalletTransAddress = (TableColumn) window.lookup("colWalletTransAddress");
			TableColumn colWalletTransAmount = (TableColumn) window.lookup("colWalletTransAmount");
	        colWalletTransDate.setCellValueFactory(
                new PropertyValueFactory<Transaction, int>("time"));
	        colWalletTransType.setCellValueFactory(
                new PropertyValueFactory<Transaction, String>("category"));
	        colWalletTransAddress.setCellValueFactory(
                new PropertyValueFactory<Transaction, String>("account"));
	        colWalletTransAmount.setCellValueFactory(
                new PropertyValueFactory<Transaction, double>("amount"));
	        tblWalletTrans.setItems(walletTransactionData);
		} catch (Exception e) {
			debug.log(String.format("ERROR: (setup wallet table) %s", e.getMessage()));
		}
	}

	@FXML
	private void onGetAddress(MouseEvent event) {
		debug.log("Get address clicked!");
		// debug.log(rpc.callToJson(new NewAddress.Request("Wilcokat007")));
	}

	@FXML
	private void onWalletTap(MouseEvent event) {
		try {
			ToggleButton btnWallet = (ToggleButton) window.lookup("btnWallet");
			ToggleButton btnTransactions = (ToggleButton) window.lookup("btnTransactions");
			ToggleButton btnNodes = (ToggleButton) window.lookup("btnNodes");
			btnTransactions.setSelected(false);
			btnNodes.setSelected(false);
			btnWallet.setSelected(true);
			VBox pnlWallet = (VBox) window.lookup("pnlWallet");
			VBox pnlTransactions = (VBox) window.lookup("pnlTransactions");
			VBox pnlNodes = (VBox) window.lookup("pnlNodes");
			pnlTransactions.setVisible(false);
			pnlNodes.setVisible(false);
			pnlWallet.setVisible(true);
			debug.log("Wallet clicked!");
		} catch (Exception e) {
			debug.log(String.format("ERROR: (wallet click) %s", e.getMessage()));
		}
	}

	@FXML
	private void onTransactionsTap(MouseEvent event) {
		try {
			ToggleButton btnWallet = (ToggleButton) window.lookup("btnWallet");
			ToggleButton btnTransactions = (ToggleButton) window.lookup("btnTransactions");
			ToggleButton btnNodes = (ToggleButton) window.lookup("btnNodes");
			btnNodes.setSelected(false);
			btnWallet.setSelected(false);
			btnTransactions.setSelected(true);
			VBox pnlWallet = (VBox) window.lookup("pnlWallet");
			VBox pnlTransactions = (VBox) window.lookup("pnlTransactions");
			VBox pnlNodes = (VBox) window.lookup("pnlNodes");
			pnlWallet.setVisible(false);
			pnlNodes.setVisible(false);
			pnlTransactions.setVisible(true);
			debug.log("Transactions clicked!");
		} catch (Exception e) {
			debug.log(String.format("ERROR: (transactions click) %s", e.getMessage()));
		}
	}

	@FXML
	private void onNodesTap(MouseEvent event) {
		try {
			ToggleButton btnWallet = (ToggleButton) window.lookup("btnWallet");
			ToggleButton btnTransactions = (ToggleButton) window.lookup("btnTransactions");
			ToggleButton btnNodes = (ToggleButton) window.lookup("btnNodes");
			btnWallet.setSelected(false);
			btnTransactions.setSelected(false);
			btnNodes.setSelected(true);
			VBox pnlWallet = (VBox) window.lookup("pnlWallet");
			VBox pnlTransactions = (VBox) window.lookup("pnlTransactions");
			VBox pnlNodes = (VBox) window.lookup("pnlNodes");
			pnlWallet.setVisible(false);
			pnlTransactions.setVisible(false);
			pnlNodes.setVisible(true);
			debug.log("Nodes clicked!");
		} catch (Exception e) {
			debug.log(String.format("ERROR: (nodes click) %s", e.getMessage()));
		}
	}

	public void propertyChange(PropertyChangeEvent event) {
		Stage stage = window.getStage();
		debug.log("Main Window change fired!");
		debug.log(event.getPropertyName());
		debug.log(String.format("Value: %.8f", (double) event.getNewValue()));
		if (event.getPropertyName().equals(wallet.BALANCE_PROPERTY)) {
			Label lblBalance = (Label) stage.getScene().lookup("#lblBalance");
			lblBalance.setText(String.format("%.8fugd", (double) event.getNewValue()));
		}
	}
}
