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
import javafx.stage.Stage;
import org.unigrid.janus.model.cdi.Eager;

@Eager
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
	private AnchorPane mnemonic;
	private AnchorPane password;
	private AnchorPane promtMnemonic;
	private AnchorPane shuffleMnemonic;

	private Window currentWindow;

	@PostConstruct
	public void init() {
		try {
			createWallet = FXMLLoader
				.load(getClass().getResource("/org/unigrid/janus/view/createnewwallet.fxml"));
			System.out.println("wallet setup init");
			mnemonic = (AnchorPane) FXMLLoader
				.load(getClass().getResource("/org/unigrid/janus/view/mnemonic.fxml"));
			password = (AnchorPane) FXMLLoader
				.load(getClass().getResource("/org/unigrid/janus/view/password.fxml"));
			promtMnemonic = (AnchorPane) FXMLLoader
				.load(getClass().getResource("/org/unigrid/janus/view/promtMnemonic.fxml"));
			shuffleMnemonic = (AnchorPane) FXMLLoader
				.load(getClass().getResource("/org/unigrid/janus//view/shuffleMnemonic.fxml"));
			centerView.getChildren().addAll(mnemonic, password, promtMnemonic, shuffleMnemonic);
			password.setVisible(true);
			System.out.println("wallet setup initialize");
		} catch (IOException ex) {
			System.out.println("ERROR: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	@FXML
	public void onNext(ActionEvent ev) {
		if (currentWindow == null) {
			currentWindow = Window.PASSWORD;
		}
		System.out.println(currentWindow);
		switch (currentWindow) {
			case PASSWORD:
				password.setVisible(false);
				promtMnemonic.setVisible(true);
				currentWindow = Window.PROMTMNEMONIC;
			case PROMTMNEMONIC:
				promtMnemonic.setVisible(false);
				mnemonic.setVisible(true);
				currentWindow = Window.MNEMONIC;
			case MNEMONIC:
				mnemonic.setVisible(false);
				shuffleMnemonic.setVisible(true);
				currentWindow = Window.SHUFFLEMNEMONIC;
			case SHUFFLEMNEMONIC:
		}
	}

	@FXML
	public void onBack(ActionEvent ev) {
		switch (currentWindow) {
			case PASSWORD:
			case PROMTMNEMONIC:
			case MNEMONIC:
			case SHUFFLEMNEMONIC:
		}
	}

	@FXML
	public void onWriteByHand(ActionEvent ev) {
		//
	}

	@FXML
	public void onPrint(ActionEvent ev) {
		//
	}

	@FXML
	public void onHardwareWallet(ActionEvent ev) {
		//
	}

	public void show() {
		createWallet.show();
	}
}
