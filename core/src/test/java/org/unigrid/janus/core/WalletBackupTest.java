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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.stream.Stream;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WalletBackupTest {
	private static final Clock NOON = Clock.fixed(Instant.parse("2026-09-22T12:30:05Z"), ZoneOffset.UTC);
	private static final byte[] KEYS = {1, 2, 3, 4};

	private Path root;
	private Path folder;
	private Path wallet;
	private WalletBackup backup;

	@BeforeTry
	public void prepareAWallet() throws IOException {
		root = Files.createTempDirectory("janus");
		folder = root.resolve("backups");
		wallet = Files.write(root.resolve("wallet.dat"), KEYS);
		backup = new WalletBackup(folder, NOON);
	}

	@AfterTry
	public void removeEverything() throws IOException {
		try (Stream<Path> paths = Files.walk(root)) {
			for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(path);
			}
		}
	}

	@Example
	public void shouldCopyTheWalletIntoAFolderItCreates() throws IOException {
		final Path copy = backup.backup(wallet);

		assertEquals(folder.resolve("wallet-20260922-123005.dat"), copy);
		assertArrayEquals(KEYS, Files.readAllBytes(copy));
	}

	@Example
	public void shouldKeepAnEarlierCopyMadeTheSameSecond() throws IOException {
		final Path first = backup.backup(wallet);

		Files.write(wallet, new byte[] {9});

		final Path second = backup.backup(wallet);

		assertEquals(folder.resolve("wallet-20260922-123005-2.dat"), second);
		assertArrayEquals(KEYS, Files.readAllBytes(first));
		assertArrayEquals(new byte[] {9}, Files.readAllBytes(second));
	}

	@Example
	public void shouldKeepTheCopiesFromOtherUsers() throws IOException {
		final Path copy = backup.backup(wallet);

		if (Files.getFileStore(root).supportsFileAttributeView("posix")) {
			assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(folder)));
			assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(copy)));
		}
	}

	@Example
	public void shouldSayWhichWalletCouldNotBeCopied() {
		final Path missing = root.resolve("gone.dat");
		final UncheckedIOException thrown = assertThrows(UncheckedIOException.class,
			() -> backup.backup(missing)
		);

		assertTrue(thrown.getMessage().contains(missing.toString()), thrown.getMessage());
	}
}
