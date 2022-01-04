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
import javafx.scene.layout.VBox;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.WindowService;
// import org.unigrid.janus.model.rpc.entity.NewAddress;

public class MainWindowController implements Initializable {
	private static DebugService debug = new DebugService();
	private static RPCService rpc = new RPCService();
	private static WindowService window = new WindowService();

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		/* Empty on purpose */
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
}
