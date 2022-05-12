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

package org.unigrid.janus.model.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import javax.naming.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.unigrid.janus.model.Preferences;
import org.unigrid.janus.model.rpc.entity.BlockCount;

@ApplicationScoped
public class Daemon {

	@Inject
	private RPCService rpc;
	private static final String PROPERTY_LOCATION_KEY = "janus.daemon.location";
	private static final String PROPERTY_LOCATION_DEFAULT
		= (Daemon.class.getResource("/daemon/unigridd").getFile() == null)
		? Daemon.class.getResource("/daemon/unigridd.exe").getFile()
		: Daemon.class.getResource("/daemon/unigridd").getFile();

	public static final String PROPERTY_LOCATION
		= Preferences.PROPS.getString(PROPERTY_LOCATION_KEY, PROPERTY_LOCATION_DEFAULT);

	private Optional<Process> process = Optional.empty();

	private void runDaemon() throws IOException {
		System.out.println("starting daemon");
		if (isDaemonRunning()) {
			process = Optional.of(Runtime.getRuntime().exec(new String[]{PROPERTY_LOCATION}));
		}
	}

	private boolean isDaemonRunning() {
		boolean isRunning = true;
		try {
			BlockCount blocks = rpc.call(new BlockCount(), BlockCount.class);

			isRunning = false;
		} catch (jakarta.ws.rs.ProcessingException e) {

			isRunning = true;
		}

		return isRunning;
	}

	public boolean isHttp() throws MalformedURLException {
		return "http".equals(new URL(PROPERTY_LOCATION).getProtocol());
	}

	public boolean isHttp(String value) throws MalformedURLException {
		return "http".equals(new URL(value).getProtocol());
	}

	public boolean isLocalFile() {

		return new File(PROPERTY_LOCATION).exists();
	}

	public void start() throws ConfigurationException, IOException, MalformedURLException {

		if (StringUtils.isNotBlank(PROPERTY_LOCATION)) {
			if (isLocalFile()) {
				runDaemon();
			} else if (!isHttp()) {
				throw new ConfigurationException(String.format("Invalid protocol specified for RPC "
					+ "daemon backend in property '%s'. This has to point to a valid "
					+ "HTTP endpoint.", PROPERTY_LOCATION_KEY)
				);
			}
		} else {
			throw new ConfigurationException(String.format("No location to the daemon specified in "
				+ " property '%s'. This should point to either a local file, "
				+ "or a remote HTTP location.", PROPERTY_LOCATION_KEY)
			);
		}
	}

	public void stop() throws InterruptedException {
		if (process.isPresent()) {
			process.get().destroy();
			process.get().waitFor();
		}
	}

	public String getRPCAdress() {

		String s = "";

		try {
			System.out.println(PROPERTY_LOCATION);
			if (isHttp(PROPERTY_LOCATION)) {
				s = PROPERTY_LOCATION;
			} else if (isLocalFile()) {
				s = "http://127.0.0.1:51993";
			}
		} catch (MalformedURLException e) {
			s = "http://127.0.0.1:51993";
		}

		return s;
	}
}
