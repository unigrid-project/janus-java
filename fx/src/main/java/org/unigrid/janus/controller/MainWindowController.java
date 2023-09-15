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
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.scene.layout.AnchorPane;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javafx.animation.FadeTransition;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.rpc.entity.LockWallet;
import org.kordamp.ikonli.javafx.FontIcon;
import org.unigrid.janus.model.ExternalVersion;
import org.unigrid.janus.model.signal.Navigate;
import org.unigrid.janus.model.signal.OverlayRequest;

import static org.unigrid.janus.model.signal.Navigate.Location.*;
import org.unigrid.janus.model.signal.UnlockRequest;

@ApplicationScoped
public class MainWindowController implements Initializable, PropertyChangeListener {
	@Inject private DebugService debug;
	@Inject private RPCService rpc;
	@Inject private Wallet wallet;
	@Inject private ExternalVersion externalVersion;
	@Inject private Event<ExternalVersion> versionEvent;
	@Inject
	private Event<OverlayRequest> overlayRequest;
	@Inject private Event<UnlockRequest> unlockRequestEvent;

	// @FXML private Label lblBlockCount;
	// @FXML private Label lblConnection;
	@FXML private AnchorPane pnlMain;
	@FXML private AnchorPane pnlSplash;
	@FXML private FontIcon blocksIcn;
	@FXML private ToggleButton btnWallet;
	@FXML private ToggleButton btnTransactions;
	@FXML private ToggleButton btnNodes;
	@FXML private ToggleButton btnAddress;
	@FXML private ToggleButton btnDocs;
	@FXML private ToggleButton btnSettings;
	@FXML private ToggleButton btnGovernace;
	@FXML private VBox pnlWallet;
	@FXML private VBox pnlTransactions;
	@FXML private VBox pnlNodes;
	@FXML private VBox pnlAddress;
	@FXML private VBox pnlSettings;
	@FXML private VBox pnlDocs;
	@FXML private VBox pnlGovernace;
	@FXML private AnchorPane pnlOverlay;
	@FXML private AnchorPane pnlWarning;
	@FXML private FontIcon lockBtn;
	@FXML private FontIcon satelliteIcn;
	@FXML private FontIcon coinsBtn;
	@FXML private FontIcon unlockedBtn;
	@FXML private Tooltip connectionTltp;
	@FXML private Tooltip lockedTltp;
	@FXML private Tooltip stakingTltp;
	@FXML private Tooltip blocksTltp;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		pnlOverlay.setVisible(false);
		pnlWarning.setVisible(false);
		pnlSplash.setVisible(false);
		wallet.addPropertyChangeListener(this);
		lockBtn.iconColorProperty().setValue(Color.RED);
	}

	private void select(VBox panel, ToggleButton button) {
		btnWallet.setSelected(false);
		btnTransactions.setSelected(false);
		btnNodes.setSelected(false);
		btnSettings.setSelected(false);
		pnlWallet.setVisible(false);
		pnlTransactions.setVisible(false);
		pnlNodes.setVisible(false);
		pnlSettings.setVisible(false);
		pnlAddress.setVisible(false);
		btnAddress.setSelected(false);
		btnDocs.setSelected(false);
		pnlDocs.setVisible(false);
		pnlGovernace.setVisible(false);
		btnGovernace.setSelected(false);

		panel.setVisible(true);
		button.setSelected(true);
	}

	@FXML
	private void onWalletTap(MouseEvent event) {
		select(pnlWallet, btnWallet);
	}

	@FXML
	private void onTransactionsTap(MouseEvent event) {
		select(pnlTransactions, btnTransactions);
	}

	@FXML
	private void onNodesTap(MouseEvent event) {
		select(pnlNodes, btnNodes);
	}

	@FXML
	private void onAddressTap(MouseEvent event) {
		select(pnlAddress, btnAddress);
	}

	@FXML
	private void onDocsClicked(MouseEvent event) {
		select(pnlDocs, btnDocs);
	}

	@FXML
	private void onSettingsTap(MouseEvent event) {
		if (externalVersion.getDaemonVersion().equals("")) {
			externalVersion.callRPCForDaemonVersion();
			versionEvent.fire(externalVersion);
		}
		select(pnlSettings, btnSettings);
	}

	@FXML
	private void onGovernaceClicked(MouseEvent event) {
		select(pnlGovernace, btnGovernace);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals(wallet.BLOCKS_PROPERTY)) {
			String blocks;
			if (wallet.getSyncStatus() == Wallet.SyncStatus.SYNCING) {
				blocks = String.format("Syncing / Blocks: %d", (int) event.getNewValue());
			} else {
				blocks = String.format("Blocks: %d", (int) event.getNewValue());
			}

			blocksTltp.setText(blocks);
		}

		if (event.getPropertyName().equals(Wallet.CONNECTIONS_PROPERTY)) {
			connectionTltp.setText(String.format("Connections: %d", (int) event.getNewValue()));
			int connections = (int) event.getNewValue();

			if (connections > 0 && connections < 5) {
				satelliteIcn.iconColorProperty().setValue(Color.ORANGE);
			} else if (connections >= 5 && connections < 10) {
				satelliteIcn.iconColorProperty().setValue(Color.YELLOWGREEN);
			} else if (connections >= 10) {
				satelliteIcn.iconColorProperty().setValue(Color.GREEN);
			} else {
				satelliteIcn.iconColorProperty().setValue(Color.RED);
			}
		}

		if (event.getPropertyName().equals(Wallet.LOCKED_PROPERTY)) {
			boolean locked = (boolean) event.getNewValue();
			debug.log(String.format("Wallet Locked: %s", locked));

			if (locked) {
				lockedTltp.setText("Wallet Locked");
				unlockedBtn.setVisible(false);
				lockBtn.setVisible(true);
			} else {
				lockedTltp.setText("Wallet Unlocked");
				unlockedBtn.setVisible(true);
				lockBtn.setVisible(false);
			}

		}

		if (event.getPropertyName().equals(Wallet.LOCKED_STATE_PROPERTY)) {
			Wallet.LockState lockedState = (Wallet.LockState) event.getNewValue();
			debug.print(lockedState.toString(), this.getClass().getSimpleName());

			if (lockedState.equals(Wallet.LockState.UNLOCKED_FOR_STAKING) && !wallet.getStakingStatus()) {
				FadeTransition ft = new FadeTransition(Duration.millis(500), coinsBtn);
				ft.setFromValue(1.0);
				ft.setToValue(0.3);
				ft.setCycleCount(40);
				ft.setAutoReverse(true);
				ft.play();
			}
		}

		if (event.getPropertyName().equals(Wallet.SYNC_STATE)) {
			Wallet.SyncStatus syncStatus = (Wallet.SyncStatus) event.getNewValue();
			debug.print("sync state: " + syncStatus, this.getClass().getSimpleName());

			if (syncStatus.equals(Wallet.SyncStatus.SYNCING)) {
				blocksIcn.iconColorProperty().setValue(Color.RED);
				FadeTransition ft = new FadeTransition(Duration.millis(500), blocksIcn);
				ft.setFromValue(1.0);
				ft.setToValue(0.3);
				ft.setCycleCount(40);
				ft.setAutoReverse(true);
				ft.play();
			} else {
				blocksIcn.iconColorProperty().setValue(Color.web("#68c5ff"));
			}
		}

		if (event.getPropertyName().equals(Wallet.STAKING_PROPERTY)) {
			boolean staking = (boolean) event.getNewValue();

			if (staking) {
				stakingTltp.setText("Staking On");
				coinsBtn.iconColorProperty().setValue(Color.ORANGE);
			} else {
				stakingTltp.setText("Staking Off");
				coinsBtn.iconColorProperty().setValue(Color.RED);
			}
		}

		if (event.getPropertyName().equals(Wallet.IS_OFFLINE)) {
			debug.print("wallet.IS_OFFLINE", this.getClass().getSimpleName());

			if (wallet.getOffline()) {
				pnlWarning.setVisible(true);
			}
		}

		/*
		if (event.getPropertyName().equals(wallet.STATUS_PROPERTY)) {
			String status = (String) event.getNewValue();
			if (status.equals("Done loading")) {
				pnlSplash.setVisible(false);
			} else {
				pnlSplash.setVisible(true);
			}
		}
		 */
	}

	@FXML
	private void onLockPressed(MouseEvent event) {
		if (!wallet.getLocked()) {
			return;
		}

		unlockRequestEvent.fire(UnlockRequest.builder().type(UnlockRequest.Type.ORDINARY).build());
		overlayRequest.fire(OverlayRequest.OPEN);
	}

	@FXML
	private void onUnlockPressed(MouseEvent event) {
		debug.print("Locking wallet pressed", MainWindowController.class.getSimpleName());

		if (wallet.getLocked()) {
			return;
		}

		wallet.setLocked(Boolean.TRUE);
		final LockWallet call = rpc.call(new LockWallet.Request(), LockWallet.class);
		debug.print("Locking wallet", MainWindowController.class.getSimpleName());
	}

	@FXML
	private void onCoinsPressed(MouseEvent event) {
		if (wallet.getStakingStatus() || !wallet.getLocked()) {
			return;
		}

		unlockRequestEvent.fire(UnlockRequest.builder().type(UnlockRequest.Type.FOR_STAKING).build());
		overlayRequest.fire(OverlayRequest.OPEN);
	}

	private void eventNavigate(@Observes Navigate navigate) {
		switch (navigate.getLocation()) {
			case ADDRESS_TAB ->
				select(pnlAddress, btnAddress);
			case WALLET_TAB ->
				select(pnlWallet, btnWallet);
		}
	}

	public void showSplashScreen() {
		System.out.println("Show the shutdown splash screen");
		pnlSplash.setVisible(true);
		rpc.stopPolling();
		System.exit(0);
	}
}
