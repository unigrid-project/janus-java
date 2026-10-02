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
import java.time.Clock;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WalletChoiceTest {
	/* The magic of a Berkeley DB btree where a wallet.dat has it, then some bytes standing in for its keys. */
	private static final byte[] KEYS = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x62, 0x31, 0x05, 0, 1, 2, 3};
	private static final String DUMP = "# Wallet dump created by UNIGRID 2.9.17\n"
		+ "PdiUUh8dnXB36B2XcbPdodUX5Ujoj2VDubJwcW1V8DJu6eQQxzNx 2018-01-02T10:00:00Z label=\n";

	private Path root;
	private Path backups;
	private Path wallet;
	private WalletChoice choice;

	@BeforeTry
	public void prepareAWallet() throws IOException {
		root = Files.createTempDirectory("janus");
		backups = root.resolve("backups");
		wallet = Files.write(root.resolve("wallet.dat"), KEYS);
		choice = new WalletChoice(new WalletBackup(backups, Clock.systemUTC()));
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
	public void shouldHaveNothingChosenToBeginWith() {
		assertEquals(Optional.empty(), choice.chosen());
		assertEquals(Optional.empty(), choice.backup());
	}

	@Example
	public void shouldRememberTheWalletThatWasChosen() {
		choice.choose(wallet);
		assertEquals(Optional.of(wallet), choice.chosen());
	}

	@Example
	public void shouldBackUpTheWalletAsItIsChosen() throws IOException {
		choice.choose(wallet);

		final Path copy = choice.backup().orElseThrow();

		assertEquals(backups, copy.getParent());
		assertArrayEquals(KEYS, Files.readAllBytes(copy));
	}

	@Example
	public void shouldRememberAWalletDumpButNeverCopyItsKeysIntoTheBackups() throws IOException {
		final Path dump = Files.writeString(root.resolve("wallet.dump"), DUMP);

		choice.choose(dump);
		assertEquals(Optional.of(dump), choice.chosen());
		assertEquals(Optional.empty(), choice.backup());
		assertTrue(Files.notExists(backups));
	}

	@Example
	public void shouldLetAWalletDumpTakeTheChoiceOfAWalletAndBack() throws IOException {
		final Path dump = Files.writeString(root.resolve("wallet.dump"), DUMP);

		choice.choose(wallet);
		choice.choose(dump);
		assertEquals(Optional.empty(), choice.backup());
		choice.choose(wallet);
		assertTrue(choice.backup().isPresent());
	}

	@Example
	public void shouldNotBackUpTheSameChoiceTwice() throws IOException {
		choice.choose(wallet);
		choice.choose(wallet);

		try (Stream<Path> copies = Files.list(backups)) {
			assertEquals(1, copies.count());
		}
	}

	@Example
	public void shouldKeepTheEarlierChoiceWhenTheBackupFails() throws IOException {
		choice.choose(wallet);

		final Path copy = choice.backup().orElseThrow();
		final Path other = Files.write(root.resolve("other.dat"), KEYS);

		Files.delete(copy);
		Files.delete(backups);
		Files.createFile(backups);

		assertThrows(UncheckedIOException.class, () -> choice.choose(other));
		assertEquals(Optional.of(wallet), choice.chosen());
		assertEquals(Optional.of(copy), choice.backup());
	}

	@Example
	public void shouldRefuseAPathWithoutAWalletFile() {
		final Path missing = Path.of("/nowhere/wallet.dat");
		final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> choice.choose(missing)
		);

		assertTrue(thrown.getMessage().contains(missing.toString()), thrown.getMessage());
		assertEquals(Optional.empty(), choice.chosen());
	}
}
