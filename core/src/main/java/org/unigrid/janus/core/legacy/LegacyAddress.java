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

import org.bitcoinj.base.Base58;
import org.bitcoinj.crypto.internal.CryptoUtils;

/** The address the legacy chain paid for a public key: Base58Check over the key's SHA-256 and RIPEMD-160 hash. */
public final class LegacyAddress {
	public static final int VERSION = 40;

	private LegacyAddress() {
	}

	public static String of(final byte[] publicKey) {
		return encode(CryptoUtils.sha256hash160(publicKey));
	}

	static String encode(final byte[] hash) {
		return Base58.encodeChecked(VERSION, hash);
	}
}
