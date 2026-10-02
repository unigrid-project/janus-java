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

import java.math.BigInteger;
import java.util.Arrays;
import org.bitcoinj.base.Base58;
import org.bitcoinj.crypto.ECKey;
import org.web3j.crypto.Sign;

/** A private key of the legacy chain. It is wiped as soon as it has been used, and never printed. */
public record LegacyKey(byte[] secret, boolean compressed) {
	private static final int SECRET_VERSION = 153;
	private static final int UNCOMPRESSED_SIZE = 1 + 32;
	private static final int COMPRESSED_SIZE = UNCOMPRESSED_SIZE + 1;

	/** Reads the private key as the legacy daemon writes it: Base58Check, version 153, a flag for compressed. */
	public static LegacyKey parse(final String text) {
		final byte[] decoded = Base58.decodeChecked(text);

		try {
			final boolean compressed = decoded.length == COMPRESSED_SIZE && decoded[COMPRESSED_SIZE - 1] == 1;

			if (decoded.length != UNCOMPRESSED_SIZE && !compressed || decoded[0] != (byte) SECRET_VERSION) {
				throw new IllegalArgumentException("Not a private key of this network");
			}

			final byte[] secret = Arrays.copyOfRange(decoded, 1, UNCOMPRESSED_SIZE);

			if (!isUsable(secret)) {
				Arrays.fill(secret, (byte) 0);
				throw new IllegalArgumentException("Not a private key the curve allows");
			}

			return new LegacyKey(secret, compressed);
		} finally {
			Arrays.fill(decoded, (byte) 0);
		}
	}

	/* bitcoinj refuses 0 and 1, which nobody holds coins under, and the curve has no key from its order up. */
	private static boolean isUsable(final byte[] secret) {
		final BigInteger value = new BigInteger(1, secret);
		return value.compareTo(BigInteger.ONE) > 0 && value.compareTo(Sign.CURVE_PARAMS.getN()) < 0;
	}

	public String address() {
		return LegacyAddress.of(ECKey.fromPrivate(secret, compressed).getPubKey());
	}

	public void wipe() {
		Arrays.fill(secret, (byte) 0);
	}

	@Override
	public String toString() {
		return "LegacyKey[hidden]";
	}
}
