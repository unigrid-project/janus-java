/*
    The Janus Wallet
    Copyright © 2022 The Unigrid Foundation

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

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import org.unigrid.janus.model.AddressListModel;
import org.unigrid.janus.model.Gridnode;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.rpc.entity.ListAddressGroupings;
import org.unigrid.janus.model.rpc.entity.ListReceivedByAddress;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.WindowService;

public class AddressController implements Initializable, PropertyChangeListener {
	private static DebugService debug = new DebugService();
	private static RPCService rpc = new RPCService();
	private static Wallet wallet = new Wallet();
	private static AddressListModel addresses = new AddressListModel();
	private static WindowService window = new WindowService();

	@FXML
	private TableView tblAddresses;
	@FXML
	private TableColumn colAddressAccount;
	@FXML
	private TableColumn colAddress;
	@FXML
	private TableColumn colAddressBalance;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		setupAddressList();
		window.setAddressController(this);
		wallet.addPropertyChangeListener(this);
		addresses.addPropertyChangeListener(this);
	}

	private void setupAddressList() {
		try {
			colAddressAccount.setCellValueFactory(
				new PropertyValueFactory<Gridnode, String>("account"));
			colAddress.setCellValueFactory(
				new PropertyValueFactory<Gridnode, String>("address"));
			colAddressBalance.setCellValueFactory(
				new PropertyValueFactory<Gridnode, String>("amount"));

		} catch (Exception e) {
			debug.log(String.format("ERROR: (setup address table) %s", e.getMessage()));
		}
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals(addresses.ADDRESS_LIST)) {
			debug.log("ADDRESS_LIST change");
			tblAddresses.setItems(addresses.getAddresses());
		}
	}

	public void loadAddresses() {
		ListReceivedByAddress addresses = rpc.call(new ListReceivedByAddress.Request(), ListReceivedByAddress.class);
		Jsonb jsonb = JsonbBuilder.create();
		String result = String.format("Address result: %s", jsonb.toJson(addresses.getResult()));
		debug.log(result);
	}

	public void loadGroupings() {
		ListAddressGroupings result = rpc.call(new ListAddressGroupings.Request(), ListAddressGroupings.class);
		addresses.setAddresses(result);
		Jsonb jsonb = JsonbBuilder.create();
		String str = String.format("Address groupings result: %s", jsonb.toJson(result));
		debug.log(str);
	}

	@FXML
	private void onLoadPressed(MouseEvent e) {
		debug.log("Calling address list refresh");
		loadAddresses();
	}

	@FXML
	private void onLoadAddressGroupings(MouseEvent e) {
		loadGroupings();
	}
}
