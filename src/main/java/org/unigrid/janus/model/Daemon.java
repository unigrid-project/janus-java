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

package org.unigrid.janus.model;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javax.annotation.PostConstruct;
import javax.naming.ConfigurationException;
//import javax.transaction.Transactional;
//import lombok.Getter;
//import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
//import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
//import org.springframework.stereotype.Service;

@ApplicationScoped
public class Daemon {
	private static final String PROPERTY_LOCATION_KEY = "janus.daemon.location";
	private static final String PROPERTY_LOCATION = Preferences.PROPS.getString(PROPERTY_LOCATION_KEY);
	private Optional<Process> process = Optional.empty();
	//@Getter private User rpcCredentials;

	@PostConstruct
	private void init() {
		//rpcCredentials = new User();
		//rpcCredentials.setName(RandomStringUtils.randomAlphabetic(30));
		//rpcCredentials.setPassword(RandomStringUtils.randomAlphabetic(50));
	}

	private boolean isLocalFile(String path) {
		return new File(path).exists();
	}

	private void start() throws IOException {
		process = Optional.of(Runtime.getRuntime().exec(new String[]{PROPERTY_LOCATION}));
	}

	private void connect() {
		/* TODO: Implement connecting to remote instances */
	}

	public void startOrConnect() throws ConfigurationException, IOException {
		if (StringUtils.isNotBlank(PROPERTY_LOCATION)) {
			if (isLocalFile(PROPERTY_LOCATION)) {
				start();
			} else {
				connect();
			}
		} else {
			new Alert(AlertType.NONE).show();
			throw new ConfigurationException(String.format("No location to the daemon specified in "
				+ " property '%s'. This should point to either a local file, "
				+ "or a remote http location.", PROPERTY_LOCATION_KEY)
			);
		}
	}

	public void stopOrDisconnect() throws InterruptedException {
		if (process.isPresent()) {
			process.get().destroy();
			process.get().waitFor();
		}

		/* TODO: Implement disconnecting from remote instances whenever we are not on a local daemon */
	}
}
