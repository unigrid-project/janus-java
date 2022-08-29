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
import jakarta.ws.rs.client.Client;
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

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Objects;
import java.util.Properties;
import javafx.application.Platform;
import org.apache.commons.io.FileUtils;
import org.unigrid.janus.Janus;
import org.unigrid.janus.model.entity.Feed;

@Eager
@ApplicationScoped
public class UpdateWallet extends TimerTask {

	private final String linuxPath = System.getProperty("user.home").concat("/.unigrid/dependencies/temp/");
	private final String macPath = System.getProperty("user.home")
			.concat("/Library/Application\\ Support/UNIGRID/dependencies/temp/");
	private final String windowsPath = System.getProperty("user.home")
			.concat("/AppData/Roaming/UNIGRID/dependencies/temp/");

	private static DebugService debug = new DebugService();
	private static final String BASE_URL = "https://raw.githubusercontent.com/unigrid-project/unigrid-update/main/%s";
	// private static PollingService polling = new PollingService();
	private OS os = OS.CURRENT;
	private static final Map<?, ?> OS_CONFIG = ArrayUtils.toMap(new Object[][] {
		{OS.LINUX, "config-linux.xml"},
		{OS.WINDOWS, "config-windows.xml"},
		{OS.MAC, "config-mac.xml"}
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
	private Client client;
	private Feed githubJson;

	public UpdateWallet() {
		System.out.println("Init walletUpdate");

		initWebTarget();
		if (this.pcs != null) {
			// TODO: Should this really be a fallthrough ? It looks dangerous.
			return;
		}

		this.pcs = new PropertyChangeSupport(this);
	}

	private Feed initWebTarget() {
		try {
			client = ClientBuilder.newBuilder()
				.build();
			Response response = client.target("https://github.com/unigrid-project/janus-java/releases.atom")
				.request(MediaType.APPLICATION_XML_TYPE).get();
			//System.out.println(response.readEntity(String.class));
			Feed feed = response.readEntity(Feed.class);
			return feed;
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.out.println(e.getCause().toString());
			return null;
		}
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

		/*if (checkUpdateBootstrap()) {

			this.pcs.firePropertyChange(this.UPDATE_PROPERTY, oldValue, UpdateState.UPDATE_READY);

			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					if (SystemUtils.IS_OS_MAC_OSX) {
						Notifications.create().title("Update Ready")
							.text("New launcher update ready \n"
								+ "Please press the update button!")
							.position(Pos.TOP_RIGHT).showInformation();
					} else {
						Notifications.create().title("Update Ready")
							.text("New launcher update ready \n"
								+ "Please press the update button!")
							.showInformation();
					}
				}
			});


		} else */
		if (checkUpdate()) {

		} else if (checkUpdate()) {
			this.pcs.firePropertyChange(this.UPDATE_PROPERTY, oldValue, UpdateState.UPDATE_READY);

			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					if (SystemUtils.IS_OS_MAC_OSX) {
						Notifications.create().title("Update Ready")
							.text("New update ready \nPleas press the update button!")
							.position(Pos.TOP_RIGHT).showInformation();
					} else {
						Notifications.create().title("Update Ready")
							.text("New update ready \nPleas press the update button!")
							.showInformation();
					}
				}
			});
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
			if (updateConfig == null) {
				debug.print("null config", UpdateWallet.class.getSimpleName());
			}
			update = updateConfig.requiresUpdate();
		} catch (IOException e) {
			update = false;
		}
		System.out.println("Is thier an update ready = " + update);
		return update;
	}

	private Boolean checkUpdateBootstrap() {
		String delimiter = ".";
		Properties myProperties = new Properties();

		try {
			myProperties.load(Janus.class.getResourceAsStream("application.properties"));
		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.out.println(e.getCause().toString());
		}

		String fullVer = Objects.requireNonNull((String) myProperties.get("proj.ver"));
		String filteredVer = fullVer.replace("-SNAPSHOT", "");

		if (getVersionNumber(filteredVer, 0) < getVersionNumber(getLatestVersion(), 0)
			|| getVersionNumber(filteredVer, 2) < getVersionNumber(getLatestVersion(), 0)
			|| getLatestVersion().equals("")) {
			bootstrapUpdate = false;
			System.out.println("The latest version of the bootstrap is the same as the one we have");
		} else {
			if (OS.CURRENT == OS.LINUX) {
				Path path = Paths.get(linuxPath);
				System.out.println("downloading linux installer");
				if (getLinuxIDLike().equals("debian")
					&& !checkTempFolder(getDEBFileName(getLatestVersion()), linuxPath)) {
					downloadFile(getDownloadURL(getLatestVersion(),
						getDEBFileName(getLatestVersion())),
						linuxPath,
						getDEBFileName(getLatestVersion()));
				} else {
					downloadFile(getDownloadURL(getLatestVersion(),
						getRPMFileName(getLatestVersion())),
						linuxPath,
						getRPMFileName(getLatestVersion()));
				}
				System.out.println("Did it start??");
			} else if (OS.CURRENT == OS.MAC
				&& !checkTempFolder(getDMGFileName(getLatestVersion()), macPath)) {
				downloadFile(getDownloadURL(getLatestVersion(), getDMGFileName(getLatestVersion())),
					macPath,
					getDMGFileName(getLatestVersion()));
			} else if (OS.CURRENT == OS.WINDOWS
				&& !checkTempFolder(getMSIFileName(getLatestVersion()), windowsPath)) {
				downloadFile(getDownloadURL(getLatestVersion(), getMSIFileName(getLatestVersion())),
					windowsPath,
					getMSIFileName(getLatestVersion()));
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
		final Object obj = new Object();
		System.out.println(bootstrapUpdate);
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				boolean isBootstrapUpdate = false;
				if (false) { //(checkUpdateBootstrap()) {
					Process process;
					//TODO: Add RPM install line
					String linuxDebInstallExec = String.format("pkexec dpkg -i %s%s", linuxPath,
						getDEBFileName(getLatestVersion()));
					String linuxRpmInstallExec = String.format("pkexec dpkg -i %s%s", linuxPath,
						getRPMFileName(getLatestVersion()));
					String windowsInstallExec = String.format("msiexec /i %s%s",
						windowsPath.replace("/", "\\"), getMSIFileName(getLatestVersion()));
					System.out.println(linuxDebInstallExec);
					String macInstallExec = macPath + getDMGFileName(getLatestVersion());
					try {
						if (OS.CURRENT == OS.LINUX) {
							if (getLinuxIDLike().equals("debian")) {
								try {
									Process p = Runtime.getRuntime()
										.exec(linuxDebInstallExec);
									p.waitFor();
								} catch (Exception e) {
									System.out.println(e.getMessage());
								}
							} else {
								try {
									Process p = Runtime.getRuntime()
										.exec(linuxRpmInstallExec);
									p.waitFor();
								} catch (Exception e) {
									System.out.println(e.getMessage());
								}
							}
							System.out.println("Did it start??");
						} else if (OS.CURRENT == OS.MAC) {
							try {
								Process p = Runtime.getRuntime()
									.exec(new String[]{"open", macInstallExec});
								int exitCode = p.waitFor();
								System.out.println("exitCode " + exitCode);
								isBootstrapUpdate = true;
							} catch (Exception e) {
								//TODO: handle exception
								System.out.println("cant open dmg: " + e.getMessage());
							}
						} else if (OS.CURRENT == OS.WINDOWS) {
							try {
								System.out.println(windowsInstallExec);
								Process p = Runtime.getRuntime().exec(windowsInstallExec);
								p.waitFor();
							} catch (Exception e) {
								System.out.println(e.getMessage());
							}
						}
					} catch (Exception e) {
						System.out.println(e.getMessage());
					}
				}
				String linuxExec = "/opt/unigrid/bin/Unigrid";
				String macExec = "open -a unigrid";
				String windowsExec = "\"C:\\Program Files\\Unigrid\\Unigrid.exe\"";
				try {
					synchronized (obj) {
						obj.notifyAll();
					}
					System.out.println("!!!!We got passed the notyfiy");
					if (OS.CURRENT == OS.LINUX) {
						System.out.println("run the app agien on linux");
						Runtime.getRuntime().exec(linuxExec);
						System.out.println("Did it start??");
					} else if (OS.CURRENT == OS.MAC && !isBootstrapUpdate) {
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
		synchronized (obj) {
			try {
				System.out.println("We are waiting!!!!!!!");
				obj.wait();
				System.out.println("we are done waiting!!!!!!");
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
		}
		System.exit(0);
	}

	private static String getFirstKeywordMatch(String s, String keyword) {
		String[] parts = s.split("=");

		for (String part : parts) {
			if (part.contains(keyword)) {
				return s.split("=")[1];
			}
		}

		return null;
	}

	private void downloadFile(String url, String path, String fileName) {
		try {
			FileUtils.copyURLToFile(new URL(url), new File(path + fileName));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	private String getDEBFileName(String version) {
		return String.format("unigrid_%s_amd64.deb", version);
	}

	private String getDMGFileName(String version) {
		return String.format("Unigrid-%s.dmg", version);
	}

	private String getMSIFileName(String version) {
		return String.format("Unigrid-%s.msi", version);
	}

	private String getRPMFileName(String version) {
		return String.format("unigrid-%s.x86_64.rpm", version);
	}

	private String getLatestVersion() {
		githubJson = initWebTarget();
		if (githubJson == null) {
			System.out.println("githubjson is null");
			return "";
		}
		String githubEntry = githubJson.getEntry().get(0).getId();
		if (githubEntry.equals("") || githubEntry == null) {
			return "";
		}
		githubEntry = githubEntry.split("/")[2].substring(1);
		System.out.println("Github tag for this version: " + githubEntry);
		return githubEntry;
	}

	private String getDownloadURL(String version, String fileName) {
		return String.format("https://github.com/unigrid-project/janus-java/releases/download/v%s/%s",
			version, fileName);
	}

	private String getLinuxIDLike() {
		String s = "";
		Map<String, String> osDetails = new HashMap<String, String>();
		try {
			Process process = Runtime.getRuntime().exec("cat /etc/os-release");
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			while ((s = br.readLine()) != null) {
				String[] args = s.split("=");
				osDetails.put(args[0], args[1]);
				System.out.println(s);
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		return osDetails.get("ID_LIKE");
	}

	private int getVersionNumber(String version, int index) {
		char[] c = version.toCharArray();

		String majorVersion = String.valueOf(c[index]);
		return Integer.parseInt(majorVersion);
	}
}
