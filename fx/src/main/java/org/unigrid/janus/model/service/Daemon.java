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

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.naming.ConfigurationException;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.unigrid.janus.model.Preferences;
import org.unigrid.janus.model.cdi.Eager;
import org.unigrid.janus.model.rpc.entity.GetBlockCount;
import org.unigrid.janus.view.AlertDialog;
import org.update4j.OS;
import org.unigrid.janus.model.BootstrapModel;

@Eager
@ApplicationScoped
public class Daemon {
	private static final String PROPERTY_LOCATION_KEY = "janus.daemon.location";
	private static final String DEFAULT_PATH_TO_DAEMON_KEY = "path.to.daemon";
	private static File file = new File(System.getProperty("user.dir") + "/bin/");
	private static String[] dirNameOfDaemon = file.list() == null ? new String[] {""} : file.list();

	@Getter
	private String location = "";

	private static final String[] LOCATIONS = new String[] {
			getBaseDirectory()
	};

	private URL primary = null;
	private Optional<Process> process = Optional.empty();
	private static final String[] EXEC = new String[] {"unigridd", "unigridd.exe"};

	@Inject private RPCService rpc;
	@Inject	private DebugService debug;

	@PostConstruct
	@SneakyThrows
	private void init() {
		if (!getDefaultPathToDaemon().equals("")) {
			System.out.println("The path is set to default");
			return;
		}

		// String loc = System.getProperty("APPDIR");
		// String root = System.getProperty("ROOTDIR");
		// debug.print("$APPDIR "+ loc, Daemon.class.getSimpleName());
		// debug.print("$ROOTDIR "+ root, Daemon.class.getSimpleName());

		for (int i = 0; i < LOCATIONS.length; i++) {
			for (int j = 0; j < EXEC.length; j++) {
				System.out.println(LOCATIONS[i] + EXEC[j]);
				debug.print(LOCATIONS[i] + EXEC[j], Daemon.class.getSimpleName());
				if (isLocalFile(LOCATIONS[i] + EXEC[j])) {
					debug.print(LOCATIONS[i] + EXEC[j], Daemon.class.getSimpleName());
					primary = new URL("file:// " + LOCATIONS[i] + EXEC[j]);
					break;
				}
			}
		}
		System.out.println("init after for");

		if (primary != null && isLocalFile(primary.getFile())) {
			location = Preferences.PROPS.getString(PROPERTY_LOCATION_KEY, primary.getFile());
		} else {
			location = Preferences.PROPS.getString(PROPERTY_LOCATION_KEY, "http://127.0.0.1:51993");
		}

		System.out.println("end of init");

	}

	private void runDaemon() throws IOException {
		debug.print("starting daemon", Daemon.class.getSimpleName());
		String testnet = BootstrapModel.isTestnet() ? "-testnet" : "-server";
		ProcessBuilder pb = new ProcessBuilder(location, testnet);
		process = Optional.of(pb.start());
	}

	private boolean isDaemonRunning() {
		boolean isRunning;

		try {
			rpc.call(new GetBlockCount.Request(), GetBlockCount.class);
			isRunning = false;
		} catch (jakarta.ws.rs.ProcessingException e) {

			isRunning = true;
		}

		return isRunning;
	}

	public boolean isHttp() throws MalformedURLException {
		return "http".equals(new URL(location).getProtocol());
	}

	public boolean isHttp(String value) throws MalformedURLException {
		return "http".equals(new URL(value).getProtocol());
	}

	public boolean isLocalFile(String file) {
		return new File(file).exists();
	}

	public boolean isLocalFile() {
		return isLocalFile(location);
	}

	public void start() throws ConfigurationException, IOException, MalformedURLException {

		if (StringUtils.isNotBlank(location)) {
			if (isLocalFile()) {
				runDaemon();
			} else if (!isHttp()) {
				throw new ConfigurationException(String.format("Invalid protocol specified for RPC "
						+ "daemon backend in property '%s'. This has to point to a valid "
						+ "HTTP endpoint.", PROPERTY_LOCATION_KEY));
			} else {
				debug.print("findFile", Daemon.class.getSimpleName());
				findFile();
				start();
			}
		} else {
			/**
			 * throw new ConfigurationException(String.format("No location to the daemon
			 * specified in " + " property
			 * '%s'. This should point to either a local file, " + "or a remote HTTP
			 * location.",
			 * PROPERTY_LOCATION_KEY)
			 * );*
			 */

			findFile();
			start();
		}
	}

	@SneakyThrows
	private void findFile() {
		AlertDialog.openVerbose(AlertType.INFORMATION,
			"Unigrid backend program not found!", "Set the path to unigridd"
		);

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Pick file unigridd");
		File temp = fileChooser.showOpenDialog(new Stage());

		if (temp.getAbsoluteFile() == null) {
			return;
		}

		location = temp.getAbsolutePath();
		addPathAsDefault(location);
	}

	@SneakyThrows
	private void addPathAsDefault(String path) {
		Preferences.get().put(DEFAULT_PATH_TO_DAEMON_KEY, path);
	}

	@SneakyThrows
	private String getDefaultPathToDaemon() {
		// location = Preferences.get().get(DEFAULT_PATH_TO_DAEMON_KEY, location);
		location = "";
		System.out.println("Get path from config " + location);
		return location;
	}

	public void stop() throws InterruptedException {
		if (process.isPresent()) {
			process.get().destroy();
			process.get().waitFor();
		}
	}

	public String getRPCAdress() {
		try {
			if (isHttp(location)) {
				return location;
			}
		} catch (MalformedURLException e) {
			/* Empty on purose */
		}
		if (BootstrapModel.isTestnet()) {
			return "http://127.0.0.1:51995";
		}

		if (BootstrapModel.getInstance().isTesting()) {
			return "http://127.0.0.1:51995";
		}

		return "http://127.0.0.1:51993";
	}

	private static String getBaseDirectory() {
		String blockRoot = "";
		switch (OS.CURRENT) {
			case LINUX:
				blockRoot = System.getProperty("user.home").concat("/.unigrid/dependencies/bin/");
				break;
			case WINDOWS:
				blockRoot = System.getProperty("user.home")
					.concat("/AppData/Roaming/UNIGRID/dependencies/bin/");
				break;
			case MAC:
				blockRoot = System.getProperty("user.home")
					.concat("/Library/Application Support/UNIGRID/dependencies/bin/");
				break;
			default:
				blockRoot = System.getProperty("user.home").concat("/UNIGRID/dependencies/bin/");
				break;
		}

		File depenendencies = new File(blockRoot);

		if (!depenendencies.exists()) {
			depenendencies.mkdirs();
		}

		return blockRoot;
	}
}
