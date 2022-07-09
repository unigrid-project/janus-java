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

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TimerTask;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;
import org.controlsfx.control.Notifications;
import org.unigrid.janus.model.cdi.Eager;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.service.PollingService;
import org.update4j.Configuration;
import org.update4j.OS;

@Eager
@ApplicationScoped
public class UpdateWallet extends TimerTask {
	private static DebugService debug = new DebugService();
	private static final String BASE_URL = "https://raw.githubusercontent.com/unigrid-project/unigrid-update/main/%s";
	private static PollingService polling = new PollingService();
	private OS os = OS.CURRENT;
	private static final Map<?, ?> OS_CONFIG = ArrayUtils.toMap(new Object[][] {
			{ OS.LINUX, "config-linux.xml" }, { OS.WINDOWS, "config-windows.xml" }, { OS.MAC, "config-mac.xml" }
	});

	public enum UpdateState {
		UPDATE_READY,
		UPDATE_NOT_READY
	}

	@Getter
	private static final String UPDATE_PROPERTY = "update";

	private UpdateState oldValue = UpdateState.UPDATE_NOT_READY;
	private Configuration updateConfig = null;
	private SimpleBooleanProperty running;
	private static PropertyChangeSupport pcs;

	public UpdateWallet() {
		System.out.println("Init walletUpdate");
		if (this.pcs != null) {
			// TODO: Should this really be a fallthrough ? It looks dangerous.
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

	@Override
	public void run() {
		if(pcs == null)
			pcs = new PropertyChangeSupport(this);
		if (checkUpdate()) {
			this.pcs.firePropertyChange(this.UPDATE_PROPERTY, oldValue, UpdateState.UPDATE_READY);

			if (SystemUtils.IS_OS_MAC_OSX) {
				Notifications.create().title("Update Ready")
						.text("New update ready \nPleas close application to update!")
						.position(Pos.TOP_RIGHT).showInformation();
			} else {
				Notifications.create().title("Update Ready")
						.text("New update ready \nPleas close application to update!").showInformation();
			}
		} else {
			
			this.pcs.firePropertyChange(this.UPDATE_PROPERTY, oldValue, UpdateState.UPDATE_READY);
			debug.print("user.dir: " + System.getProperty("user.dir"), UpdateWallet.class.getSimpleName());
		}

	}

	private Boolean checkUpdate() {
		boolean update = false;

		URL configUrl = null;
		Configuration updateConfig = null;

		try {
			configUrl = new URL(String.format(BASE_URL, OS_CONFIG.get(os)));
		} catch (MalformedURLException mle) {
			System.out.println("Unable to find url to config.xml");
			System.err.println(mle.getMessage());
		}

		try (Reader in = new InputStreamReader(configUrl.openStream(), StandardCharsets.UTF_8)) {
			updateConfig = Configuration.read(in);
			System.out.println("Reading the config file");
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		try {
			System.out.println("Checking for update");
			if(updateConfig == null)
				debug.print("null config", UpdateWallet.class.getSimpleName());
			update = updateConfig.requiresUpdate();
		} catch (IOException e) {
			update = false;
		}

		return update;
	}

	private void restartWallet() {
		// TODO: move code from WindowBarController here
	}
}
