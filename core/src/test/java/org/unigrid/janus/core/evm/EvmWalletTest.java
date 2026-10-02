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

import java.security.SecureRandom;
import java.util.List;
import net.jqwik.api.Example;
import org.unigrid.janus.core.legacy.LegacyKey;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EvmWalletTest {
	static final SeedVault VAULT = new SeedVault(new SecureRandom(), SeedVaultTest.CHEAP);
	static final Mnemonic PHRASE = Mnemonic.parse(MnemonicTest.ABANDON);

	@Example
	public void shouldHoldTheFirstAddressesOfItsPhrase() {
		final EvmWallet wallet = EvmWallet.create(PHRASE, "pw", VAULT);

		assertEquals(EvmAccounts.addresses(PHRASE, EvmWallet.ADDRESSES), wallet.addresses());
		assertEquals("m/44'/60'/0'/0", wallet.path());
	}

	@Example
	public void shouldGiveBackItsPhraseForThePassword() {
		assertEquals(PHRASE, EvmWallet.create(PHRASE, "pw", VAULT).mnemonic("pw", VAULT));
	}

	@Example
	public void shouldKeepItsPhraseFromAnotherPassword() {
		final EvmWallet wallet = EvmWallet.create(PHRASE, "pw", VAULT);

		assertThrows(WrongPassword.class, () -> wallet.mnemonic("other", VAULT));
	}

	private static List<LegacyKey> legacyKeys() {
		return List.of(
			LegacyKey.parse("PdiUUh8dnXB36B2XcbPdodUX5Ujoj2VDubJwcW1V8DJu6eQQxzNx"),
			LegacyKey.parse("68QtzUftP6UedWuuhgxsw4jV7TDsTjvyPnqUKvFA6G2LWUipq9J")
		);
	}

	private static EvmWallet withLegacyKeys() {
		return EvmWallet.create(PHRASE, "pw", VAULT, new LegacyVault(), legacyKeys());
	}

	@Example
	public void shouldHoldPlainWalletsWithNoLegacyKeys() {
		final EvmWallet wallet = EvmWallet.create(PHRASE, "pw", VAULT);

		assertEquals(EvmWallet.VERSION, wallet.version());
		assertNull(wallet.legacy());
		assertThrows(IllegalStateException.class, () -> wallet.legacyKeys("pw", VAULT, new LegacyVault()));
	}

	@Example
	public void shouldListTheLegacyAddressesInTheClearAndInOrder() {
		final EvmWallet wallet = withLegacyKeys();

		assertEquals(EvmWallet.LEGACY_VERSION, wallet.version());
		assertEquals(List.of("H78V5Mwegfjmemi2rMuVg93c8AwjirUdQH", "HS6ofefYfBjXjqaKM4pb54a1SEmAxvGKTi"),
			wallet.legacy().addresses()
		);
		assertEquals(EvmAccounts.addresses(PHRASE, EvmWallet.ADDRESSES), wallet.addresses());
	}

	@Example
	public void shouldGiveBackTheLegacyKeysForThePassword() {
		final List<LegacyKey> opened = withLegacyKeys().legacyKeys("pw", VAULT, new LegacyVault());

		assertEquals(2, opened.size());
		assertArrayEquals(legacyKeys().get(0).secret(), opened.get(0).secret());
		assertArrayEquals(legacyKeys().get(1).secret(), opened.get(1).secret());
		assertEquals(List.of(true, false), opened.stream().map(LegacyKey::compressed).toList());
	}

	@Example
	public void shouldKeepTheLegacyKeysFromAnotherPassword() {
		final EvmWallet wallet = withLegacyKeys();

		assertThrows(WrongPassword.class, () -> wallet.legacyKeys("other", VAULT, new LegacyVault()));
	}

	@Example
	public void shouldListEachLegacyAddressOnceEvenWhenAKeyIsInTheDumpTwice() {
		final List<LegacyKey> twice = List.of(legacyKeys().get(0), legacyKeys().get(0));
		final EvmWallet wallet = EvmWallet.create(PHRASE, "pw", VAULT, new LegacyVault(), twice);

		assertEquals(List.of("H78V5Mwegfjmemi2rMuVg93c8AwjirUdQH"), wallet.legacy().addresses());
		assertEquals(1, wallet.legacyKeys("pw", VAULT, new LegacyVault()).size());
	}
}
