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

package org.unigrid.bootstrap;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.update4j.Configuration;
import org.update4j.service.Delegate;
import org.update4j.OS;
import javafx.stage.StageStyle;
import io.sentry.Sentry;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.UUID;
import javafx.application.HostServices;
//import ch.qos.logback.classic.Level;
//import ch.qos.logback.classic.Logger;
//import org.slf4j.LoggerFactory;

public class App extends Application implements Delegate {

	private static Scene scene;
	private static FXMLLoader loader;
	private static Map<String, String> inputArgs = new HashMap<String, String>();
	private HostServices hostServices = getHostServices();

	@Override
	public void start(Stage stage) throws IOException {

		//final Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		//root.setLevel(Level.ALL);
		stage.setMinWidth(600);
		stage.setMinHeight(300);

		URL configUrl = null;
		OS os = OS.CURRENT;
		if (!inputArgs.containsKey("URL")) {
			if (os.equals(OS.LINUX)) {
				configUrl = new URL("https://raw.githubusercontent.com/unigrid-project/unigrid-update/main/config-linux.xml");
			} else if (os.equals(OS.WINDOWS)) {
				configUrl = new URL("https://raw.githubusercontent.com/unigrid-project/unigrid-update/main/config-windows.xml");
			} else if (os.equals(OS.MAC)) {
				configUrl = new URL("https://raw.githubusercontent.com/unigrid-project/unigrid-update/main/config-mac.xml");
			}
		} else {
			configUrl = new URL(inputArgs.get("URL"));
		}
		System.out.println(configUrl);

		Configuration config = null;

		try ( Reader in = new InputStreamReader(configUrl.openStream(), StandardCharsets.UTF_8)) {
			System.out.println("are we getting here??????");
			config = Configuration.read(in);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			Sentry.captureException(e);
			try ( Reader in = Files.newBufferedReader(Paths.get(System.getProperty("user.home"), "/work/janus-java/config/target/config.xml"))) {
				System.out.println("reading local config xml");
				config = Configuration.read(in);
			}
		}

		if (inputArgs.get("test") == null) {
			String server = "";
			final String version = config.getProperties("fx.version").get(0).getValue();
			Sentry.init(options -> {
				options.setDsn("https://18a30d2bf41643ce9efe84a451ecef1a@o266736.ingest.sentry.io/6632466");
				// Set tracesSampleRate to 1.0 to capture 100% of transactions for performance monitoring.
				// We recommend adjusting this value in production.
				options.setServerName(cryptCompName());
				options.setTag("os", OS.CURRENT.getShortName());
				options.setRelease(version);
				options.setEnvironment("production");
				options.setTracesSampleRate(0.1);
				options.setDebug(false);
			});
		}

		config.sync();
		scene = new Scene(loadFXML("updateView"));
		stage.initStyle(StageStyle.UNDECORATED);
		stage.centerOnScreen();
		stage.setResizable(false);
		stage.setScene(scene);
		stage.show();

		UpdateView.getInstance().setConfig(config, stage, inputArgs, hostServices);

	}

	static void setRoot(String fxml) throws IOException {
		scene.setRoot(loadFXML(fxml));
	}

	private static Parent loadFXML(String fxml) throws IOException {
		FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
		loader = fxmlLoader;
		return fxmlLoader.load();
	}

	public static void main(String[] args) {
		if (args != null) {
			for (String arg : args) {
				System.out.println(arg);
				if (arg.contains("=")) {
					String key = arg.split("=")[0];
					String value = arg.split("=")[1];
					inputArgs.put(key, value);
				}
			}
		}
		launch();
	}

	@Override
	public void main(List<String> list) throws Throwable {
		launch();
	}

	private String cryptCompName() {
		String s = "";
		try {
			InetAddress localHost = InetAddress.getLocalHost();
			Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
			while (nis.hasMoreElements()) {
				NetworkInterface ni = nis.nextElement();
				System.out.println(ni.getName());
				if (ni != null) {
					byte[] name = ni.getHardwareAddress();
					byte[] salt = "31".getBytes();
					byte[] result = joinBytes(name, salt);
					UUID uuid = UUID.nameUUIDFromBytes(result);
					s = uuid.toString();
					break;
				}
			}
			System.out.println(s);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return s;
	}

	private byte[] joinBytes(byte[] byteArray1, byte[] byteArray2) {
		final int finalLength = byteArray1.length + byteArray2.length;
		final byte[] result = new byte[finalLength];

		System.arraycopy(byteArray1, 0, result, 0, byteArray1.length);
		System.arraycopy(byteArray2, 0, result, byteArray1.length, byteArray2.length);
		return result;
	}
}
