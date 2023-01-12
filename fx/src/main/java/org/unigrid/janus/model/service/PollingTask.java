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

package org.unigrid.janus.model.service;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;

import org.unigrid.janus.model.Wallet;
import java.util.TimerTask;
import javafx.application.Platform;
import org.unigrid.janus.model.rpc.entity.StakingStatus;
import org.unigrid.janus.model.rpc.entity.GetBlockCount;
import org.unigrid.janus.model.rpc.entity.GetConnectionCount;
import org.unigrid.janus.model.rpc.entity.GetUnlockState;
import org.unigrid.janus.model.rpc.entity.GetWalletInfo;

public class PollingTask extends TimerTask {
	private DebugService debug;
	private RPCService rpc;
	private TrayService trayService;
	private Wallet wallet;

	public PollingTask() {
		debug = CDI.current().select(DebugService.class).get();
		rpc = CDI.current().select(RPCService.class).get();
		wallet = CDI.current().select(Wallet.class).get();
		trayService = CDI.current().select(TrayService.class).get();
		debug.log("Polling task created!");
	}

	public void run() {
		Platform.runLater(() -> {
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

			try {
				final GetWalletInfo walletInfo = rpc.call(new GetWalletInfo.Request(), GetWalletInfo.class);
				final GetBlockCount blockCount = rpc.call(new GetBlockCount.Request(), GetBlockCount.class);

				final GetConnectionCount connCount = rpc.call(new GetConnectionCount.Request(),
					GetConnectionCount.class);

				final StakingStatus staking = rpc.call(new StakingStatus.Request(), StakingStatus.class);

				final GetUnlockState unlockState = rpc.call(new GetUnlockState.Request(),
					GetUnlockState.class
				);

				wallet.setBalance(walletInfo.getResult().getTotalbalance());
				wallet.setBlocks(blockCount.getResult());
				trayService.updateBlockCount(blockCount.getResult());
				wallet.setConnections(connCount.getResult());
				wallet.setEncryptedState(walletInfo);
				wallet.setWalletState(Wallet.LockState.from(unlockState.getResult().getState()));
				wallet.setStakingStatus(staking);
				wallet.setTransactionCount(walletInfo.getResult().getTxcount());
				wallet.setStatus("done");
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(e.getMessage());
				wallet.setOffline(true);
				rpc.stopPolling();
			}
		});
	}
}
