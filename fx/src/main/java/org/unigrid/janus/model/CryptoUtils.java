/*
	The Janus Wallet
	Copyright © 2021-2023 The Unigrid Foundation, UGD Software AB

	This program is free software: you can redistribute it and/or modify it under the terms of the
	addended GNU Affero General Public License as published by the Free Software Foundation, version 3
	of the License (see COPYING and COPYING.addendum).

	This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
	even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU Affero General Public License for more details.

	You should have received an addended copy of the GNU Affero General Public License with this program.
	If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.model;

import com.jeongen.cosmos.util.AddressUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import lombok.Data;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.SignatureDecodeException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.unigrid.janus.model.service.DebugService;

@Data
@ApplicationScoped
public class CryptoUtils {

	@Inject
	private DebugService debug;

	@Inject
	private AccountManager accountManager;

	@Inject
	private AccountModel accountModel;

	private static final int SALT_SIZE = 16;
	private static final int ITERATION_COUNT = 65536;
	private static final int KEY_LENGTH = 256;

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	public String encrypt(String dataToEncrypt, String password) throws Exception {
		if (accountModel == null) {
			throw new IllegalStateException("accountModel is not injected! CryptoUtils");
		}

		SecureRandom sr = new SecureRandom();
		byte[] salt = new byte[SALT_SIZE];
		sr.nextBytes(salt);

		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
		SecretKey secretKey = factory.generateSecret(spec);
		SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		GCMParameterSpec specGCM = new GCMParameterSpec(128, salt); // using salt as IV
		cipher.init(Cipher.ENCRYPT_MODE, secret, specGCM);

		byte[] encryptedData = cipher.doFinal(dataToEncrypt.getBytes());

		// Combining salt and encrypted data
		byte[] combinedData = new byte[salt.length + encryptedData.length];
		System.arraycopy(salt, 0, combinedData, 0, SALT_SIZE);
		System.arraycopy(encryptedData, 0, combinedData, SALT_SIZE, encryptedData.length);
		System.out.println("encrypt: " + Base64.getEncoder().encodeToString(combinedData));

		DataDirectory.ensureDirectoryExists(DataDirectory.KEYRING_DIRECTORY);
		String encryptedBase64 = Base64.getEncoder().encodeToString(combinedData);
		accountModel.setEncryptedMnemonic(encryptedBase64);
		accountManager.createAccountFile(accountModel);
		System.out.println("accountModel: " + accountModel);
		return encryptedBase64;
	}

	public String decrypt(String dataToDecrypt, String password) throws Exception {
		byte[] decodedData = Base64.getDecoder().decode(dataToDecrypt);

		byte[] salt = Arrays.copyOfRange(decodedData, 0, SALT_SIZE);
		byte[] encryptedData = Arrays.copyOfRange(decodedData, SALT_SIZE, decodedData.length);

		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
		SecretKey secretKey = factory.generateSecret(spec);
		SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		GCMParameterSpec specGCM = new GCMParameterSpec(128, salt); // using salt as IV
		cipher.init(Cipher.DECRYPT_MODE, secret, specGCM);

		byte[] decryptedData = cipher.doFinal(encryptedData);
		System.out.println("decrypt: " + new String(decryptedData));

		return new String(decryptedData);
	}

	public String getAddressFromPrivateKey(String privateKey)
		throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		byte[] privateKeyBytes = hexStringToByteArray(privateKey);
		ECKey ecKey = ECKey.fromPrivate(privateKeyBytes);
		byte[] publicKeyBytes = ecKey.getPubKey();
		System.out.println("publicKey: " + bytesToHex(publicKeyBytes));

		String address = AddressUtil.publicKeyToAddress(publicKeyBytes, "unigrid");

		System.out.println("getAddressFromPrivateKey: " + address);
		return address;
	}

	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
				+ Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	private static byte[] convertBits(byte[] data, int fromBits, int toBits, boolean pad) {
		int acc = 0;
		int bits = 0;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int maxv = (1 << toBits) - 1;
		for (int i = 0; i < data.length; i++) {
			int value = data[i] & 0xff;
			if ((value >>> fromBits) != 0) {
				throw new RuntimeException(
					"ERR_BAD_FORMAT invalid data range: data[" + i + "]=" + value
					+ " (fromBits=" + fromBits + ")");
			}
			acc = (acc << fromBits) | value;
			bits += fromBits;
			while (bits >= toBits) {
				bits -= toBits;
				baos.write((acc >>> bits) & maxv);
			}
		}
		if (pad) {
			if (bits > 0) {
				baos.write((acc << (toBits - bits)) & maxv);
			}
		} else if (bits >= fromBits) {
			throw new RuntimeException("ERR_BAD_FORMAT illegal zero padding");
		} else if (((acc << (toBits - bits)) & maxv) != 0) {
			throw new RuntimeException("ERR_BAD_FORMAT non-zero padding");
		}
		return baos.toByteArray();
	}

	public String bytesToHex(byte[] bytes) {
		if (bytes == null) {
			return null; // or throw an appropriate exception
		}
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	/**
	 * Derives a set of keys based on a private key and an index range.
	 *
	 * @param privateKey the private key in hexadecimal format.
	 * @param indexes the number of indexes to use for key derivation.
	 * @return an array of derived key pairs.
	 * @throws Exception if there is an error during key derivation.
	 */
	public ECKey[] deriveKeys(String privateKey, int indexes) throws Exception {
		ECKey[] derivedKeys = new ECKey[indexes];
		MessageDigest digest = MessageDigest.getInstance("SHA-256");

		for (int i = 0; i < indexes; i++) {
			// Concatenate the private key with the index
			String input = privateKey + i;

			// Hash the concatenated value
			byte[] hash = digest.digest(input.getBytes());

			// Create a new private key from the hash
			ECKey derivedKey = ECKey.fromPrivate(hash);

			// Store the derived key in the array
			derivedKeys[i] = derivedKey;
		}

		return derivedKeys;
	}

	public void printKeys(ECKey[] keys) {
		for (int i = 0; i < keys.length; i++) {
			ECKey key = keys[i];
			// Get the private key in hexadecimal format
			String privateKeyHex = key.getPrivateKeyAsHex();
			// Get the public key in hexadecimal format
			String publicKeyHex = key.getPublicKeyAsHex();

			System.out.println("Key " + i + ":");
			System.out.println("Private Key: " + privateKeyHex);
			System.out.println("Public Key: " + publicKeyHex);
		}
	}

	public boolean verifySignature(byte[] message, byte[] signatureBytes, ECKey[] publicKeys)
		throws SignatureDecodeException {

		// Decode the signature bytes
		ECDSASignature signature = ECDSASignature.decodeFromDER(signatureBytes);

		// Hash the message (assuming SHA-256 is used)
		Sha256Hash messageHash = Sha256Hash.of(message);

		// Check each public key to see if it verifies the signature
		for (ECKey publicKey : publicKeys) {
			System.out.println("publicKey: " + publicKey);
			if (publicKey.verify(messageHash, signature)) {
				// Signature is valid for this public key
				return true;
			}
		}

		// No keys verified the signature
		return false;
	}

}
