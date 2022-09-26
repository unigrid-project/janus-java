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
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.WindowService;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.rpc.entity.Info;
import org.unigrid.janus.model.rpc.entity.UnlockWallet;
import org.unigrid.janus.model.signal.State;

@ApplicationScoped
public class OverlayController implements Initializable, PropertyChangeListener {
	@Inject private DebugService debug;
	@Inject private RPCService rpc;
	@Inject private Wallet wallet;

	@Inject private Event<State> stateEvent;

	private static WindowService window = WindowService.getInstance();

	@FXML private GridPane pnlUnlock;
	@FXML private PasswordField passphraseInput;
	@FXML private Text errorTxt;
	@FXML private ImageView spinnerIcon;
	@FXML private Button submitBtn;
	@FXML private Text unlockCopy;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		window.setOverlayController(this);
		wallet.addPropertyChangeListener(this);
		// TODO: pnlUnlock.setVisible(false);
	}

	public void startLockOverlay() {
		pnlUnlock.setVisible(true);
		wallet.setUnlockState(2);
		submitBtn.setText("UNLOCK");
		Platform.runLater(() -> passphraseInput.requestFocus());

		unlockCopy.setText("Unlock your wallet by entering your passphrase and "
			+ "pressing the UNLOCK button."
		);
	}

	public void startStakingOverlay() {
		wallet.setUnlockState(1);
		pnlUnlock.setVisible(true);
		submitBtn.setText("STAKE");
		Platform.runLater(() -> passphraseInput.requestFocus());

		unlockCopy.setText("Enable staking in your wallet by entering your passphrase and "
			+ "pressing the STAKE button."
		);
	}

	public void startUnlockForTimeOverlay() {
		debug.log("UNLOCK FOR TIME");
		wallet.setUnlockState(4);
		pnlUnlock.setVisible(true);
		submitBtn.setText("UNLOCK");
		Platform.runLater(() -> passphraseInput.requestFocus());

		unlockCopy.setText("Please enter your passphrase in order to perform this task. "
			+ "The wallet will automatically lock itself after 30 seconds.");
	}

	public void startUnlockForSendingOverlay() {
		debug.log("UNLOCK FOR SENDING");
		wallet.setUnlockState(3);
		pnlUnlock.setVisible(true);
		submitBtn.setText("SEND");
		Platform.runLater(() -> passphraseInput.requestFocus());

		unlockCopy.setText("Please enter your passphrase to send Unigrid tokens. "
			+ "If your wallet was staking you will need to enable again after the transaction completes."
		);
	}

	public void startUnlockForGridnodeOverlay() {
		debug.log("UNLOCK FOR GRIDNODE");
		wallet.setUnlockState(4);
		pnlUnlock.setVisible(true);
		submitBtn.setText("START");
		Platform.runLater(() -> passphraseInput.requestFocus());

		unlockCopy.setText("Please enter your passphrase to enable your gridnodes. "
			+ "If your wallet was staking you will need to enable again after the task completes."
		);
	}

	public void startUnlockForDump() {
		debug.log("UNLOCK FOR DUMP");
		wallet.setUnlockState(5);
		pnlUnlock.setVisible(true);
		submitBtn.setText("EXPORT");
		Platform.runLater(() -> passphraseInput.requestFocus());

		unlockCopy.setText("Please enter your passphrase to export your private keys. "
			+ "If your wallet was staking you will need to enable again after the task completes."
		);
	}

	@FXML
	private void setKeyListern(KeyEvent ke) {
		if (ke.getCode() == KeyCode.ENTER) {
			submit();
		} else if (ke.getCode() == KeyCode.ESCAPE) {
			closeUnlockOverlay();
		}
	}

	@FXML
	private void onCancelLockPressed(MouseEvent event) {
		closeUnlockOverlay();
	}

	@FXML
	private void onSubmitPassphrasePressed(MouseEvent event) {
		submit();
	}

	private void submit() {
		submitBtn.setDisable(true);
		Object[] sendArgs;
		long stakingStartTime = wallet.getStakingStartTime();

		stateEvent.fire(State.builder().working(true).build());

		// TODO: What exactly do these numbers mean ? Please change this to an enum and explain it.
		switch (wallet.getUnlockState()) {
			case 1:
				sendArgs = new Object[] {
					passphraseInput.getText(), stakingStartTime, true
				};

				break;
			case 2:
				sendArgs = new Object[] {
					passphraseInput.getText(), 0
				};

				break;
			case 3:
			case 4:
			case 5:
				// unlock for 30 seconds only
				sendArgs = new Object[] {
					passphraseInput.getText(), 30
				};

				break;
			default:
				throw new AssertionError();
		}

		if (passphraseInput.getText().equals("")) {
			errorTxt.setText("Please enter a passphrase");
			submitBtn.setDisable(false);
			stateEvent.fire(State.builder().working(false).build());
		} else {
			try {
				final UnlockWallet call = rpc.call(new UnlockWallet.Request(sendArgs), UnlockWallet.class);
				Jsonb jsonb = JsonbBuilder.create();

				if (call.getError() != null) {
					final Info info = rpc.call(new Info.Request(), Info.class);
					wallet.setInfo(info);
					String result = call.getError().getMessage();

					if (result != null) {
						submitBtn.setDisable(false);
						errorTxt.setText(result);
						passphraseInput.setText("");

						debug.print("Error unlocking wallet: ".concat(result),
							OverlayController.class.getSimpleName()
						);
					}
				} else {
					errorTxt.setText("Wallet unlocked!");
					debug.print("Successfuly unlocked wallet", OverlayController.class.getSimpleName());
					passphraseInput.setText("");

					//TODO: Get rid of these numbers and use an enum instead!
					if (wallet.getUnlockState() == 3) {
						// send transaction
						window.getWalletController().sendTransactionAfterUnlock();
					} else if (wallet.getUnlockState() == 4) {
						window.getNodeController().startMissingNodes();
					} else if (wallet.getUnlockState() == 5) {
						window.getSettingsController().dumpKeys();
					}

					if (wallet.getUnlockState() != 1) {
						wallet.setLocked(Boolean.FALSE);
					}

					closeUnlockOverlay();
				}
			} catch (Exception e) {
				debug.print(e.getMessage(), OverlayController.class.getSimpleName());
			}

			stateEvent.fire(State.builder().working(false).build());
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
		/* Empty on purpose */
	}
}
