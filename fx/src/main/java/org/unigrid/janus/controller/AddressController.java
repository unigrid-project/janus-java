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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.util.Callback;
import org.apache.commons.lang3.SystemUtils;
import org.controlsfx.control.Notifications;
import org.kordamp.ikonli.javafx.FontIcon;
import org.unigrid.janus.model.AddressListModel;
import org.unigrid.janus.model.Address;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.rpc.entity.GetNewAddress;
import org.unigrid.janus.model.rpc.entity.ListAddressBalances;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.WindowService;

@ApplicationScoped
public class AddressController implements Initializable, PropertyChangeListener {
	@Inject private DebugService debug;
	@Inject private RPCService rpc;
	@Inject private Wallet wallet;

	private final AddressListModel addresses = new AddressListModel();
	private final WindowService window = WindowService.getInstance();
	private final Clipboard clipboard = Clipboard.getSystemClipboard();
	private final ClipboardContent content = new ClipboardContent();

	@FXML private TableView tblAddresses;
	@FXML private TableColumn colAddress;
	@FXML private TableColumn colAddressBalance;
	@FXML private HBox newAddressDisplay;
	@FXML private Text addressDisplay;
	@FXML private CheckBox chkAddress;
	@FXML private CheckBox chkAmountSort;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		wallet.addPropertyChangeListener(this);
		addresses.addPropertyChangeListener(this);

		setupAddressList();

		addresses.setSelected(chkAddress.isSelected());
		addresses.setSorted(chkAmountSort.isSelected());
		// addButtonToTable();
	}

	private void setupAddressList() {
		try {
			colAddress.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Address, Hyperlink>,
				ObservableValue<Hyperlink>>() {

				public ObservableValue<Hyperlink> call(TableColumn.CellDataFeatures<Address, Hyperlink> t) {

					Address address = t.getValue();
					String text = address.getAddress();

					Hyperlink link = new Hyperlink();
					link.setText(text);

					link.setOnAction(e -> {
						if (e.getTarget().equals(link)) {
							// TODO: Not a proper setter!
							window.browseURL("https://explorer.unigrid.org/address/"
								+ address.getAddress()
							);
						}
					});

					Button btn = new Button();
					FontIcon fontIcon = new FontIcon("fas-clipboard");
					fontIcon.setIconColor(Paint.valueOf("#FFFFFF"));
					btn.setGraphic(fontIcon);

					btn.setOnAction(e -> {
						final Clipboard cb = Clipboard.getSystemClipboard();
						final ClipboardContent content = new ClipboardContent();
						content.putString(address.getAddress());
						cb.setContent(content);
						if (SystemUtils.IS_OS_MAC_OSX) {
							Notifications
								.create()
								.title("Address copied to clipboard")
								.text(address.getAddress())
								.position(Pos.TOP_RIGHT)
								.showInformation();
						} else {
							Notifications
								.create()
								.title("Address copied to clipboard")
								.text(address.getAddress())
								.showInformation();
						}
					});

					link.setGraphic(btn);
					link.setAlignment(Pos.CENTER_RIGHT);
					return new ReadOnlyObjectWrapper(link);
				}
			});

			colAddressBalance.setCellValueFactory(new PropertyValueFactory<Address, String>("amount"));

		} catch (Exception e) {
			debug.log(String.format("ERROR: (setup address table) %s", e.getMessage()));
		}
	}

	private void addButtonToTable() {
		TableColumn<Address, Void> colBtn = new TableColumn("Copy");
		colBtn.setStyle("-fx-alignment: CENTER;");
		Callback<TableColumn<Address, Void>, TableCell<Address, Void>> cellFactory;

		cellFactory = (final TableColumn<Address, Void> param) -> {
			final TableCell<Address, Void> cell = new TableCell<Address, Void>() {
				private final Button btn = new Button();

				{
					FontIcon fontIcon = new FontIcon("fas-clipboard");
					fontIcon.setIconColor(Paint.valueOf("#FFFFFF"));
					btn.setGraphic(fontIcon);

					btn.setOnAction(e -> {
						Address data = getTableView().getItems().get(getIndex());
						copyToClipboard(data.getAddress());
					});
				}

				@Override
				public void updateItem(Void item, boolean empty) {
					super.updateItem(item, empty);

					if (empty) {
						setGraphic(null);
					} else {
						setGraphic(btn);
					}
				}
			};
			return cell;
		};

		colBtn.setCellFactory(cellFactory);
		// TODO: tblAddresses.getColumns().add(colBtn);
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals(addresses.ADDRESS_LIST)) {
			tblAddresses.setItems(addresses.getAddresses());
		}

		if (event.getPropertyName().equals(wallet.STATUS_PROPERTY)) {
			loadAddresses();
		}

		if (event.getPropertyName().equals(wallet.TRANSACTION_COUNT)) {
			loadAddresses();
		}
	}

	public void loadAddresses() {
		// try {
		ListAddressBalances addr = rpc.call(new ListAddressBalances.Request(), ListAddressBalances.class);
		addresses.setAddresses(addr);
		// } catch (Exception e) {
		// debug.print("loadAddresses " + e.getCause().getMessage().toString(),
		// AddressController.class.getSimpleName());
		// }
	}

	@FXML
	private void onLoadPressed(MouseEvent e) {
		// debug.log("Calling address list refresh");
		loadAddresses();
	}

	@FXML
	private void onGenerateAddressClicked(MouseEvent e) {
		GetNewAddress newAddress = rpc.call(new GetNewAddress.Request(""), GetNewAddress.class);
		addressDisplay.setText(newAddress.getResult().toString());
		newAddressDisplay.setVisible(true);
		loadAddresses();
	}

	@FXML
	private void onClearAddressClicked(MouseEvent event) {
		newAddressDisplay.setVisible(false);
		addressDisplay.setText("");
	}

	@FXML
	private void onCopyToClipboardClicked(MouseEvent event) {
		copyToClipboard(addressDisplay.getText());
	}

	private void copyToClipboard(String address) {
		content.putString(address);
		clipboard.setContent(content);
		Notifications
			.create()
			.title("Address copied to clipboard")
			.text(address)
			.showInformation();
	}

	@FXML
	private void onChecboxChange(MouseEvent event) {
		addresses.setSelected(chkAddress.isSelected());
		addresses.setSorted(chkAmountSort.isSelected());
		loadAddresses();
	}

	@FXML
	private void onSortChange(MouseEvent event) {
		addresses.setSelected(chkAddress.isSelected());
		addresses.setSorted(chkAmountSort.isSelected());
		loadAddresses();
	}
}
