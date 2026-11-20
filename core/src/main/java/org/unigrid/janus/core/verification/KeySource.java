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

package org.unigrid.janus.core.verification;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.unigrid.janus.core.evm.EvmWallet;
import org.unigrid.janus.core.evm.LegacyVault;
import org.unigrid.janus.core.evm.SeedVault;
import org.unigrid.janus.core.legacy.LegacyKey;
import org.unigrid.janus.core.legacy.WalletKeys;

/**
 * Where the private keys of the addresses to sign come from: the wallet.dat a holder opened, or the keys of a dump
 * they imported, which Janus keeps sealed in their EVM wallet. Each is opened with the secret the holder typed, which
 * is wiped once used.
 */
@FunctionalInterface
public interface KeySource {
	/** The keys of these addresses, and of no others; the caller wipes them. */
	List<LegacyKey> keys(Set<String> addresses);

	/** The keys in a wallet.dat, decrypted with its passphrase where it is encrypted. */
	static KeySource walletDat(final Path wallet, final char[] passphrase) {
		return addresses -> WalletKeys.keys(wallet, addresses, passphrase);
	}

	/**
	 * The keys of a dump imported into the EVM wallet, opened with the wallet's password. The vault takes the password
	 * as a String, which cannot be wiped, so only the array handed in is. All the keys come out of the vault together,
	 * and those of other addresses are wiped at once.
	 */
	static KeySource dumpImport(final EvmWallet wallet, final char[] password, final SeedVault seedVault,
		final LegacyVault legacyVault) {

		return addresses -> {
			final List<LegacyKey> all;

			try {
				all = wallet.legacyKeys(new String(password), seedVault, legacyVault);
			} finally {
				Arrays.fill(password, '\0');
			}

			final List<LegacyKey> kept = new ArrayList<>();

			for (final LegacyKey key : all) {
				if (addresses.contains(key.address())) {
					kept.add(key);
				} else {
					key.wipe();
				}
			}

			return kept;
		};
	}
}
