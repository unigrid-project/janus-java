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
import jakarta.enterprise.event.Observes;
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
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javax.crypto.AEADBadTagException;
import org.unigrid.janus.model.AccountsData;
import org.unigrid.janus.model.CryptoUtils;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.rpc.entity.UnlockWallet;
import org.unigrid.janus.model.signal.CosmosWalletRequest;
import org.unigrid.janus.model.signal.MergeInputsRequest;
import org.unigrid.janus.model.signal.NodeRequest;
import org.unigrid.janus.model.signal.OverlayRequest;
import org.unigrid.janus.model.signal.State;
import org.unigrid.janus.model.signal.WalletRequest;
import org.unigrid.janus.model.signal.UnlockRequest;
import static org.unigrid.janus.model.signal.UnlockRequest.Type.COSMOS_DELEGATE_GRIDNODE;
import static org.unigrid.janus.model.signal.UnlockRequest.Type.COSMOS_DELEGATE_STAKING;
import static org.unigrid.janus.model.signal.UnlockRequest.Type.COSMOS_SEND_TOKENS;
import static org.unigrid.janus.model.signal.UnlockRequest.Type.COSMOS_UNDELEGATE_GRIDNODE;
import org.unigrid.janus.view.FxUtils;

@ApplicationScoped
public class OverlayController implements Initializable {
	@Inject
	private DebugService debug;
	@Inject
	private RPCService rpc;
	@Inject
	private Wallet wallet;

