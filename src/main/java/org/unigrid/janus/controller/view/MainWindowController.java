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

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseEvent;
import javafx.stage.WindowEvent;
import javafx.scene.paint.Color;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.scene.layout.AnchorPane;
import javafx.application.Platform;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;
// import javafx.beans.property.ReadOnlyObjectWrapper;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.WindowService;
// import org.unigrid.janus.model.rpc.entity.NewAddress;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.rpc.entity.UnlockWallet;
import org.kordamp.ikonli.javafx.FontIcon;
import org.unigrid.janus.model.rpc.entity.Info;
import org.unigrid.janus.model.rpc.entity.LockWallet;

public class MainWindowController implements Initializable, PropertyChangeListener {

	private static DebugService debug = new DebugService();
	private static RPCService rpc = new RPCService();
	private static Wallet wallet = new Wallet();
	private static WindowService window = new WindowService();

	private static final int TAB_WALLET = 1;
	private static final int TAB_TRANSACTIONS = 2;
	private static final int TAB_NODES = 3;
	private static final int TAB_SETTINGS = 4;
	/* Injected fx:id from FXML */
	@FXML private Label lblBlockCount;
	@FXML private Label lblConnection;
	@FXML private AnchorPane pnlMain;
	@FXML private AnchorPane pnlSplash;
	// main navigation
	@FXML
	private ToggleButton btnWallet;
	@FXML
	private ToggleButton btnTransactions;
	@FXML
	private ToggleButton btnNodes;
	@FXML
	private ToggleButton btnSettings;
	@FXML
	private VBox pnlWallet;
	@FXML
	private VBox pnlTransactions;
	@FXML
	private VBox pnlNodes;
	@FXML
	private VBox pnlSettings;
	@FXML
	private BorderPane pnlUnlock;
	@FXML
	private FontIcon lockBtn;
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
	@FXML
	private FontIcon satelliteIcn;
	@FXML
	private FontIcon coinsBtn;
	@FXML
	private FontIcon unlockedBtn;
	@FXML
	private Tooltip connectionTltp;
	@FXML
	private Tooltip lockedTltp;
	@FXML
	private Tooltip stakingTltp;
	@FXML
	private Tooltip blocksTltp;
	@Getter @Setter
	private int unlockState = 0;
	@Getter @Setter
	private Boolean staking = false;
	@Getter @Setter
	private Boolean walletLocked = false;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		/* Empty on purpose */
		wallet.addPropertyChangeListener(this);
		pnlUnlock.setVisible(false);
	}

	@FXML
	private void onShown(WindowEvent event) {
		debug.log("Shown event fired!");
		lockBtn.iconColorProperty().setValue(Color.RED);
		Platform.runLater(() -> {
			try {
				debug.log("Shown event executing.");
				// loadWalletPreviewTrans();
			} catch (Exception e) {
				debug.log(String.format("ERROR: (onShown) %s", e.getMessage()));
			}
		});
	}

	@FXML
	private void onGetAddress(MouseEvent event) {
		debug.log("Get address clicked!");
		// debug.log(rpc.callToJson(new NewAddress.Request("Wilcokat007")));
	}

	private void tabSelect(int tab) {
		btnWallet.setSelected(false);
		btnTransactions.setSelected(false);
		btnNodes.setSelected(false);
		btnSettings.setSelected(false);
		pnlWallet.setVisible(false);
		pnlTransactions.setVisible(false);
		pnlNodes.setVisible(false);
		pnlSettings.setVisible(false);
		switch (tab) {
			case TAB_WALLET:
				pnlWallet.setVisible(true);
				btnWallet.setSelected(true);
				break;
			case TAB_TRANSACTIONS:
				pnlTransactions.setVisible(true);
				btnTransactions.setSelected(true);
				break;
			case TAB_NODES:
				pnlNodes.setVisible(true);
				btnNodes.setSelected(true);
				break;
			case TAB_SETTINGS:
				pnlSettings.setVisible(true);
				btnSettings.setSelected(true);
				break;
			default:
				pnlWallet.setVisible(true);
				btnWallet.setSelected(true);
				break;
		}

	}

	@FXML
	private void onWalletTap(MouseEvent event) {
		try {
			tabSelect(TAB_WALLET);
			// loadWalletPreviewTrans();
			debug.log("Wallet clicked!");
		} catch (Exception e) {
			debug.log(String.format("ERROR: (wallet click) %s", e.getMessage()));
		}
	}

	@FXML
	private void onTransactionsTap(MouseEvent event) {
		try {
			tabSelect(TAB_TRANSACTIONS);
			// loadTransactions(1);
			debug.log("Transactions clicked!");
		} catch (Exception e) {
			debug.log(String.format("ERROR: (transactions click) %s", e.getMessage()));
		}
	}

	@FXML
	private void onNodesTap(MouseEvent event) {
		try {
			tabSelect(TAB_NODES);
			debug.log("Nodes clicked!");
		} catch (Exception e) {
			debug.log(String.format("ERROR: (nodes click) %s", e.getMessage()));
		}
	}

	@FXML
	private void onSettingsTap(MouseEvent event) {
		try {
			tabSelect(TAB_SETTINGS);
			debug.log("Settings clicked!");
		} catch (Exception e) {
			debug.log(String.format("ERROR: (settings click) %s", e.getMessage()));
		}
	}

	public void propertyChange(PropertyChangeEvent event) {
		debug.log("Main Window change fired!");
		debug.log(event.getPropertyName());
		if (event.getPropertyName().equals(wallet.BLOCKS_PROPERTY)) {
			String blocks = String.format("Blocks: %d", (int) event.getNewValue());
			blocksTltp.setText(blocks);
		}
		if (event.getPropertyName().equals(wallet.CONNECTIONS_PROPERTY)) {
			connectionTltp.setText(String.format("Connections: %d", (int) event.getNewValue()));
			int connections = (int) event.getNewValue();
			if (connections > 0 && connections < 5) {
				satelliteIcn.iconColorProperty().setValue(Color.YELLOW);
			} else if (connections > 5) {
				satelliteIcn.iconColorProperty().setValue(Color.GREEN);
			} else {
				satelliteIcn.iconColorProperty().setValue(Color.RED);
			}
		}
		if (event.getPropertyName().equals(wallet.LOCKED_PROPERTY)) {
			boolean locked = (boolean) event.getNewValue();
			setWalletLocked(locked);
			debug.log(String.format("Wallet Locked: %s", locked));
			if (locked) {
				//tabSelect(TAB_WALLET);
				//show locked icon
				lockedTltp.setText("Wallet Locked");
				unlockedBtn.setVisible(false);
				lockBtn.setVisible(true);
			} else {
				// show unlock icon
				lockedTltp.setText("Wallet Unlocked");
				unlockedBtn.setVisible(true);
				lockBtn.setVisible(false);
			}
		}
		if (event.getPropertyName().equals(wallet.STAKING_PROPERTY)) {
			boolean staking = (boolean) event.getNewValue();
			setStaking(staking);
			if (staking) {
				stakingTltp.setText("Staking On");
				coinsBtn.iconColorProperty().setValue(Color.ORANGE);
			} else {
				stakingTltp.setText("Staking Off");
				coinsBtn.iconColorProperty().setValue(Color.RED);
			}
		}
		if (event.getPropertyName().equals(wallet.STATUS_PROPERTY)) {
			String status = (String) event.getNewValue();
			if (status.equals("Done loading")) {
				pnlSplash.setVisible(false);
			} else {
				pnlSplash.setVisible(true);
			}
		}
	}

	@FXML
	private void onLockPressed(MouseEvent event) {
		if (!getWalletLocked()) {
			return;
		}
		pnlUnlock.setVisible(true);
		submitBtn.setText("UNLOCK");
		unlockCopy.setText("Unlock your wallet by entering your passphrase and "
			+ "pressing the UNLOCK button.");
		setUnlockState(2);
	}

	@FXML
	private void onUnlockPressed(MouseEvent event) {
		final LockWallet call = rpc.call(new LockWallet.Request(), LockWallet.class);
	}

	@FXML
	private void onCoinsPressed(MouseEvent event) {
		if (getStaking()) {
			return;
		}
		setUnlockState(1);
		pnlUnlock.setVisible(true);
		submitBtn.setText("STAKE");
		unlockCopy.setText("Enable staking in your wallet by entering your passphrase and "
			+ "pressing the STAKE button.");
	}

	@FXML
	private void unlockForTime(MouseEvent event) {
		setUnlockState(3);
	}

	@FXML
	private void onCancelLockPressed(MouseEvent event) {
		closeUnlockScreen();
	}

	@FXML
	private void onSubmitPassphrasePressed(MouseEvent event) {
		submitBtn.setDisable(true);
		Object[] sendArgs;
		long stakingStartTime = wallet.getStakingStartTime();
		switch (getUnlockState()) {
			case 1:
				sendArgs = new Object[]{passphraseInput.getText(), stakingStartTime, true};
				break;
			case 2:
				sendArgs = new Object[]{passphraseInput.getText(), 0};
				break;
			case 3:
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
				closeUnlockScreen();
			}

		}
	}

	private void closeUnlockScreen() {
		setUnlockState(0);
		unlockCopy.setText("");
		spinnerIcon.setVisible(false);
		errorTxt.setText("");
		passphraseInput.setText("");
		pnlUnlock.setVisible(false);
		submitBtn.setDisable(false);
	}
}
