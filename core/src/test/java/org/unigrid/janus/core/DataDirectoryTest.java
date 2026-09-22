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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import net.jqwik.api.Example;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataDirectoryTest {
	private static final Path HOME = Path.of("/home/ann");

	@Example
	public void shouldBeAHiddenFolderInTheHomeOnLinux() {
		assertEquals(HOME.resolve(".unigrid"), DataDirectory.usual("Linux", HOME, null));
	}

	@Example
	public void shouldBeUnderApplicationSupportOnAMac() {
		assertEquals(HOME.resolve("Library/Application Support/UNIGRID"),
			DataDirectory.usual("Mac OS X", HOME, null)
		);
	}

	@Example
	public void shouldBeUnderRoamingAppDataOnWindows() {
		assertEquals(Path.of("C:/Users/ann/AppData/Roaming/UNIGRID"),
			DataDirectory.usual("Windows 11", HOME, "C:/Users/ann/AppData/Roaming")
		);
	}

	@Example
	public void shouldFallBackToTheHomeWhenWindowsDoesNotSayWhereAppDataIs() {
		assertEquals(HOME.resolve("AppData/Roaming/UNIGRID"), DataDirectory.usual("Windows 11", HOME, null));
	}

	@Example
	public void shouldFindTheWalletTheDaemonLeftBehind() throws IOException {
		final Path directory = Files.createTempDirectory("janus");
		final Path wallet = Files.createFile(directory.resolve("wallet.dat"));

		try {
			assertEquals(Optional.of(wallet), new DataDirectory(directory).wallet());
		} finally {
			Files.delete(wallet);
			Files.delete(directory);
		}
	}

	@Example
	public void shouldAdmitThereIsNoWalletRatherThanNameAMissingOne() throws IOException {
		final Path directory = Files.createTempDirectory("janus");

		try {
			assertEquals(Optional.empty(), new DataDirectory(directory).wallet());
		} finally {
			Files.delete(directory);
		}
	}

	@Example
	public void shouldNotMistakeAFolderForAWallet() throws IOException {
		final Path directory = Files.createTempDirectory("janus");
		final Path folder = Files.createDirectory(directory.resolve("wallet.dat"));

		try {
			assertEquals(Optional.empty(), new DataDirectory(directory).wallet());
		} finally {
			Files.delete(folder);
			Files.delete(directory);
		}
	}
}
