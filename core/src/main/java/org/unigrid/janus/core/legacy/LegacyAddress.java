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
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;

/** The address the legacy chain paid for a public key: Base58Check over the key's SHA-256 and RIPEMD-160 hash. */
public final class LegacyAddress {
	public static final int VERSION = 40;

	private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
	private static final BigInteger BASE = BigInteger.valueOf(ALPHABET.length());
	private static final int CHECKSUM_SIZE = 4;

	private LegacyAddress() {
	}

	public static String of(final byte[] publicKey) {
		return encode(hash160(publicKey));
	}

	/* The version byte is never zero, so there are no leading zero bytes to spell out as '1'. */
	static String encode(final byte[] hash) {
		final byte[] payload = ByteBuffer.allocate(1 + hash.length).put((byte) VERSION).put(hash).array();
		final byte[] checked = ByteBuffer.allocate(payload.length + CHECKSUM_SIZE).put(payload)
			.put(sha256(sha256(payload)), 0, CHECKSUM_SIZE).array();
		final StringBuilder address = new StringBuilder();

		for (BigInteger rest = new BigInteger(1, checked); rest.signum() > 0; rest = rest.divide(BASE)) {
			address.append(ALPHABET.charAt(rest.mod(BASE).intValue()));
		}

		return address.reverse().toString();
	}

	private static byte[] hash160(final byte[] publicKey) {
		final byte[] sha = sha256(publicKey);
		final RIPEMD160Digest ripemd = new RIPEMD160Digest();
		final byte[] hash = new byte[ripemd.getDigestSize()];

		ripemd.update(sha, 0, sha.length);
		ripemd.doFinal(hash, 0);
		return hash;
	}

	private static byte[] sha256(final byte[] data) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(data);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Every Java platform provides SHA-256", e);
		}
	}
}
