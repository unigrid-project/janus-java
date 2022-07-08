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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;
import org.controlsfx.control.Notifications;
import org.unigrid.janus.model.cdi.Eager;
import org.update4j.Configuration;
import org.update4j.OS;

@Eager
@ApplicationScoped
public class UpdateWallet extends TimerTask {
	private static final String BASE_URL = "https://raw.githubusercontent.com/unigrid-project/unigrid-update/%s";

	private static final Map<?, ?> OS_CONFIG = ArrayUtils.toMap(new Object[][] {
		{OS.LINUX, "config-linux.xml"}, {OS.WINDOWS, "config-windows.xml"}, {OS.MAC, "config-mac.xml"}
	});

	public enum UpdateState {
		UPDATE_READY,
		UPDATE_NOT_READY
	}

	@Getter
	private static final String UPDATE_PROPERTY = "update";

	private UpdateState oldValue = UpdateState.UPDATE_NOT_READY;
	private Configuration config;
	private SimpleBooleanProperty running;
	private PropertyChangeSupport pcs;

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.removePropertyChangeListener(listener);
	}

	@PostConstruct
	private void init() {
		System.out.println("Init walletUpdate");
		OS os = OS.CURRENT;

		if (pcs != null) {
			// TODO: Should this really be a fallthrough ? It looks dangerous.
			return;
		}

		URL configUrl = null;
		Configuration updateConfig = null;
		pcs = new PropertyChangeSupport(this);

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

			// TODO: Hardcoded to marcus home directory! Needs to be fixed.
			final String directory = "/home/marcus/Documents/unigrid/config/UpdateWalletConfig/config.xml";

			try (Reader in = Files.newBufferedReader(Paths.get(directory))) {
				System.out.println("reading local config xml");
				updateConfig = Configuration.read(in);
			} catch (IOException ioe) {
				/* Just rethrow as a runtime exception for now - if we get here, we are in trouble anyway */
				throw new IllegalStateException(ioe);
			}
		}
	}

	@Override
	public void run() {
		if (checkUpdate()) {
			pcs.firePropertyChange(this.UPDATE_PROPERTY, oldValue, UpdateState.UPDATE_READY);

			if (SystemUtils.IS_OS_MAC_OSX) {
				Notifications.create().title("Update Ready")
					.text("New update ready \nPleas close application to update!")
					.position(Pos.TOP_RIGHT).showInformation();
			} else {
				Notifications.create().title("Update Ready")
					.text("New update ready \nPleas close application to update!").showInformation();
			}
		}
	}

	private Boolean checkUpdate() {
		boolean update = false;

		try {
			System.out.println("Checking for update");
			update = config.requiresUpdate();
		} catch (IOException e) {
			update = false;
		}

		return update;
	}

	private void doUpdate() {
		running.set(true);
	}
}
