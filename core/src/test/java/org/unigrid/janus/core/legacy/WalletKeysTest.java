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

package org.unigrid.janus.core.legacy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.jqwik.api.Example;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WalletKeysTest {
	private static final String PASSPHRASE = "Unigrid tëst passphrase";
	private static final char[] NO_PASSPHRASE = new char[0];
	private static final String COMPRESSED = "compressed";

	/* In wallet.dat, whose keys are made up: a plain key, and an encrypted one with a master key cut short. */
	private static final String MADE_UP_KEY = "HQPjfHhSHs2rt97BrdGjT1BdL3Yg43yhXe";
	private static final String MADE_UP_ENCRYPTED_KEY = "HHNVubwUgnB59iDrDCUwE8wKAg7qwYbTAe";

	/* The keys make-wallets.py wrote beside a wallet, a line each: the address, the secret in hex, the encoding. */
	private static Map<String, LegacyKey> expected(final String name) throws IOException {
		final Map<String, LegacyKey> keys = new TreeMap<>();

		for (final String line : Files.readAllLines(BerkeleyFileTest.fixture(name + ".keys"))) {
			final String[] fields = line.split(" ");
			keys.put(fields[0], new LegacyKey(HexFormat.of().parseHex(fields[1]), COMPRESSED.equals(fields[2])));
		}

		return keys;
	}

	private static void assertSameKeys(final Map<String, LegacyKey> expected, final List<LegacyKey> keys) {
		assertEquals(expected.size(), keys.size());

		for (final LegacyKey key : keys) {
			final LegacyKey wanted = expected.get(key.address());

			assertNotNull(wanted, key.address());
			assertArrayEquals(wanted.secret(), key.secret(), key.address());
			assertEquals(wanted.compressed(), key.compressed(), key.address());
		}
	}

	/* Among them an uncompressed key, and a wkey and a key without its check hash, as the oldest wallets wrote them;
	   the rest carry a check hash, which is compared. */
	@Example
	public void shouldReadEveryKeyOfAPlainWalletWithoutAPassphrase() throws IOException {
		final Map<String, LegacyKey> expected = expected("plain-wallet");
		final Path wallet = BerkeleyFileTest.fixture("plain-wallet.dat");

		assertSameKeys(expected, WalletKeys.keys(wallet, expected.keySet(), NO_PASSPHRASE));
	}

	@Example
	public void shouldDecryptEveryKeyOfAnEncryptedWalletWithItsPassphrase() throws IOException {
		final Map<String, LegacyKey> expected = expected("encrypted-wallet");
		final Path wallet = BerkeleyFileTest.fixture("encrypted-wallet.dat");
		final char[] passphrase = PASSPHRASE.toCharArray();

		assertSameKeys(expected, WalletKeys.keys(wallet, expected.keySet(), passphrase));
		assertArrayEquals(new char[passphrase.length], passphrase, "the passphrase is wiped once used");
	}

	@Example
	public void shouldGiveOnlyTheKeysAskedFor() throws IOException {
		final Map<String, LegacyKey> expected = expected("encrypted-wallet");
		final List<String> two = expected.keySet().stream().limit(2).toList();
		final Path wallet = BerkeleyFileTest.fixture("encrypted-wallet.dat");
		final List<LegacyKey> keys = WalletKeys.keys(wallet, Set.copyOf(two), PASSPHRASE.toCharArray());

		assertEquals(two, keys.stream().map(LegacyKey::address).sorted().toList());
	}

	@Example
	public void shouldSayAWrongPassphraseIsWrongWithoutQuotingIt() throws IOException {
		final Set<String> addresses = expected("encrypted-wallet").keySet();
		final Path wallet = BerkeleyFileTest.fixture("encrypted-wallet.dat");

		for (final String wrong : List.of("", "Unigrid test passphrase", "wrong")) {
			final char[] passphrase = wrong.toCharArray();
			final WrongPassphrase thrown = assertThrows(WrongPassphrase.class,
				() -> WalletKeys.keys(wallet, addresses, passphrase)
			);

			assertEquals("The passphrase does not open " + wallet, thrown.getMessage());
			assertArrayEquals(new char[passphrase.length], passphrase, "a refused passphrase is wiped too");
		}
	}

	@Example
	public void shouldTellADamagedKeyFromAWrongPassphrase() {
		final Path wallet = BerkeleyFileTest.fixture("damaged-key.dat");
		final Set<String> addresses = LegacyWallet.addresses(wallet);
		final UnreadableWallet damaged = assertThrows(UnreadableWallet.class,
			() -> WalletKeys.keys(wallet, addresses, PASSPHRASE.toCharArray())
		);

		assertEquals("a key does not open under its master key", damaged.reason());
		assertThrows(WrongPassphrase.class, () -> WalletKeys.keys(wallet, addresses, "wrong".toCharArray()));
	}

	@Example
	public void shouldNameAnAddressTheWalletHoldsNoKeyFor() {
		final Path wallet = BerkeleyFileTest.fixture("encrypted-wallet.dat");
		final String someoneElse = "HVrjNTDp7PzvXmiZ1Cf4t9AFogZg5BbcAE";
		final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> WalletKeys.keys(wallet, Set.of(someoneElse), PASSPHRASE.toCharArray())
		);

		assertFalse(thrown instanceof UnreadableWallet, "the wallet is fine, the address is not its own");
		assertEquals(wallet + " holds no key for " + someoneElse, thrown.getMessage());
	}

	@Example
	public void shouldRefuseAKeyWhoseCheckHashDiffers() {
		final Path wallet = BerkeleyFileTest.fixture("bad-hash.dat");
		final String address = LegacyWallet.addresses(wallet).first();
		final UnreadableWallet thrown = assertThrows(UnreadableWallet.class,
			() -> WalletKeys.keys(wallet, Set.of(address), NO_PASSPHRASE)
		);

		assertEquals("the key of " + address + " does not match its check hash", thrown.reason());
	}

	@Example
	public void shouldRefuseKeysTheLegacyWalletCouldNotHaveWritten() {
		final Path wallet = BerkeleyFileTest.fixture("wallet.dat");
		final char[] passphrase = PASSPHRASE.toCharArray();

		assertEquals("a private key is not in the form the legacy wallet writes", assertInstanceOf(
			UnreadableWallet.class, assertThrows(IllegalArgumentException.class,
				() -> WalletKeys.keys(wallet, Set.of(MADE_UP_KEY), passphrase))
		).reason());

		assertEquals("a record is cut short", assertInstanceOf(
			UnreadableWallet.class, assertThrows(IllegalArgumentException.class,
				() -> WalletKeys.keys(wallet, Set.of(MADE_UP_ENCRYPTED_KEY), passphrase))
		).reason());
	}
}
