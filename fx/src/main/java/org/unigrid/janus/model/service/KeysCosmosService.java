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
package org.unigrid.janus.model.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.List;

import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.ArrayList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.SignatureDecodeException;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;

import org.unigrid.janus.controller.CosmosController;
import org.unigrid.janus.model.AccountsData;
import org.unigrid.janus.model.AccountsData.Account;
import org.unigrid.janus.model.CryptoUtils;
import org.unigrid.janus.model.rest.entity.CollateralRequired;

@ApplicationScoped
public class KeysCosmosService {

	@Inject
	private GridnodeDelegationService gridnodeDelegationService;
	@Inject
	private CollateralRequired collateral;
	@Inject
	private AccountsData accountsData;
	@Inject
	private CryptoUtils cryptoUtils;

	@FXML
	private void generateKeys(ActionEvent event) throws SignatureDecodeException, Exception {

		BigDecimal currentDelegationAmount = gridnodeDelegationService
			.getCurrentDelegationAmount();
		BigDecimal collateralAmount = BigDecimal.valueOf(collateral.getAmount());
		BigDecimal numberOfNodes = currentDelegationAmount.divide(collateralAmount, 0,
			RoundingMode.DOWN);
		int numberOfNodesInt = numberOfNodes.intValue();
		System.out.println("Nodes we can run: " + numberOfNodesInt);
		int keysToCreate = numberOfNodesInt;
		Account selectedAccount = accountsData.getSelectedAccount();
		String pubKey = selectedAccount.getPublicKey();
		byte[] seed = Sha256Hash.hash(pubKey.getBytes());
		System.out.println("pubKey: " + pubKey);
		System.out.println("seed: " + seed);

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

		ECKey[] derivedKeys = derivedKeysList.toArray(new ECKey[0]);

		// cryptoUtils.printKeys(derivedKeys);
		// Now iterate through the allKeys array, signing and verifying a message with
		// each key
		String messageStr = "Start gridnode message";
		byte[] messageBytes = messageStr.getBytes();
		// get private key to sign with
		String encryptedPrivateKey = selectedAccount.getEncryptedPrivateKey();
		System.out.println("encryptedPrivateKey: " + encryptedPrivateKey);

		// Prompt the user to enter the password
		String password = getPasswordFromUser();
		if (password == null) {
			System.out.println("Password input cancelled!");
			return;
		}

		// Decrypt the private key. The returned value should be the original private
		byte[] privateKeyBytes = cryptoUtils.decrypt(encryptedPrivateKey, password);
		byte[] signedMessage = cryptoUtils.signMessage(messageBytes, privateKeyBytes);

		// Verify the signature and the derived keys
		ECKey publicKey = ECKey.fromPrivate(privateKeyBytes);
		List<ECKey> publicKeysToVerify = derivedKeysList;
		System.out.println("publicKey.getPubKey(): " + publicKey.getPubKey());

		long startTime = System.currentTimeMillis(); // Capture the start time

		boolean areKeysVerified = cryptoUtils.verifySignatureKeys(messageBytes,
			signedMessage, derivedKeys, keysToCreate, pubKey);

		long endTime = System.currentTimeMillis(); // Capture the end time

		long elapsedTime = endTime - startTime; // Calculate the elapsed time

		System.out.println("Are keys verified: " + (areKeysVerified ? "Yes" : "No"));
		System.out.println("Verification time: " + elapsedTime + " milliseconds");

	}

	private void verifyWithBadKeys(byte[] messageBytes, ECKey signingKey)
		throws SignatureDecodeException {
		ECDSASignature signature = signingKey.sign(Sha256Hash.of(messageBytes));
		byte[] signatureBytes = signature.encodeToDER();

		ECKey[] badKeys = {
			ECKey.fromPrivate(new BigInteger("deadbeefdeadbeefdeadbeefdeadbeef", 16)),
			ECKey.fromPrivate(
			new BigInteger("badbadbadbadbadbadbadbadbadbadbad", 16)),
			ECKey.fromPrivate(
			new BigInteger("facefacefacefacefacefacefaceface", 16))};

		// for (int i = 0; i < badKeys.length; i++) {
		// ECKey[] singleKeyArray = { badKeys[i] };
		// boolean isVerified = cryptoUtils.verifySignature(messageBytes,
		// signatureBytes,
		// singleKeyArray);
		// System.out.println("Verification for bad key " + i + ": "
		// + (isVerified ? "Succeeded" : "Failed"));
		// }
	}

	private String getPasswordFromUser() {
		// just use this default for testing right now
		return "pickles";
	}
}
