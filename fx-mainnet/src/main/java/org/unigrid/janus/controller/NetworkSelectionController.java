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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import org.unigrid.janus.model.cdi.Eager;
import org.unigrid.janus.view.NetworkSelection;

@Eager
@ApplicationScoped
public class NetworkSelectionController {

	@Inject
	private NetworkSelection networkSelection;
	
	@FXML
	private Button btnOk;
	
	@FXML
	private ChoiceBox<String> cboxHedgehogNetwork;
	
	public void onBtnPrimaryClicked(ActionEvent event) {
		String s = cboxHedgehogNetwork.getValue();
	}

}
