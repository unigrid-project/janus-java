/*
	The Janus Wallet
	Copyright © 2021-2022 The Unigrid Foundation, UGD Software AB

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

import jakarta.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import lombok.NoArgsConstructor;
import org.web3j.crypto.MnemonicUtils;
import org.web3j.crypto.Bip39Wallet;
import org.web3j.crypto.Bip44WalletUtils;
import org.web3j.crypto.CipherException;

@ApplicationScoped
@NoArgsConstructor
public class FxMnemonic {

	public String generateWallet(String password, boolean testnet) throws CipherException, IOException {
		String dataDir = DataDirectory.get() + "/ugdcos/";
		File file = new File(dataDir);
		if (!file.exists()) {
			file.mkdir();
		}
		Bip39Wallet wallet = Bip44WalletUtils.generateBip44Wallet(password, file, testnet);

		return wallet.getMnemonic();
	}

	public String generateMnemonic() {
		byte[] initialEntropy = new byte[32];
		SecureRandom serRandom = new SecureRandom();
		serRandom.nextBytes(initialEntropy);
		String mnemonic = MnemonicUtils.generateMnemonic(initialEntropy);
		return mnemonic;
	}

	public void generateKeyring(String mnemonic, String password) {
		if (!MnemonicUtils.validateMnemonic(mnemonic)) {
			return;
		}

		byte[] seed = MnemonicUtils.generateSeed(mnemonic, password);
		//HDWallet hdWallet = HDWallet.create(seed);
	}
}
