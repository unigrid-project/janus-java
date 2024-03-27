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

package org.unigrid.janus.utils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Bech32;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Bech32.Encoding;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException.MnemonicLengthException;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

//import org.unigrid.cosmos.util.AddressUtil;
//import org.unigrid.cosmos.CosmosRestApiClient;
//import org.unigrid.cosmos.crypto.CosmosCredentials;
//import org.unigrid.cosmos.vo.SendInfo;
//
//import cosmos.base.abci.v1beta1.Abci;
import java.security.Security;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.unigrid.pax.sdk.cosmos.UnigridCredentials;

public class MnemonicToPrivateKey {

	static {
		Security.addProvider(new BouncyCastleProvider());
	}
//	public static byte[] derivePrivateKeyFromMnemonic(String mnemonic)
//		throws UnreadableWalletException {
//		DeterministicSeed seed = new DeterministicSeed(mnemonic, null, "", 0);
//		DeterministicKeyChain chain = DeterministicKeyChain.builder().seed(seed).build();
//		// Define the BIP44 path using ChildNumber instances
//		List<ChildNumber> path = Arrays.asList(
//			new ChildNumber(44, true),
//			new ChildNumber(118, true),
//			new ChildNumber(0, true),
//			ChildNumber.ZERO,
//			ChildNumber.ZERO
//		);
//		// Get the key for the specified path
//		DeterministicKey key = chain.getKeyByPath(path, true);
//		// public key and address
//		byte[] pubKey = key.getPubKey();
//		// Hash the public key with SHA-256
//		byte[] sha256Hash = Sha256Hash.hash(pubKey);
//		// Hash the result of the SHA-256 hash with RIPEMD-160
//		RIPEMD160Digest digest = new RIPEMD160Digest();
//		digest.update(sha256Hash, 0, sha256Hash.length);
//		byte[] ripemd160Hash = new byte[20];
//		digest.doFinal(ripemd160Hash, 0);
//		// Convert from 8-bit to 5-bit representation
//		byte[] convertedBits = convertBits(ripemd160Hash, 8, 5, true);
//		// Encode the result using Bech32 with the HRP "unigrid"
//		String address = Bech32.encode("unigrid", convertedBits);
//		System.out.println("Address: " + address);
//		byte[] privateKeyBytes = key.getPrivKeyBytes();
//		return privateKeyBytes;  // returns the private key as a byte array
//	}

	public static byte[] derivePrivateKeyFromMnemonic(String mnemonic) throws Exception {
		// Convert mnemonic to seed
		List<String> mnemonicWords = Arrays.asList(mnemonic.split(" "));

		byte[] seed = MnemonicCode.toSeed(mnemonicWords, "");

		// Create master key from seed
		DeterministicKey masterKey = HDKeyDerivation.createMasterPrivateKey(seed);

		// Derive the key step by step following the path "M/44'/118'/0'/0/0"
		DeterministicKey level1 = HDKeyDerivation.deriveChildKey(masterKey,
			new ChildNumber(44, true));
		DeterministicKey level2 = HDKeyDerivation.deriveChildKey(level1,
			new ChildNumber(118, true));
		DeterministicKey level3 = HDKeyDerivation.deriveChildKey(level2,
			new ChildNumber(0, true));
		DeterministicKey level4 = HDKeyDerivation.deriveChildKey(level3,
			new ChildNumber(0, false));
		DeterministicKey key = HDKeyDerivation.deriveChildKey(level4,
			new ChildNumber(0, false));

		// Optionally print the address (requires additional logic for accurate Cosmos address computation)
		// byte[] publicKey = key.getPubKey();
		// byte[] address = computeAddress(publicKey);
		// System.out.println("Address: " + Base64.encodeBase64String(address));  // Replace with correct encoding
		// Return private key bytes
		byte[] privateKeyBytes = key.getPrivKeyBytes();
		return privateKeyBytes;
	}

