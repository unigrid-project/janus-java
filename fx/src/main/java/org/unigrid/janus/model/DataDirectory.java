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

import com.sun.jna.platform.win32.KnownFolders;
import com.sun.jna.platform.win32.Shell32Util;
import java.io.File;
import java.nio.file.Paths;
import java.util.Iterator;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.SystemUtils;

public class DataDirectory {
	private static final String APPLICATION_NAME = "UNIGRID";
	public static final String CONFIG_FILE = "unigrid.conf";
	public static final String GRIDNODE_FILE = "masternode.conf";
	public static final String DEBUG_LOG = "debug.log";
	private static final String OSX_SUPPORT_DIR = "Library/Application Support";

	public static final String DATADIR_CONFIG_RPCUSER_KEY = "rpcuser";
	public static final String DATADIR_CONFIG_RPCPASSWORD_KEY = "rpcpassword";

	public static String get() {
		String head;
		String tail;

		if (SystemUtils.IS_OS_WINDOWS) {
			head = Shell32Util.getKnownFolderPath(KnownFolders.FOLDERID_RoamingAppData);
			tail = APPLICATION_NAME;
			System.out.println("OS is windows");
		} else {
			head = SystemUtils.getUserHome().getAbsolutePath();

			if (SystemUtils.IS_OS_MAC_OSX) {
				head = Paths.get(head, OSX_SUPPORT_DIR).toString();
				tail = APPLICATION_NAME;
			} else {
				tail = Paths.get(".".concat(APPLICATION_NAME).toLowerCase()).toString();
			}
		}

		if (BootstrapModel.getInstance().isTesting()) {
			return Paths.get(head, tail, "testnet4").toString();
		}
		return Paths.get(head, tail).toString();
	}

	public static Configuration getConfig(boolean blocking) throws ConfigurationException {
		System.out.println("Create config builder!!!");
		final Parameters parameters = new Parameters();

		FileBasedConfigurationBuilder<FileBasedConfiguration> builder
			= new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
				.configure(parameters.properties().setFile(getConfigFile()));

		// System.out.println("found folder = " + builder.getFileHandler().locate());
		// System.out.println(builder.getFileHandler().getPath());
		System.out.println("Path: ".concat(builder.getFileHandler().getPath()));

		try {
			do {
				System.out.println("init loop DataDirectory!!!");

				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
					System.out.println("Somthing whent wrong with the thread");
				}
			} while (blocking && !builder.getFileHandler().locate());
		} catch (Exception e) {
			System.out.println("dowhile");
		}

		return builder.getConfiguration();
	}

	public static String getConfigKeys() throws ConfigurationException {
		Configuration config = getConfig(true);
		Iterator<String> keys = config.getKeys();
		String result = "";

		while (keys.hasNext()) {
			result = String.format("%s\n%s", result, keys.next());
		}

		return result;
	}

	public static File getConfigFile() {
		return Paths.get(get(), CONFIG_FILE).toFile();
	}

	public static File getDebugLog() {
		return Paths.get(get(), DEBUG_LOG).toFile();
	}

	public static File getGridnodeFile() {
		return Paths.get(get(), GRIDNODE_FILE).toFile();
	}

	public static File getBackendLog() {
		return Paths.get(get(), "debug.log").toFile();
	}

	public static File getWalletLog() {
		return Paths.get(get(), "wallet.log").toFile();
	}
}
