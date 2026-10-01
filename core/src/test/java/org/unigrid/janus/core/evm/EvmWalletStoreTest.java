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

package org.unigrid.janus.core.evm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.stream.Stream;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.unigrid.janus.core.OwnerOnlyAssertions.assertOwnerOnly;

public class EvmWalletStoreTest {
	private Path home;
	private Path folder;
	private EvmWalletStore store;

	@BeforeTry
	public void prepare() throws IOException {
		home = Files.createTempDirectory("janus");
		folder = home.resolve("wallets");
		store = new EvmWalletStore(folder);
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
	public void shouldReadBackTheWalletItSaved() {
		final EvmWallet wallet = EvmWallet.create(EvmWalletTest.PHRASE, "pw", EvmWalletTest.VAULT);

		assertEquals(wallet, EvmWalletStore.read(store.save(wallet)));
	}

	@Example
	public void shouldNameTheFileAfterTheFirstAddress() {
		final Path saved = store.save(EvmWallet.create(EvmWalletTest.PHRASE, "pw", EvmWalletTest.VAULT));

		assertEquals(folder.resolve("evm-0x9858EfFD232B4033E47d90003D41EC34EcaEda94.json"), saved);
		assertTrue(EvmWalletStore.holds(saved));
	}

	@Example
	public void shouldLetOnlyItsOwnerNear() throws IOException {
		final Path saved = store.save(EvmWallet.create(EvmWalletTest.PHRASE, "pw", EvmWalletTest.VAULT));

		assertOwnerOnly(folder);
		assertOwnerOnly(saved);
	}

	@Example
	public void shouldKeepNeitherWordsNorSeedInTheClear() throws IOException {
		final String entropy = HexFormat.of().formatHex(EvmWalletTest.PHRASE.entropy());
		final String written = Files.readString(
			store.save(EvmWallet.create(EvmWalletTest.PHRASE, "pw", EvmWalletTest.VAULT)), StandardCharsets.UTF_8
		);

		assertFalse(written.contains("abandon"));
		assertFalse(written.contains(entropy));
	}

	@Example
	public void shouldReplaceTheWalletWhenItsPhraseIsRestoredAgain() throws IOException {
		store.save(EvmWallet.create(EvmWalletTest.PHRASE, "old", EvmWalletTest.VAULT));

		final Path saved = store.save(EvmWallet.create(EvmWalletTest.PHRASE, "new", EvmWalletTest.VAULT));

		try (Stream<Path> files = Files.list(folder)) {
			assertEquals(1, files.count());
		}

		assertEquals(EvmWalletTest.PHRASE, EvmWalletStore.read(saved).mnemonic("new", EvmWalletTest.VAULT));
	}

	@Example
	public void shouldNameAFileThatIsNoWallet() throws IOException {
		final Path file = Files.writeString(home.resolve("evm-broken.json"), "not json");
		final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> EvmWalletStore.read(file)
		);

		assertEquals(file + " is not a wallet Janus can read", thrown.getMessage());
	}

	@Example
	public void shouldTellAWalletDatFromAnEvmWallet() {
		assertFalse(EvmWalletStore.holds(Path.of("/home/me/.janus/backups/wallet-20260927-101010.dat")));
	}
}
