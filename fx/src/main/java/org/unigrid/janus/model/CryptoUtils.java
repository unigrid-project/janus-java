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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
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
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
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

	public String encrypt(byte[] dataToEncrypt, String password) throws Exception {
		if (accountModel == null) {
			throw new IllegalStateException("accountModel is not injected! CryptoUtils");
		}

		SecureRandom sr = new SecureRandom();
		byte[] salt = new byte[SALT_SIZE];
		sr.nextBytes(salt);

		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
		PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT,
				KEY_LENGTH);
		SecretKey secretKey = factory.generateSecret(spec);
		SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		GCMParameterSpec specGCM = new GCMParameterSpec(128, salt); // using salt as IV
		cipher.init(Cipher.ENCRYPT_MODE, secret, specGCM);

		byte[] encryptedData = cipher.doFinal(dataToEncrypt);

		// Combining salt and encrypted data
		byte[] combinedData = new byte[salt.length + encryptedData.length];
		System.arraycopy(salt, 0, combinedData, 0, SALT_SIZE);
		System.arraycopy(encryptedData, 0, combinedData, SALT_SIZE, encryptedData.length);
		System.out
				.println("encrypt: " + Base64.getEncoder().encodeToString(combinedData));

		DataDirectory.ensureDirectoryExists(DataDirectory.KEYRING_DIRECTORY);
		String encryptedBase64 = Base64.getEncoder().encodeToString(combinedData);
		accountModel.setEncryptedPrivateKey(encryptedBase64);
		accountManager.createAccountFile(accountModel);
		System.out.println("accountModel: " + accountModel);
		return encryptedBase64;
	}

	public byte[] decrypt(String dataToDecrypt, String password) throws Exception {
		byte[] decodedData = Base64.getDecoder().decode(dataToDecrypt);

		byte[] salt = Arrays.copyOfRange(decodedData, 0, SALT_SIZE);
		byte[] encryptedData = Arrays.copyOfRange(decodedData, SALT_SIZE,
				decodedData.length);

		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
		PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT,
				KEY_LENGTH);
		SecretKey secretKey = factory.generateSecret(spec);
		SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		GCMParameterSpec specGCM = new GCMParameterSpec(128, salt); // using salt as IV
		cipher.init(Cipher.DECRYPT_MODE, secret, specGCM);

		byte[] decryptedData = cipher.doFinal(encryptedData);
		System.out.println("decrypt: " + new String(decryptedData));

		return decryptedData;
	}

	public String getAddressFromPrivateKey(String privateKey)
			throws NoSuchAlgorithmException, NoSuchProviderException,
			InvalidKeySpecException {
		byte[] publicKeyBytes = getPublicKeyBytes(privateKey);
		System.out.println("publicKey: " + bytesToHex(publicKeyBytes));

		String address = AddressUtil.publicKeyToAddress(publicKeyBytes, "unigrid");

		System.out.println("getAddressFromPrivateKey: " + address);
		return address;
	}

	public byte[] getPublicKeyFromPrivateKey(String privateKey)
			throws NoSuchAlgorithmException, NoSuchProviderException,
			InvalidKeySpecException {

		byte[] publicKeyBytes = getPublicKeyBytes(privateKey);
		System.out.println("getPublicKeyFromPrivateKey: " + bytesToHex(publicKeyBytes));

		return publicKeyBytes;
	}

	public byte[] getPublicKeyBytes(String privateKey) {
		byte[] privateKeyBytes = hexStringToByteArray(privateKey);
		ECKey ecKey = ECKey.fromPrivate(privateKeyBytes);
		byte[] publicKeyBytes = ecKey.getPubKey();
		return publicKeyBytes;
	}

	public byte[] getPrivateKeyBytes(String privateKey) {
		byte[] privateKeyBytes = hexStringToByteArray(privateKey);
		return privateKeyBytes;

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

	private static byte[] convertBits(byte[] data, int fromBits, int toBits,
			boolean pad) {
		int acc = 0;
		int bits = 0;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int maxv = (1 << toBits) - 1;
		for (int i = 0; i < data.length; i++) {
			int value = data[i] & 0xff;
			if ((value >>> fromBits) != 0) {
				throw new RuntimeException("ERR_BAD_FORMAT invalid data range: data[" + i
						+ "]=" + value + " (fromBits=" + fromBits + ")");
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
	 * @param indexes    the number of indexes to use for key derivation.
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

	public boolean verifySignature(byte[] message, byte[] signatureBytes,
			ECKey[] publicKeys) throws SignatureDecodeException {

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

	public boolean verifySignatureKeys(byte[] message, byte[] signatureBytes,
			ECKey[] publicKeys, int keysToCreate, String pubKey)
			throws SignatureDecodeException {
		System.out.println(Arrays.toString(signatureBytes));

		// Decode the signature bytes
		ECDSASignature signature = ECDSASignature.decodeFromDER(signatureBytes);
		System.out.println("signature: " + signature);

		// Hash the message (assuming SHA-256 is used)
		Sha256Hash messageHash = Sha256Hash.of(message);
		System.out.println("messageHash: " + messageHash);
		String recoveredPublicKeyHex = null;
		ECKey publicKey = null;
		for (int recId = 0; recId < 4; recId++) { // Loop through the possible recId
													// values
			publicKey = ECKey.recoverFromSignature(recId, signature, messageHash, false);
			if (publicKey != null) {
				byte[] compressedPublicKeyBytes = publicKey.getPubKeyPoint()
						.getEncoded(true);
				recoveredPublicKeyHex = bytesToHex(compressedPublicKeyBytes);
				if (recoveredPublicKeyHex.equals(pubKey)) {
					break; // Exit the loop if the recovered public key matches the
							// provided pubKey
				}
				publicKey = null; // Reset publicKey to null if no match is found
			}
		}

		if (publicKey == null) {
			System.out.println("Failed to recover matching public key");
			return false; // Return false if no matching public key is found
		}

		// Assuming uncompressed public key. Adjust as necessary.
		// if (publicKey == null) {
		// return false; // Failed to recover public key
		// }
		// Generate a list of keys from the public key
		List<ECKey> generatedKeys = generateKeysFromCompressedPublicKey(
				recoveredPublicKeyHex, keysToCreate);

		// Compare the generated keys with the keys passed in
		if (generatedKeys.size() != publicKeys.length) {
			System.out.println("Lenghts dont match of keys");

			return false; // The number of keys do not match
		}

		for (int i = 0; i < generatedKeys.size(); i++) {
//			System.out.println(
//					"publicKeys[i].getPubKey()" + bytesToHex(publicKeys[i].getPubKey()));
//			System.out.println("generatedKeys[i].getPubKey()"
//					+ bytesToHex(generatedKeys.get(i).getPubKey()));
			if (!Arrays.equals(generatedKeys.get(i).getPubKey(),
					publicKeys[i].getPubKey())) {

				return false; // The keys do not match
			}
		}
		System.out.println("All keys match this account!");

		return true;
	}

	public String getAddressFromKey(ECKey ecKey) {
		byte[] publicKeyBytes = ecKey.getPubKey();
		System.out.println("publicKey: " + bytesToHex(publicKeyBytes));

		String address = AddressUtil.publicKeyToAddress(publicKeyBytes, "unigrid");

		System.out.println("getAddressFromKey: " + address);
		return address;
	}

	public List<ECKey> generateKeysFromCompressedPublicKey(String compressedPublicKeyHex,
			int keysToCreate) {
		System.out.println("Key being used in test: " + compressedPublicKeyHex);

		// Hash the compressed public key bytes to generate the seed
		byte[] seed = Sha256Hash.hash(compressedPublicKeyHex.getBytes());

		// Current time in milliseconds since epoch
		long creationTimeSeconds = System.currentTimeMillis() / 1000L;

		// Generate the HD wallet from the seed
		DeterministicSeed deterministicSeed = new DeterministicSeed(seed, "",
				creationTimeSeconds);
		DeterministicKeyChain chain = DeterministicKeyChain.builder()
				.seed(deterministicSeed).build();

		// Derive child keys
		List<ECKey> derivedKeysList = new ArrayList<>();
		DeterministicKey parentKey = chain.getWatchingKey();
		for (int i = 0; i < keysToCreate; i++) {
			DeterministicKey childKey = HDKeyDerivation.deriveChildKey(parentKey,
					new ChildNumber(i));
			derivedKeysList.add(ECKey.fromPrivate(childKey.getPrivKey()));
		}

		return derivedKeysList;
	}

	public byte[] signMessage(byte[] messageBytes, byte[] privateKeyBytes) {
		ECKey key = ECKey.fromPrivate(privateKeyBytes);
		ECDSASignature signature = key.sign(Sha256Hash.of(messageBytes));
		return signature.encodeToDER();
	}

}
