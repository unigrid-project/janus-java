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

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

@ApplicationScoped
public class WalletSetupController {

	public enum Window {
		MNEMONIC,
		PASSWORD,
		PROMTMNEMONIC,
		SHUFFLEMNEMONIC
	}

	@FXML
	private Stage createWallet;
	@FXML
	private AnchorPane centerView;
	private Pane mnemonic;
	private Pane password;
	private Pane promtMnemonic;
	private Pane shuffleMnemonic;

	private Window currentWindow;

	@PostConstruct
	public void init() {
		try {
			mnemonic = FXMLLoader.load(getClass().getResource("/view/mnemonic.fxml"));
			password = FXMLLoader.load(getClass().getResource("/view/password.fxml"));
			promtMnemonic = FXMLLoader.load(getClass().getResource("/view/promtMnemonic.fxml"));
			shuffleMnemonic = FXMLLoader.load(getClass().getResource("/view/shuflleMnemonic.fxml"));
			centerView.getChildren().addAll(mnemonic, password, promtMnemonic, shuffleMnemonic);

		} catch (IOException e) {
			//close application if it fails.
		}
	}

	@FXML
	public void onNext(ActionEvent ev) {
		switch (currentWindow) {
			case PASSWORD:
				password.setVisible(false);
				promtMnemonic.setVisible(true);
			case PROMTMNEMONIC:
				promtMnemonic.setVisible(false);
				mnemonic.setVisible(true);
			case MNEMONIC:
				mnemonic.setVisible(false);
				shuffleMnemonic.setVisible(true);
		}
	}

	@FXML
	public void onBack(ActionEvent ev) {
		//
	}

	public void show() {
		createWallet.show();
		password.setVisible(true);
	}
}
