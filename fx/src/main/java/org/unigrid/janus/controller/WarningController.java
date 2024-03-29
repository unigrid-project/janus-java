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
import jakarta.inject.Inject;
import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;

import java.io.File;

import org.unigrid.janus.model.DataDirectory;
import org.unigrid.janus.model.JanusModel;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.view.FxUtils;

@ApplicationScoped
public class WarningController {
	public static final String HIDE_WARNING = "hidewarning";
	public static final String STATUS_PROPERTY = "walletstatus";

	@Inject private DebugService debug;
	@Inject private RPCService rpc;
	@Inject private HostServices hostServices;

	private static JanusModel janusModel = new JanusModel();

	public void onRestartClicked(MouseEvent event) {
		debug.log("onRestartClicked");
		// TODO: Will be implemented once CDI is working
		// warningEvent.fire(this);
		janusModel.setAppState(JanusModel.AppState.RESTARTING);
		//window.getMainWindowController().hideWarning();

		FxUtils.executeParentById("pnlWarning", (Node) event.getSource(), (node) -> {
			node.setVisible(false);
		});
	}

	@FXML
	public void onShowDebug(MouseEvent event) {
		File debugLog = DataDirectory.getDebugLog();
		try {
			hostServices.showDocument(debugLog.getAbsolutePath());
		} catch (NullPointerException e) {
			System.out.println("Null Host services " + e.getMessage());
		}
	}
}
