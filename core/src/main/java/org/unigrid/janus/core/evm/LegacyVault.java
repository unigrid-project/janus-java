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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.unigrid.janus.core.legacy.LegacyKey;

/**
 * Seals the private keys of an imported legacy wallet under the private key of the first account of the wallet's
 * recovery phrase. The key is stretched by HKDF so the signing key itself is never an encryption key, and the
 * addresses the keys belong to are bound in as associated data, so the ciphertext opens only for them.
 */
public class LegacyVault {
	/** What the keys are sealed as, laid out as the other sealed blocks of a wallet file are. */
	public record Locked(String cipher, String kdf, String nonce, String ciphertext) { }

	private static final String CIPHER = "aes-256-gcm";
	private static final String KDF = "hkdf-sha256";
	private static final byte[] INFO = "janus legacy keys v1".getBytes(StandardCharsets.US_ASCII);
	private static final int NONCE_SIZE = 12;
	private static final int TAG_BITS = 128;
	private static final int KEY_SIZE = 32;
	private static final int SECRET_SIZE = 32;
	private static final int ENTRY_SIZE = 1 + SECRET_SIZE;

	private final SecureRandom random;

	public LegacyVault() {
		this(new SecureRandom());
	}

	public LegacyVault(final SecureRandom random) {
		this.random = random;
	}

	/** The keys stay the caller's to wipe. */
	public Locked seal(final byte[] accountKey, final List<LegacyKey> keys, final List<String> addresses) {
		final byte[] nonce = new byte[NONCE_SIZE];
		final byte[] plain = pack(keys);

		random.nextBytes(nonce);

		try {
			final byte[] sealed = crypt(Cipher.ENCRYPT_MODE, accountKey, nonce, addresses, plain);

			return new Locked(CIPHER, KDF, hex(nonce), hex(sealed));
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("Every Java platform provides AES-GCM", e);
		} finally {
			Arrays.fill(plain, (byte) 0);
		}
	}

	/** The keys come back to the caller, who wipes them. */
	public List<LegacyKey> open(final byte[] accountKey, final Locked locked, final List<String> addresses) {
		if (!CIPHER.equals(locked.cipher()) || !KDF.equals(locked.kdf())) {
			throw new IllegalArgumentException("Janus cannot open keys sealed with " + locked.cipher());
		}

		byte[] plain = null;

		try {
			plain = crypt(Cipher.DECRYPT_MODE, accountKey, HexFormat.of().parseHex(locked.nonce()), addresses,
				HexFormat.of().parseHex(locked.ciphertext())
			);

			return unpack(plain);
		} catch (AEADBadTagException e) {
			throw new WrongPassword();
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("Every Java platform provides AES-GCM", e);
		} finally {
			if (plain != null) {
				Arrays.fill(plain, (byte) 0);
			}
		}
	}

	private static byte[] crypt(final int mode, final byte[] accountKey, final byte[] nonce,
		final List<String> addresses, final byte[] input) throws GeneralSecurityException {

		final byte[] key = stretch(accountKey);

		try {
			final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

			cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
			cipher.updateAAD(String.join("\n", addresses).getBytes(StandardCharsets.UTF_8));
			return cipher.doFinal(input);
		} finally {
			Arrays.fill(key, (byte) 0);
		}
	}

	private static byte[] stretch(final byte[] accountKey) {
		final HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
		final byte[] key = new byte[KEY_SIZE];

		hkdf.init(new HKDFParameters(accountKey, null, INFO));
		hkdf.generateBytes(key, 0, KEY_SIZE);
		return key;
	}

	private static byte[] pack(final List<LegacyKey> keys) {
		final ByteBuffer plain = ByteBuffer.allocate(Integer.BYTES + keys.size() * ENTRY_SIZE).putInt(keys.size());

		keys.forEach(key -> plain.put((byte) (key.compressed() ? 1 : 0)).put(key.secret()));
		return plain.array();
	}

	private static List<LegacyKey> unpack(final byte[] plain) {
		final ByteBuffer buffer = ByteBuffer.wrap(plain);
		final int count = buffer.getInt();

		if (plain.length != Integer.BYTES + count * ENTRY_SIZE) {
			throw new IllegalArgumentException("The sealed keys are not laid out as expected");
		}

		final List<LegacyKey> keys = new ArrayList<>();

		for (int i = 0; i < count; i++) {
			final boolean compressed = buffer.get() == 1;
			final byte[] secret = new byte[SECRET_SIZE];

			buffer.get(secret);
			keys.add(new LegacyKey(secret, compressed));
		}

		return keys;
	}

	private static String hex(final byte[] bytes) {
		return HexFormat.of().formatHex(bytes);
	}
}