	@Inject
	private Event<NodeRequest> nodeRequestEvent;
	@Inject
	private Event<WalletRequest> walletRequestEvent;
	@Inject
	private Event<State> stateEvent;
	@Inject
	private Event<MergeInputsRequest> mergeInputsEvent;
	@Inject
	private Event<CosmosWalletRequest> cosmosWalletEvent;
	@FXML
	private GridPane pnlUnlock;
	@FXML
	private PasswordField passphraseInput;
	@FXML
	private Text errorTxt;
	@FXML
	private ImageView spinnerIcon;
	@FXML
	private Button submitBtn;
	@FXML
	private Text unlockCopy;
	private UnlockRequest currentUnlockRequest; // Instance variable to store the current UnlockRequest	
	@Inject
	private AccountsData accountsData;
	@Inject
	private CryptoUtils cryptoUtils;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		/* Empty on purpose */
	}

	private void eventUnlockRequest(@Observes UnlockRequest unlockRequest) {
		debug.print("Unlock request fired", OverlayController.class.getSimpleName());

		submitBtn.setText(unlockRequest.getType().getAction());
		//wallet.setUnlockState(unlockRequest.getType().getState());
		unlockCopy.setText(unlockRequest.getType().getDescription());
		pnlUnlock.setVisible(true);

		Platform.runLater(() -> passphraseInput.requestFocus());
		currentUnlockRequest = unlockRequest;
	}

	@FXML
	private void setKeyListern(KeyEvent ke) {
		if (ke.getCode() == KeyCode.ENTER) {
			submit();
		} else if (ke.getCode() == KeyCode.ESCAPE) {
			hide();
		}
	}

	@FXML
	private void onCancelLockPressed(MouseEvent event) {
		hide();
	}

	@FXML
	private void onSubmitPassphrasePressed(MouseEvent event) {
		submit();
	}
	
	private void checkCosmosPassword(UnlockRequest.Type unlockType) throws Exception {
		AccountsData.Account selectedAccount = accountsData.getSelectedAccount();
		String encryptedPrivateKey = selectedAccount.getEncryptedPrivateKey();

		try {
			cryptoUtils.decrypt(encryptedPrivateKey, passphraseInput.getText());
			switch (unlockType) {
				case COSMOS_SEND_TOKENS:
					cosmosWalletEvent.fire(CosmosWalletRequest.SEND_TOKENS);
					break;
				case COSMOS_DELEGATE_GRIDNODE:
					cosmosWalletEvent.fire(CosmosWalletRequest.DELEGATE_GRIDNODE);
					break;
				case COSMOS_UNDELEGATE_GRIDNODE:
					cosmosWalletEvent.fire(CosmosWalletRequest.UNDELEGATE_GRIDNODE);
					break;
				case COSMOS_DELEGATE_STAKING:
					cosmosWalletEvent.fire(CosmosWalletRequest.DELEGATE_STAKING);
					break;
				case COSMOS_CLAIM_REWARDS:
					cosmosWalletEvent.fire(CosmosWalletRequest.CLAIM_REWARDS);
					break;
				default:
					throw new AssertionError();
			}
			hide();
		} catch (AEADBadTagException e) {
			errorTxt.setText("Bad Password");
			submitBtn.setDisable(false);
			e.printStackTrace();
		}

	}

	private void submit() {
		submitBtn.setDisable(true);
		Object[] sendArgs;
		long stakingStartTime = wallet.getStakingStartTime();
		stateEvent.fire(State.builder().working(true).build());

		UnlockRequest.Type unlockType = currentUnlockRequest.getType();
		switch (unlockType) {
			case FOR_STAKING:
				sendArgs = new Object[]{passphraseInput.getText(), stakingStartTime, true};
				break;
			case ORDINARY:
			case FOR_MERGING:
				sendArgs = new Object[]{passphraseInput.getText(), 0};
				break;
			case FOR_SEND:
			case FOR_GRIDNODE:
			case FOR_DUMP:
				// Unlock for 30 seconds only
				sendArgs = new Object[]{passphraseInput.getText(), 30};
				break;
			case COSMOS_SEND_TOKENS: 
			case COSMOS_DELEGATE_GRIDNODE:
			case COSMOS_UNDELEGATE_GRIDNODE:
			case COSMOS_DELEGATE_STAKING:
			case COSMOS_CLAIM_REWARDS: {
				try {
					checkCosmosPassword(unlockType);
					return;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

			default:
				throw new AssertionError();
		}
		
		if (passphraseInput.getText().equals("")) {
			errorTxt.setText("Please enter a passphrase");
			submitBtn.setDisable(false);
			stateEvent.fire(State.builder().working(false).build());
		} else {
			try {
				final UnlockWallet call = rpc.call(new UnlockWallet.Request(sendArgs),
					UnlockWallet.class);
				Jsonb jsonb = JsonbBuilder.create();

				if (call.getError() != null) {
					String result = call.getError().getMessage();

					if (result != null) {
						submitBtn.setDisable(false);
						errorTxt.setText(result);
						passphraseInput.setText("");

						debug.print("Error unlocking wallet: ".concat(result),
							OverlayController.class.getSimpleName());
					}
				} else {
					errorTxt.setText("Wallet unlocked!");
					debug.print("Successfuly unlocked wallet",
						OverlayController.class.getSimpleName());
					passphraseInput.setText("");

					// TODO: Get rid of these numbers and use an enum instead!
					if (wallet.getUnlockState() == 3) {
						walletRequestEvent.fire(WalletRequest.SEND_TRANSACTION);
					} else if (wallet.getUnlockState() == 4) {
						nodeRequestEvent.fire(NodeRequest.START_MISSING);
					} else if (wallet.getUnlockState() == 5) {
						walletRequestEvent.fire(WalletRequest.DUMP_KEYS);
					}

					if (currentUnlockRequest.getType() == UnlockRequest.Type.FOR_MERGING) {
						mergeInputsEvent.fire(MergeInputsRequest.builder()
							.type(MergeInputsRequest.Type.MERGE)
							.address(currentUnlockRequest.getAddress())
							.amount(currentUnlockRequest.getAmount())
							.utxos(currentUnlockRequest.getUtxos())
							.build());
					}

					if (wallet.getUnlockState() != 1) {
						wallet.setLocked(Boolean.FALSE);
					}

					hide();
				}
			} catch (Exception e) {
				debug.print(e.getMessage(), OverlayController.class.getSimpleName());
			}

			stateEvent.fire(State.builder().working(false).build());
		}
	}

	private void hide() {
		wallet.setUnlockState(0);
		unlockCopy.setText("");
		spinnerIcon.setVisible(false);
		errorTxt.setText("");
		passphraseInput.setText("");
		pnlUnlock.setVisible(false);
		submitBtn.setDisable(false);

		eventOverlayRequest(OverlayRequest.CLOSE);
	}

	private void eventOverlayRequest(@Observes OverlayRequest overlayRequest) {
		FxUtils.executeParentById("pnlOverlay", pnlUnlock, node -> {
			node.setVisible(overlayRequest == OverlayRequest.OPEN);
		});
	}
}
