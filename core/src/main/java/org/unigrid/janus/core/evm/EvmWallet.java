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

/**
 * An EVM wallet as Janus keeps it: the addresses in the clear, so that it can be shown without a password,
 * and the recovery phrase sealed, so that nothing on disk can spend from it.
 */
public record EvmWallet(int version, String path, List<String> addresses, Sealed crypto) {
	public static final int VERSION = 1;
	public static final int ADDRESSES = 10;

	public EvmWallet {
		addresses = List.copyOf(addresses);
	}

	public static EvmWallet create(final Mnemonic mnemonic, final String password, final SeedVault vault) {
		final byte[] entropy = mnemonic.entropy();

		try {
			return new EvmWallet(VERSION, EvmAccounts.PATH, EvmAccounts.addresses(mnemonic, ADDRESSES),
				vault.seal(entropy, password)
			);
		} finally {
			Arrays.fill(entropy, (byte) 0);
		}
	}

	public Mnemonic mnemonic(final String password, final SeedVault vault) {
		return Mnemonic.of(vault.open(crypto, password));
	}
}
