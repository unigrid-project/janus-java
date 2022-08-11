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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Timer;
import org.unigrid.janus.model.UpdateWallet;

@ApplicationScoped
public class PollingService {
	private static Timer pollingTimer;
	private static Timer updateTimer;

	@Inject private DebugService debug;
	//@Inject private UpdateWallet updateWallet;

	public void poll(int interval) {
		debug.print("poll", PollingService.class.getSimpleName());
		pollingTimer = new Timer(true);
		pollingTimer.scheduleAtFixedRate(new LongPollingTask(), 0, interval);
	}

	public void stopPolling() {
		if (pollingTimer != null) {
			pollingTimer.cancel();
			pollingTimer.purge();
		}
	}

	public void pollForUpdate(int interval) {
		System.out.println("starting the update timer");
		updateTimer = new Timer(false);
		updateTimer.scheduleAtFixedRate(new UpdateWallet(), 0, interval);

	}

	public void stopPollingForUpdate() {
		if (updateTimer != null) {
			updateTimer.cancel();
			updateTimer.purge();
		}
	}
}
