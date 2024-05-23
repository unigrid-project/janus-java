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
import java.util.TimerTask;
import javafx.application.Platform;
import org.unigrid.janus.controller.DocumentationController;
import org.unigrid.janus.model.Wallet;

public class LongPollingTask extends TimerTask {
	private DebugService debug;
	private PollingService polling;
	private Wallet wallet;

	public LongPollingTask() {
		debug = CDI.current().select(DebugService.class).get();
		polling = CDI.current().select(PollingService.class).get();
		wallet = CDI.current().select(Wallet.class).get();

		debug.log("Long polling task created!");
	}

	public void run() {
		Platform.runLater(() -> {
			try {
				CDI.current().select(DocumentationController.class).get().pullNewDocumentaion();
			} catch (Exception e) {
				debug.print("error getting docs: ".concat(e.getMessage()),
					LongPollingTask.class.getSimpleName()
				);

				if (polling.getSyncTimerRunning()) {
					polling.stopSyncPoll();
				}
			}
		});
	}
}
