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
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.generators.SCrypt;
import org.unigrid.janus.core.evm.Sealed.CipherParams;
import org.unigrid.janus.core.evm.Sealed.KdfParams;
import org.web3j.crypto.Hash;

/**
 * Seals a secret behind a password as a version 3 keystore does: scrypt stretches the password, the first
 * half of the result keys AES-128-CTR and the second half proves, through a Keccak-256 MAC, that the
 * password was right before anything is decrypted.
 */
public class SeedVault {
	/* The cost geth and Besu use for their standard keystores: about a second and 256 MB per attempt. */
	public static final int STANDARD_COST = 1 << 18;

	private static final String CIPHER = "aes-128-ctr";
	private static final String KDF = "scrypt";
	private static final int BLOCK_SIZE = 8;
	private static final int PARALLELISM = 1;
	private static final int KEY_LENGTH = 32;
	private static final int HALF = KEY_LENGTH / 2;
	private static final HexFormat HEX = HexFormat.of();

	private final SecureRandom random;
	private final int cost;

	public SeedVault() {
		this(new SecureRandom(), STANDARD_COST);
	}

	public SeedVault(final SecureRandom random, final int cost) {
		this.random = random;
		this.cost = cost;
	}

	public Sealed seal(final byte[] secret, final String password) {
		final byte[] salt = randomBytes(KEY_LENGTH);
		final byte[] iv = randomBytes(HALF);
		final KdfParams kdf = new KdfParams(cost, BLOCK_SIZE, PARALLELISM, KEY_LENGTH, HEX.formatHex(salt));
		final byte[] derived = derive(password, kdf);

		try {
			final byte[] ciphertext = crypt(derived, iv, secret);

			return new Sealed(CIPHER, new CipherParams(HEX.formatHex(iv)), HEX.formatHex(ciphertext), KDF, kdf,
				HEX.formatHex(mac(derived, ciphertext))
			);
		} finally {
			Arrays.fill(derived, (byte) 0);
		}
	}

	public byte[] open(final Sealed sealed, final String password) {
		if (!CIPHER.equals(sealed.cipher()) || !KDF.equals(sealed.kdf())) {
			throw new IllegalArgumentException("Only " + CIPHER + " under " + KDF + " can be opened, not "
				+ sealed.cipher() + " under " + sealed.kdf()
			);
		}

		final byte[] derived = derive(password, sealed.kdfparams());

		try {
			final byte[] ciphertext = HEX.parseHex(sealed.ciphertext());

			if (!MessageDigest.isEqual(mac(derived, ciphertext), HEX.parseHex(sealed.mac()))) {
				throw new WrongPassword();
			}

			return crypt(derived, HEX.parseHex(sealed.cipherparams().iv()), ciphertext);
		} finally {
			Arrays.fill(derived, (byte) 0);
		}
	}

	private byte[] randomBytes(final int size) {
		final byte[] bytes = new byte[size];

		random.nextBytes(bytes);
		return bytes;
	}

	private static byte[] derive(final String password, final KdfParams kdf) {
		return SCrypt.generate(password.getBytes(StandardCharsets.UTF_8), HEX.parseHex(kdf.salt()),
			kdf.n(), kdf.r(), kdf.p(), kdf.dklen()
		);
	}

	/* Counter mode encrypts and decrypts alike. */
	private static byte[] crypt(final byte[] derived, final byte[] iv, final byte[] input) {
		try {
			final Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
			final SecretKeySpec key = new SecretKeySpec(derived, 0, HALF, "AES");

			cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
			return cipher.doFinal(input);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("AES-128-CTR is not available", e);
		}
	}

	private static byte[] mac(final byte[] derived, final byte[] ciphertext) {
		return Hash.sha3(ByteBuffer.allocate(HALF + ciphertext.length).put(derived, HALF, HALF).put(ciphertext)
			.array()
		);
	}
}
