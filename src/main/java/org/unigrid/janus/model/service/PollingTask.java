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

package org.unigrid.janus.model.service;

import org.unigrid.janus.model.rpc.entity.Info;
import org.unigrid.janus.model.Wallet;
import java.util.TimerTask;
import javafx.application.Platform;
import org.unigrid.janus.model.rpc.entity.StakingStatus;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

public class PollingTask extends TimerTask {

	private static DebugService debug = new DebugService();
	private static RPCService rpc = new RPCService();
	private static Wallet wallet = new Wallet();
	private static Jsonb jsonb = JsonbBuilder.create();

	public PollingTask() {
		debug.log("Polling task created!");
	}

	public void run() {
		Platform.runLater(() -> {
			//debug.log(rpc.callToJson(new Info.Request()));
			wallet.setProcessingStatus();
			final Info info = rpc.call(new Info.Request(), Info.class);
			String sInfo = jsonb.toJson(info);
			debug.log(sInfo);
			wallet.setInfo(info);
			final StakingStatus staking = rpc.call(new StakingStatus.Request(), StakingStatus.class);
			wallet.setStakingStatus(staking);
			debug.log(rpc.callToJson(new StakingStatus.Request()));
		});
	}
}
