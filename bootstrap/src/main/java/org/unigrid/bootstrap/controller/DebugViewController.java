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

package org.unigrid.bootstrap.controller;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.unigrid.bootstrap.App;
import static org.unigrid.bootstrap.App.startupState;
import org.unigrid.bootstrap.UpdateView;

public class DebugViewController implements Initializable {

	@FXML private Button closeButton;
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		// Empty on purpuse
	}

	@FXML
	public void onCLose(ActionEvent event) {
		Stage stage = (Stage) closeButton.getScene().getWindow();
		startupState = App.state.NORMAL;
		stage.close();
	}

	@FXML
	public void onResetDepends(ActionEvent event) {
		UpdateView.getInstance().removeDepends();
	}
}