	public static void main(String[] args) throws UnreadableWalletException, Exception {
		String mnemonic = "chronic cash supply shed huge basic tide minute three wagon "
			+ "quick carpet ability alone trim void gallery stool betray "
			+ "oval desert equal mother chronic";

		byte[] privateKey = derivePrivateKeyFromMnemonic(mnemonic);
		System.out.println("Private Key: " + org.bitcoinj.core.Utils.HEX.encode(privateKey));
		//generateKeys(mnemonic);
		//
		UnigridCredentials creds = AddressUtil.getCredentials(mnemonic, "",
			"m/44'/118'/0'/0/0", "unigrid");

		System.out.println("Address from creds: " + creds.getAddress());
		System.out.println("EcKey from creds: " + creds.getEcKey());
		//testSendMultiTx(privateKey);
		//System.out.println("Private Key: " + Arrays.toString(privateKey));
		//generateMnemonicAddress();
	}

	// public static byte[] derivePrivateKeyFromMnemonic(String mnemonic) throws Exception {
	// 	// Create a deterministic seed using the mnemonic
	// 	DeterministicSeed seed = new DeterministicSeed(mnemonic, null, "", 0);
	// 	// Create a deterministic key chain using the seed
	// 	DeterministicKeyChain chain = DeterministicKeyChain.builder().seed(seed).build();
	// 	// Define the BIP44 path for the first key in the BIP44 wallet structure
	// 	List<ChildNumber> path = Arrays.asList(
	// 		new ChildNumber(44, true),
	// 		new ChildNumber(118, true),
	// 		new ChildNumber(0, true),
	// 		ChildNumber.ZERO_HARDENED,
	// 		ChildNumber.ZERO
	// 	);
	// 	// Get the key following the BIP44 path
	// 	DeterministicKey key = chain.getKeyByPath(path, true);
	// 	// Get the private key bytes
	// 	byte[] privateKeyBytes = key.getPrivKeyBytes();
	// 	return privateKeyBytes;
	// }
	public static byte[] convertBits(final byte[] data, final int fromBits, final int toBits,
		final boolean pad) throws AddressFormatException {
		int acc = 0;
		int bits = 0;
		final ByteArrayOutputStream out = new ByteArrayOutputStream(64);
		final int maxv = (1 << toBits) - 1;
		final int maxAcc = (1 << (fromBits + toBits - 1)) - 1;
		for (int i = 0; i < data.length; i++) {
			final int value = data[i] & 0xff;
			if ((value >>> fromBits) != 0) {
				throw new AddressFormatException(
					String.format("Input value '%X' exceeds '%d' bit size",
						value, fromBits));
			}
			acc = ((acc << fromBits) | value) & maxAcc;
			bits += fromBits;
			while (bits >= toBits) {
				bits -= toBits;
				out.write((acc >>> bits) & maxv);
			}
		}
		if (pad) {
			if (bits > 0) {
				out.write((acc << (toBits - bits)) & maxv);
			}
		} else if (((acc << (toBits - bits)) & maxv) != 0 || bits >= fromBits) {
			throw new AddressFormatException("Could not convert bits, invalid padding");
		}
		return out.toByteArray();
	}

