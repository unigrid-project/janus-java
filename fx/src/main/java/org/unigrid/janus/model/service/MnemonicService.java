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

import com.evolvedbinary.j8fu.function.TriConsumer;
import com.evolvedbinary.j8fu.function.TriFunction;
import jakarta.inject.Inject;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.unigrid.janus.model.AccountModel;
import org.unigrid.janus.model.MnemonicModel;
import org.unigrid.janus.utils.AddressUtil;
import org.unigrid.janus.utils.CosmosCredentials;

public class MnemonicService {
	@FXML
	private TextArea seedPhraseTextArea;	
	@FXML
	private TextField addressFieldPassword;
	@Inject
	private AccountModel accountModel;
	@Inject
	private MnemonicModel mnemonicModel;

	private final List<String> mnemonicWordList = new ArrayList<>();

	public List<String> getMnemonicWordList() {
		return mnemonicWordList;
	}

	public void setMnemonicFromClipboard() {
		final Clipboard clipboard = Clipboard.getSystemClipboard();
		if (clipboard.hasString()) {
			String mnemonic = clipboard.getString();
			String[] words = mnemonic.split("\\s+"); // Split by whitespace
			mnemonicWordList.clear();
			mnemonicWordList.addAll(Arrays.asList(words));
		}
	}
	
	public boolean compareMnemonicWithModel() {
		// Convert mnemonicWordList to a space-separated string
		String copiedMnemonic = String.join(" ", mnemonicModel.getMnemonicWordList());

		String modelMnemonic = accountModel.getMnemonic();
		System.out.println("modelMnemonic: " + modelMnemonic);
		System.out.println("copiedMnemonic: " + copiedMnemonic);

		// Compare the two mnemonics
		return copiedMnemonic.equals(modelMnemonic);
	}

	final TriFunction<DeterministicKey, Integer, Boolean, DeterministicKey> deriveKey = (key, child, hardened) -> {
		return HDKeyDerivation.deriveChildKey(key, new ChildNumber(child, hardened));
	};

	public byte[] derivePrivateKeyFromMnemonic(String mnemonic, int index) {
		final List<String> mnemonicWords = Arrays.asList(mnemonic.split(" "));
		final byte[] seed = MnemonicCode.toSeed(mnemonicWords, "");

		// Derive the key step by step following the path "M/44'/118'/0'/0/0"
		final DeterministicKey masterKey = HDKeyDerivation.createMasterPrivateKey(seed);
		final DeterministicKey level1 = deriveKey.apply(masterKey, 44, true);
		final DeterministicKey level2 = deriveKey.apply(level1, 118, true);
		final DeterministicKey level3 = deriveKey.apply(level2, 0, true);
		final DeterministicKey level4 = deriveKey.apply(level3, 0, false);
		final DeterministicKey key = deriveKey.apply(level4, index, false);

		return key.getPrivKeyBytes();
	}
	
	public void generateMnemonicAddress() throws MnemonicException.MnemonicLengthException {
		// TODO find a better way to handle multiple accounts
		// and addresses
		int index = 0;

		// Step 1: Generate a new 12-word mnemonic
		SecureRandom secureRandom = new SecureRandom();
		List<String> mnemonicWords = MnemonicCode.INSTANCE
			.toMnemonic(secureRandom.generateSeed(32));
		String mnemonic = String.join(" ", mnemonicWords);
		// this.mnemonicArea.setText(mnemonic);

		// // Encrypt the mnemonic before setting it to the accountModel
		// String password1 = passwordField1.getText();
		// String encryptedPrivateKey = cryptoUtils.encrypt(mnemonic, password1);
		// System.out.println("Set encrypted mnemonic: " + accountModel.getMnemonic());
		accountModel.setMnemonic(mnemonic);

		// Utilize the derivePrivateKeyFromMnemonic method and CosmosCredentials block
		byte[] privateKey = derivePrivateKeyFromMnemonic(mnemonic, index);
		System.out.println(
			"Private Key: " + org.bitcoinj.core.Utils.HEX.encode(privateKey));

		String path = String.format("m/44'/118'/0'/0/%d", index);
		CosmosCredentials creds = AddressUtil.getCredentials(mnemonic, "", path,
			"unigrid");

		System.out.println("Address from creds: " + creds.getAddress());
		System.out.println("EcKey from creds: " + creds.getEcKey());
		// Populate the AccountModel
		accountModel.setMnemonic(mnemonic);
		System.out.println("Set mnemonic: " + accountModel.getMnemonic());

		accountModel.setAddress(creds.getAddress());
		System.out.println("Set address: " + accountModel.getAddress());

		accountModel.setPrivateKey(privateKey);
		accountModel.setPublicKey(creds.getEcKey().getPubKey());

		// Update UI fields
		seedPhraseTextArea.setStyle(
			"-fx-font-size: 25px; -fx-background-color: rgba(0, 0, 0, 0.2);");
		seedPhraseTextArea.setText(accountModel.getMnemonic());

		addressFieldPassword.setText(accountModel.getAddress());
		// addressCosmos.setAddress(creds.getAddress());
		// addressCosmos.setPublicKey(org.bitcoinj.core.Utils.HEX.encode(creds.getEcKey().getPubKey()));
		// addressCosmos.setName("pickles_" + index);
		// addressCosmos.setKeyIndex(index);
		// handleSaveAddress(addressCosmos);
	}

	// Other methods related to mnemonics...
}
