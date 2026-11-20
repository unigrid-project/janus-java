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
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.bitcoinj.base.Base58;
import org.bitcoinj.crypto.ECKey;
import org.web3j.crypto.Sign;

/** A private key of the legacy chain. It is wiped as soon as it has been used, and never printed. */
public record LegacyKey(byte[] secret, boolean compressed) {
	private static final int SECRET_VERSION = 153;
	private static final int SECRET_SIZE = 32;
	private static final int UNCOMPRESSED_SIZE = 1 + SECRET_SIZE;
	private static final int COMPRESSED_SIZE = UNCOMPRESSED_SIZE + 1;

	private static final int SEQUENCE = 0x30;
	private static final int LONG_LENGTH = 0x80;
	private static final int MAXIMUM_LENGTH_BYTES = 2;
	private static final byte[] VERSION_ONE = {0x02, 0x01, 0x01};
	private static final int OCTET_STRING = 0x04;

	/** Reads the private key as the legacy daemon writes it: Base58Check, version 153, a flag for compressed. */
	public static LegacyKey parse(final String text) {
		final byte[] decoded = Base58.decodeChecked(text);

		try {
			final boolean compressed = decoded.length == COMPRESSED_SIZE && decoded[COMPRESSED_SIZE - 1] == 1;

			if (decoded.length != UNCOMPRESSED_SIZE && !compressed || decoded[0] != (byte) SECRET_VERSION) {
				throw new IllegalArgumentException("Not a private key of this network");
			}

			return of(Arrays.copyOfRange(decoded, 1, UNCOMPRESSED_SIZE), compressed);
		} finally {
			Arrays.fill(decoded, (byte) 0);
		}
	}

	/**
	 * Reads the DER a plain wallet.dat keeps a key in (SEC 1's ECPrivateKey) as strictly as the daemon's own reader
	 * (key.cpp): only what comes before the secret is checked, and a short secret is padded on the left.
	 */
	static LegacyKey fromDer(final byte[] der, final boolean compressed) {
		final ByteBuffer in = ByteBuffer.wrap(der);

		try {
			final int sequence = Byte.toUnsignedInt(in.get());
			final int form = Byte.toUnsignedInt(in.get());
			final int lengthBytes = form & ~LONG_LENGTH;

			if (sequence != SEQUENCE || (form & LONG_LENGTH) == 0
				|| lengthBytes < 1 || lengthBytes > MAXIMUM_LENGTH_BYTES) {

				throw notDer();
			}

			int length = 0;

			for (int i = 0; i < lengthBytes; i++) {
				length = length << Byte.SIZE | Byte.toUnsignedInt(in.get());
			}

			if (length > in.remaining()) {
				throw notDer();
			}

			final byte[] version = new byte[VERSION_ONE.length];

			in.get(version);

			final int type = Byte.toUnsignedInt(in.get());
			final int size = Byte.toUnsignedInt(in.get());

			if (!Arrays.equals(VERSION_ONE, version) || type != OCTET_STRING || size > SECRET_SIZE) {
				throw notDer();
			}

			final byte[] secret = new byte[SECRET_SIZE];

			in.get(secret, SECRET_SIZE - size, size);
			return of(secret, compressed);
		} catch (BufferUnderflowException _) {
			throw notDer();
		}
	}

	/** A key from its secret, refused and wiped unless it is one the curve allows. */
	static LegacyKey of(final byte[] secret, final boolean compressed) {
		if (secret.length != SECRET_SIZE || !isUsable(secret)) {
			Arrays.fill(secret, (byte) 0);
			throw new IllegalArgumentException("Not a private key the curve allows");
		}

		return new LegacyKey(secret, compressed);
	}

	/* bitcoinj refuses 0 and 1, which nobody holds coins under, and the curve has no key from its order up. */
	private static boolean isUsable(final byte[] secret) {
		final BigInteger value = new BigInteger(1, secret);
		return value.compareTo(BigInteger.ONE) > 0 && value.compareTo(Sign.CURVE_PARAMS.getN()) < 0;
	}

	/* Says nothing of the bytes, which hold the secret. */
	private static IllegalArgumentException notDer() {
		return new IllegalArgumentException("a private key is not in the form the legacy wallet writes");
	}

	/** The public key of the secret, in the encoding, compressed or not, that its address was made from. */
	public byte[] publicKey() {
		return ECKey.fromPrivate(secret, compressed).getPubKey();
	}

	public String address() {
		return LegacyAddress.of(publicKey());
	}

	public void wipe() {
		Arrays.fill(secret, (byte) 0);
	}

	@Override
	public String toString() {
		return "LegacyKey[hidden]";
	}
}
