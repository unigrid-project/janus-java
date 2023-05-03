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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.HostServices;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.unigrid.bootstrap.App;
import static org.unigrid.bootstrap.App.startupState;
import org.unigrid.bootstrap.UpdateView;
import org.update4j.OS;

public class DebugViewController implements Initializable {

	@FXML private Button closeButton;
	@FXML private Label txtRemoveDepndsDone;
	@FXML private Label txtRemoveDebug;
	@FXML private Label txtRemoveBlockChainData;
	@FXML private TextField txtConfigURL;
	@FXML private Button btnOpenDebug;
	@FXML private Label txtOpenDebugStatus;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		String basePath = UpdateView.getUnigridHome();
		File file = new File(basePath + "debug.log");
		if (!file.exists()) {
			btnOpenDebug.setDisable(true);
			txtOpenDebugStatus.setText("Could not find debug.log");
		}
	}

	@FXML
	public void onCLose(ActionEvent event) {
		Stage stage = (Stage) closeButton.getScene().getWindow();
		String configURL = txtConfigURL.getText();
		if (!configURL.equals("")) {
			UpdateView.getInstance().setConfigURL(configURL);
		}
		startupState = App.state.NORMAL;
		stage.close();
	}

	@FXML
	public void onResetDepends(ActionEvent event) {
		if (UpdateView.getInstance().removeDepends()) {
			txtRemoveDepndsDone.setText("\u2713");
			txtRemoveDepndsDone.setVisible(true);
		}
	}

	@FXML
	public void onRemoveDebug(ActionEvent event) {
		if (UpdateView.getInstance().removeDebug()) {
			btnOpenDebug.setDisable(true);
			txtOpenDebugStatus.setText("debug.log is removed");

			txtRemoveDebug.setText("\u2713");
			txtRemoveDebug.setVisible(true);
			
		}
	}

	@FXML
	public void onRemoveBlockChainData(ActionEvent event) {
		if (UpdateView.getInstance().removeBlockChainData()) {
			txtRemoveBlockChainData.setText("\u2713");
			txtRemoveBlockChainData.setVisible(true);
		}
	}

	@FXML
	public void onOpenDebug(ActionEvent event) throws IOException {
		HostServices services = UpdateView.getInstance().getHostServices();
		String path = UpdateView.getUnigridHome() + "debug.log";
		switch(OS.CURRENT) {
			case LINUX -> {
				services.showDocument(path);
			}
			case MAC -> {
				Process p = new ProcessBuilder()
					.command("open", "-t", path)
					.directory(new File(UpdateView.getUnigridHome()))
					.start();
			}
			case WINDOWS -> services.showDocument(path);
			case OTHER -> services.showDocument(path);
		}
	}
}
