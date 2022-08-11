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
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TimerTask;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;
import org.controlsfx.control.Notifications;
import org.unigrid.janus.model.cdi.Eager;
import org.unigrid.janus.model.service.DebugService;
//import org.unigrid.janus.model.service.PollingService;
import org.update4j.Configuration;
import org.update4j.OS;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Objects;
import java.util.Properties;
import javafx.application.Platform;
import org.apache.commons.io.FileUtils;
import org.unigrid.janus.Janus;
import org.unigrid.janus.model.entity.GithubJson;
import org.unigrid.janus.model.entity.GithubJson.Asset;

@Eager
@ApplicationScoped
public class UpdateWallet extends TimerTask {

	final private String LINUX_PATH = System.getProperty("user.home").concat("/.unigrid/dependencies/temp/");
	final private String MAC_PATH = System.getProperty("user.home")
		.concat("/Library/Application Support/UNIGRID/dependencies/temp/");
	final private String WINDOWS_PATH = System.getProperty("user.home")
		.concat("/AppData/Roaming/UNIGRID/dependencies/temp/");

	private static DebugService debug = new DebugService();
	private static final String BASE_URL = "https://raw.githubusercontent.com/unigrid-project/unigrid-update/main/%s";
	//private static PollingService polling = new PollingService();
	private OS os = OS.CURRENT;
	private static final Map<?, ?> OS_CONFIG = ArrayUtils.toMap(new Object[][]{
		{OS.LINUX, "config-linux.xml"}, {OS.WINDOWS, "config-windows.xml"}, {OS.MAC, "config-mac.xml"}
	});

	public enum UpdateState {
		UPDATE_READY,
		UPDATE_NOT_READY
	}

	private boolean bootstrapUpdate = false;

	@Getter
	private static final String UPDATE_PROPERTY = "update";

	private UpdateState oldValue = UpdateState.UPDATE_NOT_READY;
	private Configuration updateConfig = null;
	private SimpleBooleanProperty running;
	private static PropertyChangeSupport pcs;
	private WebTarget target;
	private GithubJson githubJson;

	public UpdateWallet() {
		System.out.println("Init walletUpdate");

		initWebTarget();
		if (this.pcs != null) {
			// TODO: Should this really be a fallthrough ? It looks dangerous.
			return;
		}

		this.pcs = new PropertyChangeSupport(this);
	}

	private void initWebTarget() {
		target = ClientBuilder.newBuilder()
			.build().target("https://api.github.com/repos/unigrid-project/janus-java/releases/latest");
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.removePropertyChangeListener(listener);
	}

