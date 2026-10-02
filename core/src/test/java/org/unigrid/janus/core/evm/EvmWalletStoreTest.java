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
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import org.unigrid.janus.core.legacy.LegacyKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.unigrid.janus.core.OwnerOnlyAssertions.assertOwnerOnly;

public class EvmWalletStoreTest {
	private static final String COMPRESSED_WIF = "PdiUUh8dnXB36B2XcbPdodUX5Ujoj2VDubJwcW1V8DJu6eQQxzNx";

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

	private static EvmWallet withLegacyKeys() {
		final List<LegacyKey> keys = List.of(LegacyKey.parse(COMPRESSED_WIF),
			LegacyKey.parse("68QtzUftP6UedWuuhgxsw4jV7TDsTjvyPnqUKvFA6G2LWUipq9J")
		);

		return EvmWallet.create(EvmWalletTest.PHRASE, "pw", EvmWalletTest.VAULT, new LegacyVault(), keys);
	}

	@Example
	public void shouldReadBackTheLegacyBlockItSaved() {
		final EvmWallet wallet = withLegacyKeys();
		final EvmWallet read = EvmWalletStore.read(store.save(wallet));

		assertEquals(wallet, read);
		assertEquals(2, read.legacy().addresses().size());
		assertEquals(2, read.legacyKeys("pw", EvmWalletTest.VAULT, new LegacyVault()).size());
	}

	@Example
	public void shouldKeepNoLegacyPrivateKeyInTheClear() throws IOException {
		final byte[] secret = new byte[32];
		final String written = Files.readString(store.save(withLegacyKeys()), StandardCharsets.UTF_8);

		secret[31] = 2;
		assertFalse(written.contains(COMPRESSED_WIF));
		assertFalse(written.contains(HexFormat.of().formatHex(secret)));
		assertTrue(written.contains("H78V5Mwegfjmemi2rMuVg93c8AwjirUdQH"));
	}

	@Example
	public void shouldWriteAPlainWalletWithoutALegacyBlock() throws IOException {
		final String written = Files.readString(
			store.save(EvmWallet.create(EvmWalletTest.PHRASE, "pw", EvmWalletTest.VAULT)), StandardCharsets.UTF_8
		);

		assertFalse(written.contains("legacy"));
	}

	@Example
	public void shouldFindTheWalletKeptForAnAddressAndNothingWhenThereIsNone() throws IOException {
		final EvmWallet wallet = withLegacyKeys();
		final String address = wallet.addresses().get(0);

		assertEquals(Optional.empty(), store.find(address));
		store.save(wallet);
		assertEquals(Optional.of(wallet), store.find(address));
		assertEquals(Optional.empty(), store.find("0xSomeoneElse"));
	}

	@Example
	public void shouldFindNothingInAFileThatIsNoWallet() throws IOException {
		Files.createDirectories(folder);
		Files.writeString(folder.resolve("evm-0xBroken.json"), "not json");
		assertEquals(Optional.empty(), store.find("0xBroken"));
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
