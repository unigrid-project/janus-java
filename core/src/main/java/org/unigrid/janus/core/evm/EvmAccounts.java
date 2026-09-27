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

import java.util.List;
import java.util.stream.IntStream;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.MnemonicUtils;

/**
 * The addresses a recovery phrase holds, derived along the BIP44 path Ethereum wallets share, so that
 * Besu, MetaMask and Janus all find the same accounts in the same order.
 */
public final class EvmAccounts {
	public static final String PATH = "m/44'/60'/0'/0";

	private static final int[] ACCOUNT = {
		44 | Bip32ECKeyPair.HARDENED_BIT, 60 | Bip32ECKeyPair.HARDENED_BIT, Bip32ECKeyPair.HARDENED_BIT, 0
	};

	private EvmAccounts() {
	}

	/** The first addresses of the phrase, in EIP-55 checksummed form. */
	public static List<String> addresses(final Mnemonic mnemonic, final int count) {
		final byte[] seed = MnemonicUtils.generateSeed(mnemonic.phrase(), "");
		final Bip32ECKeyPair account = Bip32ECKeyPair.deriveKeyPair(Bip32ECKeyPair.generateKeyPair(seed), ACCOUNT);

		return IntStream.range(0, count).mapToObj(index -> Bip32ECKeyPair.deriveKeyPair(account, new int[] {index}))
			.map(key -> Keys.toChecksumAddress(Keys.getAddress(key))).toList();
	}
}
