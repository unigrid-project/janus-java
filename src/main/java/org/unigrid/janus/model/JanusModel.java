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

package org.unigrid.janus.model;

import jakarta.enterprise.context.ApplicationScoped;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import lombok.Getter;

@ApplicationScoped
public class JanusModel {

	public static final String RESTART_WALLET = "restartwallet";
	private static PropertyChangeSupport pcs;

	public enum AppState {
		STARTING,
		LOADED,
		RESTARTING
	}

	@Getter
	private AppState appState;

	public JanusModel() {
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

	public void setAppState(AppState state) {
		AppState oldState = appState;
		appState = state;
		AppState newState = state;

		if (state == AppState.RESTARTING) {
			System.out.println("RESTART_WALLET");
			this.pcs.firePropertyChange(this.RESTART_WALLET, oldState, newState);
		}
	}
}
