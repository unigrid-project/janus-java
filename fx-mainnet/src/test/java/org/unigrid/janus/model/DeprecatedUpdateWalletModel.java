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
import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.client.Client;
import java.beans.PropertyChangeSupport;
import java.util.Map;
import java.util.TimerTask;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.unigrid.janus.model.service.DebugService;
import org.update4j.Configuration;
import org.update4j.OS;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.unigrid.janus.model.entity.Feed;

@ApplicationScoped
public class DeprecatedUpdateWalletModel extends TimerTask {

	private final String linuxPath = System.getProperty("user.home").concat("/.unigrid/dependencies/temp/");
	private final String macPath = System.getProperty("user.home")
		.concat("/Library/Application\\ Support/UNIGRID/dependencies/temp/");
	private final String windowsPath = System.getProperty("user.home")
		.concat("/AppData/Roaming/UNIGRID/dependencies/temp/");

	private DebugService debug;

	private static final String BASE_URL = "https://raw.githubusercontent.com/unigrid-project/unigrid-update/main/%s";
	private static final String BOOTSTRAP_URL = UpdateURL.getBootstrapUrl();

	private OS os = OS.CURRENT;
	private int exitCode = 0;

	private static final Map<?, ?> OS_CONFIG = ArrayUtils.toMap(new Object[][]{
		{OS.LINUX, UpdateURL.getLinuxUrl()},
		{OS.WINDOWS, UpdateURL.getWindowsUrl()},
		{OS.MAC, UpdateURL.getMacUrl()}
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

	public DeprecatedUpdateWalletModel() {
		debug = CDI.current().select(DebugService.class).get();

		System.out.println("Init walletUpdate");

		initWebTarget();
	}

	public Feed initWebTarget() {
		try {
			System.out.println(BOOTSTRAP_URL);
			client = ClientBuilder.newBuilder()
				.build();
			Response response = client.target(BOOTSTRAP_URL)
				.request(MediaType.APPLICATION_XML_TYPE).get();
			//System.out.println(response.readEntity(String.class));
			Feed feed = response.readEntity(Feed.class);
			return feed;
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return null;
		}
	}

	@Override
	public void run() {
	}

	public Boolean checkUpdateBootstrap() {
		String filteredVer = BootstrapModel.getBootstrapVer();
		System.out.println("getBootstrapVer in UpdateWallet Check: " + BootstrapModel.getBootstrapVer());

		//TODO: Move "VersionNumber" to a seperate class with a comparator so we can clean this up
		if ((getVersionNumber(filteredVer, 0) == getVersionNumber(getLatestVersion(), 0))
			&& (getVersionNumber(filteredVer, 2) == getVersionNumber(getLatestVersion(), 2))
			&& (getVersionNumber(filteredVer, 4) == getVersionNumber(getLatestVersion(), 4))
			|| getLatestVersion().equals("")) {

			BootstrapModel.setBootstrapUpdate(false);
			debug.print("VERSION: " + filteredVer, DeprecatedUpdateWalletModel.class.getSimpleName());
			System.out.println("The latest version of the bootstrap is the same as the one we have");
		} else {
			BootstrapModel.setBootstrapUpdate(true);
		}
		System.out.println("are we upadting the bootstrap: " + BootstrapModel.isBootstrapUpdate());
		return BootstrapModel.isBootstrapUpdate();
	}

	public String getLatestVersion() {
		githubJson = initWebTarget();

		if (githubJson == null) {
			System.out.println("githubjson is null");
			return "";
		}

		String githubEntry = githubJson.getEntry().get(0).getId();

		if (githubEntry == null || githubEntry.equals("")) {
			return "";
		}

		githubEntry = githubEntry.split("/")[2].substring(1);
		System.out.println("Github tag for this version: " + githubEntry);

		return githubEntry;
	}

	public int getVersionNumber(String version, int index) {
		char[] c = version.toCharArray();

		String majorVersion = String.valueOf(c[index]);
		return Integer.parseInt(majorVersion);
	}
}
