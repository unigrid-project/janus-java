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

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;
import org.bitcoinj.base.Sha256Hash;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.crypto.ECKey.ECDSASignature;
import org.web3j.crypto.Sign;

/**
 * Signs a text the way the legacy wallet's signmessage did, so that whatever reads the wallet's signatures reads
 * this one too. The wallet hashes its chain's magic and the text, each led by its length, twice with SHA-256, and
 * signs the hash with deterministic ECDSA (RFC 6979) and a low s. The 65 bytes start with a header, 27 plus the
 * recovery id plus 4 for a compressed key, then r and s.
 */
public final class LegacySigner {
	public static final int SIZE = 65;

	private static final byte[] MAGIC = "DarkNet Signed Message:\n".getBytes(StandardCharsets.US_ASCII);
	private static final int FIRST_HEADER = 27;
	private static final int COMPRESSED_OFFSET = 4;
	private static final int LAST_HEADER = FIRST_HEADER + 2 * COMPRESSED_OFFSET - 1;
	private static final int RECOVERY_IDS = 4;
	private static final int COORDINATE_SIZE = 32;
	private static final int ONE_BYTE_LENGTH_LIMIT = 252;

	private static final String NOT_ASCII = "A signed text is plain ASCII";
	private static final String TOO_LONG = "A signed text is at most %d bytes";
	private static final String NO_RECOVERY = "A signature recovers to no key of its signer";

	private LegacySigner() {
	}

	/** The signature of the text under the key, as the legacy wallet would print it before base64. */
	public static byte[] sign(final LegacyKey key, final String text) {
		final Sha256Hash hash = hashOf(text);
		final ECKey signer = ECKey.fromPrivate(key.secret(), key.compressed());

		/* bitcoinj signs with RFC 6979 and turns a high s into the low one, as the wallet does */
		final ECDSASignature signature = signer.sign(hash);
		final int recoveryId = recoveryIdOf(signer, signature, hash);
		final int header = FIRST_HEADER + recoveryId + (key.compressed() ? COMPRESSED_OFFSET : 0);

		return ByteBuffer.allocate(SIZE).put((byte) header).put(coordinate(signature.r))
			.put(coordinate(signature.s)).array();
	}

	/**
	 * The address whose key made the signature over the text, found from the signature alone, as the wallet prints no
	 * key with it. Empty for what the wallet could never have made: a size other than 65, a header out of range, r or
	 * s outside 1 to n - 1, a high s, no key for r, or a text it could not sign.
	 */
	public static Optional<String> addressOf(final String text, final byte[] signature) {
		if (signature.length != SIZE) {
			return Optional.empty();
		}

		final int header = Byte.toUnsignedInt(signature[0]);
		final BigInteger r = new BigInteger(1, Arrays.copyOfRange(signature, 1, 1 + COORDINATE_SIZE));
		final BigInteger s = new BigInteger(1, Arrays.copyOfRange(signature, 1 + COORDINATE_SIZE, SIZE));

		if (header < FIRST_HEADER || header > LAST_HEADER || isOutOfRanges(r, s) || isHigh(s)) {
			return Optional.empty();
		}

		try {
			final ECKey key = ECKey.recoverFromSignature((header - FIRST_HEADER) % RECOVERY_IDS,
				new ECDSASignature(r, s), hashOf(text), header >= FIRST_HEADER + COMPRESSED_OFFSET
			);

			return Optional.ofNullable(key).map(found -> LegacyAddress.of(found.getPubKey()));
		} catch (IllegalArgumentException _) {
			return Optional.empty();
		}
	}

	/*
	 * Only ASCII, as a character outside it would turn into a question mark and let two texts hash alike, and only a
	 * text whose length fits one byte, which is all the wallet's length prefix holds below 253.
	 */
	static Sha256Hash hashOf(final String text) {
		if (!StandardCharsets.US_ASCII.newEncoder().canEncode(text)) {
			throw new IllegalArgumentException(NOT_ASCII);
		}

		final byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);

		if (bytes.length > ONE_BYTE_LENGTH_LIMIT) {
			throw new IllegalArgumentException(TOO_LONG.formatted(ONE_BYTE_LENGTH_LIMIT));
		}

		final ByteArrayOutputStream data = new ByteArrayOutputStream();

		data.write(MAGIC.length);
		data.writeBytes(MAGIC);
		data.write(bytes.length);
		data.writeBytes(bytes);
		return Sha256Hash.twiceOf(data.toByteArray());
	}

	/* Of the four keys a signature can be recovered to, the one that is the signer's names the header. */
	private static int recoveryIdOf(final ECKey signer, final ECDSASignature signature, final Sha256Hash hash) {
		return IntStream.range(0, RECOVERY_IDS).filter(id -> {
			final ECKey recovered = ECKey.recoverFromSignature(id, signature, hash, signer.isCompressed());
			return recovered != null && Arrays.equals(recovered.getPubKey(), signer.getPubKey());
		}).findFirst().orElseThrow(() -> new IllegalStateException(NO_RECOVERY));
	}

	private static byte[] coordinate(final BigInteger value) {
		final byte[] bytes = value.toByteArray();
		final byte[] fixed = new byte[COORDINATE_SIZE];
		final int length = Math.min(bytes.length, COORDINATE_SIZE);

		System.arraycopy(bytes, bytes.length - length, fixed, COORDINATE_SIZE - length, length);
		return fixed;
	}

	/* Any value outside 1 to n - 1, which no signature of the curve holds. */
	private static boolean isOutOfRanges(final BigInteger... values) {
		return Arrays.stream(values)
			.anyMatch(value -> value.signum() <= 0 || value.compareTo(Sign.CURVE_PARAMS.getN()) >= 0);
	}

	private static boolean isHigh(final BigInteger s) {
		return s.compareTo(Sign.CURVE_PARAMS.getN().shiftRight(1)) > 0;
	}
}
