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

import java.util.HexFormat;
import java.util.List;
import net.jqwik.api.Example;
import org.unigrid.janus.core.legacy.LegacyKey;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LegacyVaultTest {
	private static final List<String> ADDRESSES = List.of("H78V5Mwegfjmemi2rMuVg93c8AwjirUdQH",
		"HS6ofefYfBjXjqaKM4pb54a1SEmAxvGKTi"
	);

	private final LegacyVault vault = new LegacyVault();

	private static byte[] accountKey() {
		return EvmAccounts.privateKey(Mnemonic.parse(MnemonicTest.ABANDON), 0);
	}

	private static List<LegacyKey> keys() {
		return List.of(
			LegacyKey.parse("PdiUUh8dnXB36B2XcbPdodUX5Ujoj2VDubJwcW1V8DJu6eQQxzNx"),
			LegacyKey.parse("68QtzUftP6UedWuuhgxsw4jV7TDsTjvyPnqUKvFA6G2LWUipq9J")
		);
	}

	private static LegacyVault.Locked altered(final LegacyVault.Locked locked, final String ciphertext) {
		return new LegacyVault.Locked(locked.cipher(), locked.kdf(), locked.nonce(), ciphertext);
	}

	@Example
	public void shouldOpenWhatItSealed() {
		final LegacyVault.Locked locked = vault.seal(accountKey(), keys(), ADDRESSES);
		final List<LegacyKey> opened = vault.open(accountKey(), locked, ADDRESSES);

		assertEquals(2, opened.size());
		assertArrayEquals(keys().get(0).secret(), opened.get(0).secret());
		assertArrayEquals(keys().get(1).secret(), opened.get(1).secret());
		assertEquals(List.of(true, false), opened.stream().map(LegacyKey::compressed).toList());
		assertEquals(ADDRESSES, opened.stream().map(LegacyKey::address).toList());
	}

	@Example
	public void shouldSealNoKeysAndOpenNoKeys() {
		final LegacyVault.Locked locked = vault.seal(accountKey(), List.of(), List.of());

		assertTrue(vault.open(accountKey(), locked, List.of()).isEmpty());
	}

	@Example
	public void shouldNameWhatItEncryptsWith() {
		final LegacyVault.Locked locked = vault.seal(accountKey(), keys(), ADDRESSES);

		assertEquals("aes-256-gcm", locked.cipher());
		assertEquals("hkdf-sha256", locked.kdf());
		assertEquals(12, HexFormat.of().parseHex(locked.nonce()).length);
	}

	@Example
	public void shouldKeepTheSecretsOutOfTheCiphertext() {
		final LegacyVault.Locked locked = vault.seal(accountKey(), keys(), ADDRESSES);
		final String secret = HexFormat.of().formatHex(keys().get(0).secret());

		assertFalse(locked.ciphertext().contains(secret));
	}

	@Example
	public void shouldUseAFreshNonceEachTime() {
		final LegacyVault.Locked first = vault.seal(accountKey(), keys(), ADDRESSES);
		final LegacyVault.Locked second = vault.seal(accountKey(), keys(), ADDRESSES);

		assertNotEquals(first.nonce(), second.nonce());
		assertNotEquals(first.ciphertext(), second.ciphertext());
	}

	@Example
	public void shouldRefuseAKeyThatIsNotTheOneItWasSealedWith() {
		final LegacyVault.Locked locked = vault.seal(accountKey(), keys(), ADDRESSES);
		final byte[] other = EvmAccounts.privateKey(Mnemonic.parse(MnemonicTest.ABANDON), 1);

		assertThrows(WrongPassword.class, () -> vault.open(other, locked, ADDRESSES));
	}

	@Example
	public void shouldRefuseCiphertextThatWasAltered() {
		final LegacyVault.Locked locked = vault.seal(accountKey(), keys(), ADDRESSES);
		final byte[] bytes = HexFormat.of().parseHex(locked.ciphertext());

		bytes[0] ^= 1;
		assertThrows(WrongPassword.class,
			() -> vault.open(accountKey(), altered(locked, HexFormat.of().formatHex(bytes)), ADDRESSES)
		);
	}

	@Example
	public void shouldRefuseAddressesThatAreNotTheOnesTheKeysWereSealedFor() {
		final LegacyVault.Locked locked = vault.seal(accountKey(), keys(), ADDRESSES);

		assertThrows(WrongPassword.class, () -> vault.open(accountKey(), locked, List.of(ADDRESSES.get(0))));
		assertThrows(WrongPassword.class,
			() -> vault.open(accountKey(), locked, List.of(ADDRESSES.get(1), ADDRESSES.get(0)))
		);
	}
}
