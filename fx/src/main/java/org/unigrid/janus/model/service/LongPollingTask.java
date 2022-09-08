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

import java.util.TimerTask;
import javafx.application.Platform;
import org.unigrid.janus.model.Wallet;

public class LongPollingTask extends TimerTask {
	private static DebugService debug = new DebugService();
	private static PollingService polling = new PollingService();
	private static Wallet wallet = new Wallet();
	private static WindowService window = new WindowService();

	public LongPollingTask() {
		debug.log("Long polling task created!");
	}

	public void run() {
		Platform.runLater(() -> {
			try {
				window.getDocsController().pullNewDocumentaion();
			} catch (Exception e) {
				debug.print("error getting docs: ".concat(e.getMessage()),
					LongPollingTask.class.getSimpleName()
				);

				polling.stopPolling();
			}
		});
	}
}
