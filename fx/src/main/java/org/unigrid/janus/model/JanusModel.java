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

package org.unigrid.janus.model;

import jakarta.enterprise.context.ApplicationScoped;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.unigrid.janus.model.cdi.Eager;

@Data
@Eager
@ApplicationScoped
public class JanusModel {
	public static final String APP_STATE_CHANGE = "appstatechange";
	public static final String APP_RESTARTING = "apprestarting";
	private static PropertyChangeSupport pcs;

	private AppState appState;
	@Setter(AccessLevel.NONE) private Boolean hasRun;
	@Setter(AccessLevel.NONE) private String version = "";

	public enum AppState {
		STARTING, LOADED, RESTARTING
	}

	public JanusModel() {
		final Properties properties = new Properties();

		try {
			properties.load(getClass().getResourceAsStream("application.properties"));

			version = Objects.requireNonNull(properties.getProperty("proj.ver"))
				.replace("-SNAPSHOT", "");
		} catch (NullPointerException | IOException e) {
			throw new IllegalStateException(e);
		}

		//appState = JanusModel.AppState.STARTING;

		if (pcs == null) {
			pcs = new PropertyChangeSupport(this);
		}
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.removePropertyChangeListener(listener);
	}

	public void setAppState(AppState state) {
		this.appState = state;

		System.out.println("appState " + state);
		if (state == AppState.RESTARTING) {
			this.pcs.firePropertyChange(this.APP_RESTARTING,
				Math.random(), Math.random());
		}
	}
}