	public static void generateMnemonicAddress() throws Exception {
		// Step 1: Generate a new 12-word mnemonic
		SecureRandom secureRandom = new SecureRandom();
		List<String> mnemonicWords = null;
		try {
			mnemonicWords = MnemonicCode.INSTANCE.toMnemonic(secureRandom.generateSeed(32));
		} catch (MnemonicLengthException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String mnemonic = String.join(" ", mnemonicWords);

		// Step 2: Derive the private key from the mnemonic
		DeterministicSeed seed = null;
		try {
			seed = new DeterministicSeed(mnemonic, null, "cosmos", 2048);
		} catch (UnreadableWalletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DeterministicKeyChain chain = DeterministicKeyChain.builder().seed(seed).build();

		// Define the BIP44 path using ChildNumber instances
		List<ChildNumber> path = Arrays.asList(
			new ChildNumber(44, true),
			new ChildNumber(118, true),
			new ChildNumber(0, true),
			ChildNumber.ZERO,
			ChildNumber.ZERO
		);

		// Get the key for the specified path
		DeterministicKey key = chain.getKeyByPath(path, true);

		// Step 3: Get the public key
		byte[] pubKey = key.getPubKey();

		// Step 4: Hash the public key with SHA-256
		byte[] sha256Hash = Sha256Hash.hash(pubKey);

		// Step 5: Hash the result of the SHA-256 hash with RIPEMD-160
		RIPEMD160Digest digest = new RIPEMD160Digest();
		digest.update(sha256Hash, 0, sha256Hash.length);
		byte[] ripemd160Hash = new byte[20];
		digest.doFinal(ripemd160Hash, 0);

		// Step 6: Convert from 8-bit to 5-bit representation
		byte[] convertedBits = convertBits(ripemd160Hash, 8, 5, true);

		// Step 7: Encode the result using Bech32 with the HRP "unigrid"
		String address = Bech32.encode(Encoding.BECH32, "unigrid", convertedBits);

		System.out.println("Mnemonic: " + mnemonic);
		System.out.println("Address: " + address);
		byte[] privateKey = null;
		try {
			privateKey = derivePrivateKeyFromMnemonic(mnemonic);
		} catch (UnreadableWalletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Private Key: " + Arrays.toString(privateKey));
	}

	public static void testSendMultiTx(byte[] key) throws Exception {
//uncomment all		CosmosRestApiClient unigridApiService = new CosmosRestApiClient("http://localhost:1317",
//			"cosmosdaemon", "ugd");
//
//		CosmosCredentials credentials = CosmosCredentials.create(key, "unigrid");
//		// generate address
//		System.out.println("address:" + credentials.getAddress());
//		List<SendInfo> sendList = new ArrayList<>();
//		// add a send message
//		SendInfo sendMsg1 = SendInfo.builder()
//			.credentials(credentials)
//			.toAddress("unigrid16d3mfh60csakuld5dc7pgj28wk0kda4s5eeqhs")
//			.amountInAtom(new BigDecimal("0.0001"))
//			.build();
//		sendList.add(sendMsg1);
//		// add a send message
//		SendInfo sendMsg2 = SendInfo.builder()
//			.credentials(credentials)
//			.toAddress("unigrid16d3mfh60csakuld5dc7pgj28wk0kda4s5eeqhs")
//			.amountInAtom(new BigDecimal("0.0001"))
//			.build();
//		sendList.add(sendMsg2);
//		// build、sign、broadcast transactions
//		Abci.TxResponse txResponse = unigridApiService.sendMultiTx(credentials,
//			sendList, new BigDecimal("0.000001"), 200000);
		//System.out.println(txResponse.toString());
		System.out.println("transactions sent");
		// query send tx by height
	}

	public static void generateKeys(String mnemonic) throws NoSuchAlgorithmException, UnreadableWalletException,
		Exception {
		// Derive the seed from the mnemonic
		DeterministicSeed seed = new DeterministicSeed(mnemonic, null, "", 0);
		DeterministicKeyChain chain = DeterministicKeyChain.builder().seed(seed).build();

		// Define the BIP44 path for Cosmos
		List<ChildNumber> path = parsePath("m/44'/118'/0'/0/0");
		DeterministicKey key = chain.getKeyByPath(path, true);

		// Get the private and public keys
		byte[] privKeyBytes = key.getPrivKeyBytes();
		byte[] pubKeyBytes = key.getPubKey();

		// Print private and public keys
		System.out.println("Private key: " + bytesToHex(privKeyBytes));
		System.out.println("Public key: " + bytesToHex(pubKeyBytes));

		// Hash the public key for the address
		MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
		byte[] sha256Hash = sha256.digest(pubKeyBytes);

		// This is a simplified version, you may need to process the hash
		// further and use a Bech32 library for encoding
		byte[] ripemd160Hash = ripemd160(sha256Hash);
		byte[] convertedBits = convertBits(ripemd160Hash, 8, 5, true);
		String address = Bech32.encode("unigrid", convertedBits);
		//String address = Bech32.encode("unigrid", sha256Hash);
		System.out.println("Address: " + address);
	}

	// Helper function to convert bytes to hex
	private static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	public static List<ChildNumber> parsePath(String path) {
		String[] segments = path.split("/");
		List<ChildNumber> childNumbers = new ArrayList<>();
		for (int i = 1; i < segments.length; i++) {  // Start at 1 to skip the "m"
			String segment = segments[i];
			boolean isHardened = segment.endsWith("'");
			int index = Integer.parseInt(segment.replace("'", ""));
			childNumbers.add(new ChildNumber(index, isHardened));
		}
		return childNumbers;
	}

	private static byte[] sha256(byte[] data) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256", "BC");
		return digest.digest(data);
	}

	private static byte[] ripemd160(byte[] data) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("RipeMD160", "BC");
		return digest.digest(data);
	}

}
