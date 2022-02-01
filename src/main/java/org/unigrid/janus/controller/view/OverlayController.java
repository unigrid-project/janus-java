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
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.WindowService;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.rpc.entity.Info;
import org.unigrid.janus.model.rpc.entity.UnlockWallet;

public class OverlayController implements Initializable, PropertyChangeListener {
	private static DebugService debug = new DebugService();
	private static RPCService rpc = new RPCService();
	private static Wallet wallet = new Wallet();
	private static WindowService window = new WindowService();

	@FXML
	private GridPane pnlUnlock;
	@FXML
	private TextField passphraseInput;
	@FXML
	private Text errorTxt;
	@FXML
	private ImageView spinnerIcon;
	@FXML
	private Button submitBtn;
	@FXML
	private Text unlockCopy;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		/* Empty on purpose */
		window.setOverlayController(this);
		wallet.addPropertyChangeListener(this);
		pnlUnlock.setVisible(false);
	}

	public void startLockOverlay() {
		pnlUnlock.setVisible(true);
		submitBtn.setText("UNLOCK");
		unlockCopy.setText("Unlock your wallet by entering your passphrase and "
			+ "pressing the UNLOCK button.");
		wallet.setUnlockState(2);
	}

	public void startStakingOverlay() {
		wallet.setUnlockState(1);
		pnlUnlock.setVisible(true);
		submitBtn.setText("STAKE");
		unlockCopy.setText("Enable staking in your wallet by entering your passphrase and "
			+ "pressing the STAKE button.");
	}

	public void startUnlockForTimeOverlay() {
		debug.log("UNLOCK FOR TIME");
		wallet.setUnlockState(4);
		pnlUnlock.setVisible(true);
		submitBtn.setText("UNLOCK");
		unlockCopy.setText("Please enter your passphrase in order to perform this task. "
			+ "The wallet will automatically lock itself after 30 seconds.");
	}

	public void startUnlockForSendingOverlay() {
		debug.log("UNLOCK FOR SENDING");
		wallet.setUnlockState(3);
		pnlUnlock.setVisible(true);
		submitBtn.setText("SEND");
		unlockCopy.setText("Please enter your passphrase to send Unigrid tokens. "
			+ "If your wallet was staking you will need to enable again after the transaction completes.");
	}

	@FXML
	private void onCancelLockPressed(MouseEvent event) {
		closeUnlockOverlay();
	}

	@FXML
	private void onSubmitPassphrasePressed(MouseEvent event) {
		submitBtn.setDisable(true);
		Object[] sendArgs;
		long stakingStartTime = wallet.getStakingStartTime();
		switch (wallet.getUnlockState()) {
			case 1:
				sendArgs = new Object[]{passphraseInput.getText(), stakingStartTime, true};
				break;
			case 2:
				sendArgs = new Object[]{passphraseInput.getText(), 0};
				break;
			case 3:
			case 4:
				// unlock for 30 seconds only
				sendArgs = new Object[]{passphraseInput.getText(), 30};
				break;
			default:
				throw new AssertionError();
		}

		if (passphraseInput.getText() == "") {
			errorTxt.setText("Please enter a passphrase");
			submitBtn.setDisable(false);
		} else {
			final UnlockWallet call = rpc.call(
				new UnlockWallet.Request(sendArgs), UnlockWallet.class);
			Jsonb jsonb = JsonbBuilder.create();
			if (call.getError() != null) {
				final Info info = rpc.call(new Info.Request(), Info.class);
				wallet.setInfo(info);
				String result = call.getError().getMessage();
				if (result != null) {
					submitBtn.setDisable(false);
					debug.log(result);
					errorTxt.setText(result);
					passphraseInput.setText("");
				}
			} else {
				errorTxt.setText("Wallet unlocked!");
				passphraseInput.setText("");
				if (wallet.getUnlockState() == 3) {
					// send transaction
					window.getWalletController().sendTransactionAfterUnlock();
				}
				closeUnlockOverlay();
			}
		}
	}

	private void closeUnlockOverlay() {
		wallet.setUnlockState(0);
		unlockCopy.setText("");
		spinnerIcon.setVisible(false);
		errorTxt.setText("");
		passphraseInput.setText("");
		pnlUnlock.setVisible(false);
		submitBtn.setDisable(false);
		window.getMainWindowController().hideOverlay();
	}

	public void propertyChange(PropertyChangeEvent event) {
	}

}
