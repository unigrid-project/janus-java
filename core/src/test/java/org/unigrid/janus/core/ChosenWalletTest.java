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
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ChosenWalletTest {
	private Path home;
	private Path file;
	private Path backup;
	private ChosenWallet chosen;

	@BeforeTry
	public void prepare() throws IOException {
		home = Files.createTempDirectory("janus");
		file = home.resolve(".janus").resolve("wallet");
		backup = Files.createFile(home.resolve("wallet-20260927-101010.dat"));
		chosen = new ChosenWallet(file);
	}

	@AfterTry
	public void clean() throws IOException {
		try (Stream<Path> paths = Files.walk(home)) {
			for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(path);
			}
		}
	}

	@Example
	public void shouldRememberNothingAtFirst() {
		assertEquals(Optional.empty(), chosen.remembered());
	}

	@Example
	public void shouldRememberTheBackupAcrossInstances() {
		chosen.remember(backup);
		assertEquals(Optional.of(backup.toAbsolutePath()), new ChosenWallet(file).remembered());
	}

	@Example
	public void shouldKeepWhatItRemembersToItsOwner() throws IOException {
		chosen.remember(backup);
		assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(file)));
	}

	@Example
	public void shouldForgetABackupThatIsGone() throws IOException {
		chosen.remember(backup);
		Files.delete(backup);
		assertEquals(Optional.empty(), chosen.remembered());
		assertFalse(Files.exists(file));
	}

	@Example
	public void shouldForgetWhenAsked() {
		chosen.remember(backup);
		chosen.forget();
		assertEquals(Optional.empty(), chosen.remembered());
		assertFalse(Files.exists(file));
	}
}
