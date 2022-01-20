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
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.WindowService;
import org.unigrid.janus.model.Wallet;

public class SettingsController implements Initializable {
	private static DebugService debug = new DebugService();
	private static RPCService rpc = new RPCService();
	private static Wallet wallet = new Wallet();
	private static WindowService window = new WindowService();

	private static final int TAB_SETTINGS_GENERAL = 1;
	private static final int TAB_SETTINGS_DISPLAY = 2;
	private static final int TAB_SETTINGS_PASSPHRASE = 3;
	private static final int TAB_SETTINGS_EXPORT = 4;
	private static final int TAB_SETTINGS_DEBUG = 5;

	// settings navigation
	@FXML private VBox pnlSetGeneral;
	@FXML private VBox pnlSetDisplay;
	@FXML private VBox pnlSetPassphrase;
	@FXML private VBox pnlSetExport;
	@FXML private VBox pnlSetDebug;
	// passphrase
	@FXML private Button btnUpdatePassphrase;
	@FXML private TextArea taPassphrase;
	@FXML private TextArea taRepeatPassphrase;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		/* Empty on purpose */
	}

	private void settingSelected(int tab) {
		pnlSetGeneral.setVisible(false);
		pnlSetDisplay.setVisible(false);
		pnlSetPassphrase.setVisible(false);
		pnlSetExport.setVisible(false);
		pnlSetDebug.setVisible(false);
		switch (tab) {
			case TAB_SETTINGS_GENERAL: pnlSetGeneral.setVisible(true);
						break;
			case TAB_SETTINGS_DISPLAY: pnlSetDisplay.setVisible(true);
						break;
			case TAB_SETTINGS_PASSPHRASE: pnlSetPassphrase.setVisible(true);
						break;
			case TAB_SETTINGS_EXPORT: pnlSetExport.setVisible(true);
						break;
			case TAB_SETTINGS_DEBUG: pnlSetDebug.setVisible(true);
						break;
			default: pnlSetDebug.setVisible(true);
						break;
		}

	}

	@FXML
	private void onSetGeneralTap(MouseEvent event) {
		settingSelected(TAB_SETTINGS_GENERAL);
	}

	@FXML
	private void onSetDisplayTap(MouseEvent event) {
		settingSelected(TAB_SETTINGS_DISPLAY);
	}

	@FXML
	private void onSetPassphraseTap(MouseEvent event) {
		settingSelected(TAB_SETTINGS_PASSPHRASE);
	}

	@FXML
	private void onSetExportTap(MouseEvent event) {
		settingSelected(TAB_SETTINGS_EXPORT);
	}

	@FXML
	private void onSetDebugTap(MouseEvent event) {
		settingSelected(TAB_SETTINGS_DEBUG);
	}

	@FXML
	private void onLock(MouseEvent event) {
		debug.log("Update passphrase clicked!");
		try {
			Dialog<ButtonType> dialog = new Dialog<ButtonType>();
			dialog.setTitle("Confirmation");
			dialog.setHeaderText("Be sure that you have saved the passphrase.\n"
								  + "Are you sure you're ready to lock your wallet now?\n"
				 				  + "This cannot be undone without your passphrase.");
			ButtonType btnYes = new ButtonType("Yes", ButtonData.YES);
			ButtonType btnNo = new ButtonType("No", ButtonData.NO);
			dialog.getDialogPane().getButtonTypes().add(btnYes);
			dialog.getDialogPane().getButtonTypes().add(btnNo);
			dialog.getDialogPane().getStylesheets().add("/org/unigrid/janus/view/main.css");
			Optional<ButtonType> response = dialog.showAndWait();
			debug.log(String.format("Response: %s", response.get()));
			if (response.isPresent()) {
				if (response.get() == btnYes) {
					taPassphrase.setText("");
					taRepeatPassphrase.setText("");
					taRepeatPassphrase.setBorder(new Border(
						new BorderStroke(Color.TRANSPARENT,
							BorderStrokeStyle.SOLID,
							new CornerRadii(3),
							new BorderWidths(1))));
					wallet.setLocked(true);
				}
			}
		} catch (Exception e) {
			debug.log(String.format("ERROR: (passphrase update) %s", e.getMessage()));
		}
	}

	@FXML
	private void onRepeatPassphraseChange(KeyEvent event) {
		// debug.log("passphrase change event fired!");
		try {
			if (taPassphrase.getText().equals(taRepeatPassphrase.getText())) {
				taRepeatPassphrase.setBorder(new Border(
						new BorderStroke(Color.web("#1dab00"),
							BorderStrokeStyle.SOLID,
							new CornerRadii(3),
							new BorderWidths(1))));
				btnUpdatePassphrase.setDisable(false);
			} else {
				taRepeatPassphrase.setBorder(new Border(
						new BorderStroke(Color.RED,
							BorderStrokeStyle.SOLID,
							new CornerRadii(3),
							new BorderWidths(1))));
				btnUpdatePassphrase.setDisable(true);
			}
		} catch (Exception e) {
			debug.log(String.format("ERROR: (passphrase change) %s", e.getMessage()));
		}
	}
}
