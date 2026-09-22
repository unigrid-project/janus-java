/*
    The Janus Wallet
    Copyright © 2021-2026 Stiftelsen The Unigrid Foundation

    This program is free software: you can redistribute it and/or modify it under the terms of the
    addended GNU Affero General Public License as published by the Free Software Foundation, version 3
    of the License (see COPYING and COPYING.addendum).

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received an addended copy of the GNU Affero General Public License with this program.
    If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.core;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/** Where the daemon keeps its files on this computer, and so where a wallet left behind would be. */
@ApplicationScoped
public class DataDirectory {
	private static final String WALLET = "wallet.dat";
	private static final String NAME = "UNIGRID";
	private static final String HIDDEN_NAME = ".unigrid";
	private static final String MAC_SUPPORT = "Library/Application Support";
	private static final String WINDOWS_ROAMING = "AppData/Roaming";

	private final Path path;

	public DataDirectory() {
		this(usual(System.getProperty("os.name"), Path.of(System.getProperty("user.home")),
			System.getenv("APPDATA")
		));
	}

	public DataDirectory(final Path path) {
		this.path = path;
	}

	/** Where the daemon puts its files by default on the named platform. */
	static Path usual(final String os, final Path home, final String appData) {
		final String platform = os.toLowerCase(Locale.ROOT);

		if (platform.contains("mac")) {
			return home.resolve(MAC_SUPPORT).resolve(NAME);
		}

		if (platform.contains("win")) {
			return (appData == null ? home.resolve(WINDOWS_ROAMING) : Path.of(appData)).resolve(NAME);
		}

		return home.resolve(HIDDEN_NAME);
	}

	public Path path() {
		return path;
	}

	/** The wallet the daemon left here, if there is one. */
	public Optional<Path> wallet() {
		return Optional.of(path.resolve(WALLET)).filter(Files::isRegularFile);
	}
}
