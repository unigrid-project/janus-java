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

package org.unigrid.janus.view;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Data;
import org.unigrid.janus.model.HedgehogConfig;
import org.unigrid.janus.model.cdi.Eager;

@Eager
@Data
@ApplicationScoped
public class NetworkSelection implements Window{
	
	@FXML
	private ChoiceBox<String> cboxHedgehogNetwork;
	
	@Inject
	private Stage stageSplash;
	
	@PostConstruct
	private void init() {
		stageSplash.centerOnScreen();
		stageSplash.initStyle(StageStyle.UNDECORATED);
		stageSplash.setResizable(false);
		ObservableList<String> list = FXCollections.observableArrayList();
		list.add("Mainnet");
		list.add("Testnet");
		list.add("Devnet");
		cboxHedgehogNetwork.setItems(list);
	}

	@Override
	public void show() {
		try {
			stageSplash.show();
		} catch (Exception e) {
			AlertDialog.open(Alert.AlertType.ERROR, e.getMessage());
		}
	}

	@Override
	public void hide() {
		stageSplash.hide();
	}
	
}