	@Override
	public void run() {
		if (pcs == null) {
			pcs = new PropertyChangeSupport(this);
		}
		if (checkUpdate() || checkUpdateBootstrap()) {
			this.pcs.firePropertyChange(this.UPDATE_PROPERTY, oldValue, UpdateState.UPDATE_READY);

			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					if (SystemUtils.IS_OS_MAC_OSX) {
						Notifications.create().title("Update Ready")
							.text("New update ready \nPleas close application to update!")
							.position(Pos.TOP_RIGHT).showInformation();
					} else {
						Notifications.create().title("Update Ready")
							.text("New update ready \nPleas close application to update!").showInformation();
					}
				}
			});

		} else {
			this.pcs.firePropertyChange(this.UPDATE_PROPERTY, oldValue, UpdateState.UPDATE_READY);
			//debug.print("user.dir: " + System.getProperty("user.dir"), UpdateWallet.class.getSimpleName());
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

		try ( Reader in = new InputStreamReader(configUrl.openStream(), StandardCharsets.UTF_8)) {
			updateConfig = Configuration.read(in);
			System.out.println("Reading the config file");
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		try {
			System.out.println("Checking for update");
			if (updateConfig == null) {
				debug.print("null config", UpdateWallet.class.getSimpleName());
			}
			update = updateConfig.requiresUpdate();
		} catch (IOException e) {
			update = false;
		}

		return update;
	}

	private Boolean checkUpdateBootstrap() {
		String delimiter = ".";
		githubJson = target.request().get()
			.readEntity(GithubJson.class);

		String githubTag = githubJson.getTagName();

		System.out.println("Github tag for this version: " + githubTag);
		for (Asset a : githubJson.getAssets()) {
			System.out.println(a.getBrowserDownloadUrl());
		}

		String[] s = githubTag.substring(1).split(delimiter);
		System.out.println(githubTag.substring(1));
		Properties myProperties = new Properties();

		try {
			myProperties.load(Janus.class.getResourceAsStream("application.properties"));
		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.out.println(e.getCause().toString());
		}

		String fullVer = Objects.requireNonNull((String) myProperties.get("proj.ver"));
		String filteredVer = fullVer.replace("-SNAPSHOT", "");

		System.out.println(filteredVer);
		String[] existingVersion = filteredVer.split(delimiter);
		System.out.println(s.length);
		System.out.println(existingVersion.length);
		/*TODO: find out way the split dose not work as inteded. Its wierd!!!
		if (!s[0].equals(existingVersion[0]) || !s[1].equals(existingVersion[1])) {
			b = true;
			bootstrapUpdate = true;
		} else {
			b = false;
			bootstrapUpdate = false;
		}*/
		if (githubTag.substring(1).equals(filteredVer)) {
			bootstrapUpdate = false;
		} else {
			if (OS.CURRENT == OS.LINUX
				&& !checkTempFolder(githubJson.getAssets().get(3).getName(), LINUX_PATH)) {
				System.out.println("downloading linux installer");
				downloadFile(githubJson.getAssets().get(3)
					.getBrowserDownloadUrl(),
					LINUX_PATH,
					githubJson.getAssets().get(3).getName());
				System.out.println("Did it start??");
			} else if (OS.CURRENT == OS.MAC
				&& !checkTempFolder(githubJson.getAssets().get(0).getName(), MAC_PATH)) {
				downloadFile(githubJson.getAssets().get(0)
					.getBrowserDownloadUrl(),
					MAC_PATH,
					githubJson.getAssets().get(0).getName());
			} else if (OS.CURRENT == OS.WINDOWS
				&& !checkTempFolder(githubJson.getAssets().get(1).getName(), WINDOWS_PATH)) {
				downloadFile(githubJson.getAssets().get(1)
					.getBrowserDownloadUrl(),
					WINDOWS_PATH,
					githubJson.getAssets().get(1).getName());
			}
			bootstrapUpdate = true;
		}
		System.out.println("are we upadting the bootstrap: " + bootstrapUpdate);
		return bootstrapUpdate;
	}

	private boolean checkTempFolder(String fileName, String path) {
		boolean b = false;
		try {
			b = new File(path + fileName).exists();
		} catch (Exception e) {
			b = false;
		}
		return b;
	}

	private void restartWallet() {
		// TODO: move code from WindowBarController here
	}

	public void doUpdate() {
		System.out.println(bootstrapUpdate);
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				if (checkUpdateBootstrap()) {
					String linuxInstallExec = "pkexcec dpkg -i " + LINUX_PATH
						+ githubJson.getAssets().get(3).getName();
					String macInstallExec = "open " + MAC_PATH + githubJson.getAssets()
						.get(0).getName();
					String windowsInstallExec ="msiexec /i" + WINDOWS_PATH + githubJson.getAssets()
						.get(1).getName();
					System.out.println(linuxInstallExec);
					try {
						if (OS.CURRENT == OS.LINUX) {
							System.out.println("downloading linux installer");
							downloadFile(githubJson.getAssets().get(3)
								.getBrowserDownloadUrl(),
								LINUX_PATH,
								githubJson.getAssets().get(3).getName());
							Runtime.getRuntime().exec(linuxInstallExec);
							System.out.println("Did it start??");
						} else if (OS.CURRENT == OS.MAC) {
							downloadFile(githubJson.getAssets().get(0)
								.getBrowserDownloadUrl(),
								LINUX_PATH,
								githubJson.getAssets().get(0).getName());
							Runtime.getRuntime().exec(macInstallExec);
						} else if (OS.CURRENT == OS.WINDOWS) {
						}
					} catch (Exception e) {
						System.out.println(e.getMessage());
					}
				}
				String linuxExec = "/opt/unigrid/bin/Unigrid";
				String macExec = "open -a unigrid";
				String windowsExec = "c:/programFiles/unigrid/bin/unigrid.exe";
				try {
					if (OS.CURRENT == OS.LINUX) {
						System.out.println("run the app agien on linux");
						Runtime.getRuntime().exec(linuxExec);
						System.out.println("Did it start??");
					} else if (OS.CURRENT == OS.MAC) {
						Runtime.getRuntime().exec(macExec);
					} else if (OS.CURRENT == OS.WINDOWS) {
						Runtime.getRuntime().exec(windowsExec);
					}
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
			}
		});
		t.start();
		System.exit(0);
	}

	private void downloadFile(String url, String path, String fileName) {
		try {
			FileUtils.copyURLToFile(new URL(url), new File(path + fileName));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}
