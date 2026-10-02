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

import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import org.unigrid.janus.core.legacy.LegacyKey;

/**
 * An EVM wallet as Janus keeps it: the addresses in the clear, so that it can be shown without a password,
 * and the recovery phrase sealed, so that nothing on disk can spend from it. A wallet made from an imported
 * legacy dump also carries the addresses of those keys in the clear and the keys sealed under the first
 * account of the phrase, so the phrase and its password open them too.
 */
public record EvmWallet(int version, String path, List<String> addresses, Sealed crypto, LegacyBlock legacy) {
	/** The legacy addresses in the clear and their private keys sealed. */
	public record LegacyBlock(List<String> addresses, LegacyVault.Locked keys) {
		public LegacyBlock {
			addresses = List.copyOf(addresses);
		}
	}

	public static final int VERSION = 1;
	public static final int LEGACY_VERSION = 2;
	public static final int ADDRESSES = 10;

	private static final int FIRST_ACCOUNT = 0;

	public EvmWallet {
		addresses = List.copyOf(addresses);
	}

	public static EvmWallet create(final Mnemonic mnemonic, final String password, final SeedVault vault) {
		final byte[] entropy = mnemonic.entropy();

		try {
			return new EvmWallet(VERSION, EvmAccounts.PATH, EvmAccounts.addresses(mnemonic, ADDRESSES),
				vault.seal(entropy, password), null
			);
		} finally {
			Arrays.fill(entropy, (byte) 0);
		}
	}

	/**
	 * The keys stay the caller's to wipe. Each address is kept once, whatever the keys repeat, and the keys are
	 * sealed in the order of the sorted addresses.
	 */
	public static EvmWallet create(final Mnemonic mnemonic, final String password, final SeedVault vault,
		final LegacyVault legacyVault, final List<LegacyKey> keys) {

		final EvmWallet plain = create(mnemonic, password, vault);
		final SortedMap<String, LegacyKey> byAddress = new TreeMap<>();
		final byte[] accountKey = EvmAccounts.privateKey(mnemonic, FIRST_ACCOUNT);

		keys.forEach(key -> byAddress.putIfAbsent(key.address(), key));

		try {
			final List<String> legacyAddresses = List.copyOf(byAddress.keySet());
			final LegacyVault.Locked locked = legacyVault.seal(accountKey, List.copyOf(byAddress.values()),
				legacyAddresses
			);

			return new EvmWallet(LEGACY_VERSION, plain.path(), plain.addresses(), plain.crypto(),
				new LegacyBlock(legacyAddresses, locked)
			);
		} finally {
			Arrays.fill(accountKey, (byte) 0);
		}
	}

	public Mnemonic mnemonic(final String password, final SeedVault vault) {
		return Mnemonic.of(vault.open(crypto, password));
	}

	/** The same wallet carrying the legacy block of an earlier one made from the same phrase. */
	public EvmWallet withLegacy(final LegacyBlock block) {
		return new EvmWallet(LEGACY_VERSION, path, addresses, crypto, block);
	}

	/** The legacy private keys, opened with the password. The caller wipes them. */
	public List<LegacyKey> legacyKeys(final String password, final SeedVault vault, final LegacyVault legacyVault) {
		return legacyKeys(mnemonic(password, vault), legacyVault);
	}

	/**
	 * The legacy private keys, opened with the phrase alone. They are sealed under a key the phrase gives, not
	 * under the password, so the password lost does not lose them. The caller wipes them.
	 */
	public List<LegacyKey> legacyKeys(final Mnemonic phrase, final LegacyVault legacyVault) {
		if (legacy == null) {
			throw new IllegalStateException("This wallet holds no legacy keys");
		}

		final byte[] accountKey = EvmAccounts.privateKey(phrase, FIRST_ACCOUNT);

		try {
			return legacyVault.open(accountKey, legacy.keys(), legacy.addresses());
		} finally {
			Arrays.fill(accountKey, (byte) 0);
		}
	}
}
