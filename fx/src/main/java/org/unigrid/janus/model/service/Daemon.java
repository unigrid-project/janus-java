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

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.naming.ConfigurationException;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.unigrid.janus.model.Preferences;
import org.unigrid.janus.model.rpc.entity.BlockCount;

@ApplicationScoped
public class Daemon {
	@Inject
	private RPCService rpc;
	private static final String PROPERTY_LOCATION_KEY = "janus.daemon.location";
	private static final String DEFAULT_PATH_TO_DAEMON_KEY = "path.to.daemon";
	
	@Getter private String location;
	private Optional<Process> process = Optional.empty();

	private static final String[] LOCATIONS = new String[] {
		System.getProperty("user.dir") + "/", 
		"/usr/bin/", 
		"/opt/bin/"
	};
	
	private URL primary = null;
	
	private static final String[] EXEC = new String[] { "unigridd", "unigridd.exe" };
	
	@PostConstruct
	@SneakyThrows
	private void init() {
		System.out.println("start init");
		System.out.println("blää");
                System.out.println("2");
		if (!getDefaultPathToDaemon().equals("")){
			System.out.println("The path is set to default");
			return;
		}
		System.out.println("Init Before for");
		for (int i = 0; i < LOCATIONS.length - 1; i++) {
			for (int j = 0; j < EXEC.length - 1; j++) {
				if (isLocalFile(LOCATIONS[i] + EXEC[j])) {
					System.out.println(LOCATIONS[i] + EXEC[j]);
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
		System.out.println("starting daemon");
		
		//if (isDaemonRunning()) {
			process = Optional.of(Runtime.getRuntime().exec(new String[]{ location }));
			
		//}
	}

	private boolean isDaemonRunning() {
		boolean isRunning;

		try {
			rpc.call(new BlockCount(), BlockCount.class);
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
					+ "HTTP endpoint.", PROPERTY_LOCATION_KEY)
				);
			} else {
				findFile();
				start();
			}
		} else {
			/**throw new ConfigurationException(String.format("No location to the daemon specified in "
				+ " property '%s'. This should point to either a local file, "
				+ "or a remote HTTP location.", PROPERTY_LOCATION_KEY)
			);**/
			findFile();
			start();
		}
	}
	
	@SneakyThrows
	private void findFile(){
		
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Unigrid Janus");
		alert.setHeaderText("Unigrid backend program not found!");
		alert.setContentText("Set the path to unigridd");
		alert.showAndWait();
		
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Pick file unigridd");
		
		File temp = fileChooser.showOpenDialog(new Stage());
		if (temp.getAbsoluteFile() == null){ return; }
		location = temp.getAbsolutePath();
		addPathAsDefault(location);
	}
	
	@SneakyThrows
	private void addPathAsDefault(String path){
		Preferences.get().put(DEFAULT_PATH_TO_DAEMON_KEY, path);
	}
	
	@SneakyThrows
	private String getDefaultPathToDaemon(){
		if (Preferences.get().nodeExists(DEFAULT_PATH_TO_DAEMON_KEY)) {
			Preferences.get().get(DEFAULT_PATH_TO_DAEMON_KEY, location);
			System.out.println("Get path from config " + location);
			return location;
		} else {
			return "";
		}
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

		return "http://127.0.0.1:51993";
	}
}
