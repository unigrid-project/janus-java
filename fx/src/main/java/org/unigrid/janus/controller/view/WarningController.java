/*
	The Janus Wallet
	Copyright © 2021-2022 The Unigrid Foundation

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

import jakarta.enterprise.context.ApplicationScoped;
import java.beans.PropertyChangeSupport;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import org.unigrid.janus.model.JanusModel;
import org.unigrid.janus.model.Wallet;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.WindowService;

@ApplicationScoped
public class WarningController implements Initializable{
	public static final String HIDE_WARNING = "hidewarning";
	public static final String STATUS_PROPERTY = "walletstatus";
	private static DebugService debug = new DebugService();
	private static RPCService rpc = new RPCService();
	private static Wallet wallet;
	private static JanusModel janusModel = new JanusModel();
	private static WindowService window = WindowService.getInstance();
	private static PropertyChangeSupport pcs;
	
	/**
	public WarningController() {
		wallet = window.getWallet();
		if (this.pcs != null) {
			return;
		}
		this.pcs = new PropertyChangeSupport(this);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.removePropertyChangeListener(listener);
	}
**/
	public void onRestartClicked(MouseEvent event) {
		debug.log("onRestartClicked");
		// will be implemented once CDI is working
		//warningEvent.fire(this);
		restartWallet();
	}
	
	public void restartWallet() {
		janusModel.setAppState(JanusModel.AppState.RESTARTING);
		window.getMainWindowController().hideWarning();
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		window.setWarnController(this);
	}	
	
	@FXML
	private void onButtonClicked(KeyEvent e) {
		if (e.getCode() == KeyCode.ENTER) {
			System.out.println("enter clicked");
			restartWallet();
		}
	}
}
