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

import java.security.DigestException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bitcoinj.base.Sha256Hash;

/**
 * The legacy wallet's encryption, as its crypter.cpp does it: AES-256-CBC throughout. The master key is encrypted
 * under a key and IV made from the passphrase the way OpenSSL's EVP_BytesToKey makes them with SHA-512, and each
 * private key under the master key, with an IV taken from its public key.
 */
final class LegacyCrypter {
	private static final int KEY_SIZE = 32;
	private static final int IV_SIZE = 16;
	private static final String AES = "AES";
	private static final String CIPHER = "AES/CBC/PKCS5Padding";
	private static final String SHA512 = "SHA-512";

	private LegacyCrypter() {
	}

	/**
	 * The master key the passphrase opens, or empty when it does not: with a wrong passphrase the padding fails, or
	 * leaves something other than a key, all but never anything else.
	 */
	static Optional<byte[]> openMasterKey(final MasterKey master, final byte[] passphrase) {
		final byte[] derived = derive(passphrase, master.salt(), master.rounds());

		try {
			return decrypt(new SecretKeySpec(derived, 0, KEY_SIZE, AES),
				new IvParameterSpec(derived, KEY_SIZE, IV_SIZE), master.crypted()
			);
		} finally {
			Arrays.fill(derived, (byte) 0);
		}
	}

	/** A private key's secret, decrypted under the master key with the start of its public key's double SHA-256. */
	static Optional<byte[]> openSecret(final byte[] masterKey, final byte[] publicKey, final byte[] crypted) {
		return decrypt(new SecretKeySpec(masterKey, AES),
			new IvParameterSpec(Sha256Hash.hashTwice(publicKey), 0, IV_SIZE), crypted
		);
	}

	/* EVP_BytesToKey's first block, all a 32-byte key and a 16-byte IV need: SHA-512 over the passphrase and the
	   salt, then over itself once for every further round. Hashed in place, so no round leaves a copy behind. */
	private static byte[] derive(final byte[] passphrase, final byte[] salt, final int rounds) {
		try {
			final MessageDigest sha512 = MessageDigest.getInstance(SHA512);
			final byte[] hash = new byte[sha512.getDigestLength()];

			sha512.update(passphrase);
			sha512.update(salt);
			sha512.digest(hash, 0, hash.length);

			for (int round = 1; round < rounds; round++) {
				sha512.update(hash);
				sha512.digest(hash, 0, hash.length);
			}

			return hash;
		} catch (NoSuchAlgorithmException | DigestException e) {
			throw new IllegalStateException("SHA-512 is missing from this Java", e);
		}
	}

	/* Empty when the bytes do not decrypt to a key, the sign of a wrong key: keys are all the legacy wallet encrypts. */
	private static Optional<byte[]> decrypt(final SecretKeySpec key, final IvParameterSpec iv, final byte[] crypted) {
		try {
			final Cipher cipher = Cipher.getInstance(CIPHER);

			cipher.init(Cipher.DECRYPT_MODE, key, iv);

			final byte[] plain = cipher.doFinal(crypted);

			if (plain.length == KEY_SIZE) {
				return Optional.of(plain);
			}

			Arrays.fill(plain, (byte) 0);
			return Optional.empty();
		} catch (BadPaddingException | IllegalBlockSizeException _) {
			return Optional.empty();
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("AES-256-CBC is missing from this Java", e);
		}
	}
}
