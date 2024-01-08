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
import java.util.Arrays;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import org.unigrid.janus.model.AccountsData;
import org.unigrid.janus.model.CryptoUtils;

@ApplicationScoped
public class DecryptCosmosService {
	@Inject
	private CryptoUtils cryptoUtils;

	@FXML
	private void decryptPrivateKey(ActionEvent event) {
		AccountsData.Account selectedAccount = accountsData.getSelectedAccount();
		if (selectedAccount == null) {
			System.out.println("No account selected!");
			return;
		}
		String encryptedPrivateKey = selectedAccount.getEncryptedPrivateKey();
		System.out.println("encryptedPrivateKey: " + encryptedPrivateKey);
		// Prompt the user to enter the password
		String password = getPasswordFromUser();
		if (password == null) {
			System.out.println("Password input cancelled!");
			return;
		}
		// Decrypt the private key. The returned value should be the original private
		// key bytes.
		byte[] privateKeyBytes = cryptoUtils.decrypt(encryptedPrivateKey, password);
		System.out.println(
			"Decrypted Private Key (Bytes): " + Arrays.toString(privateKeyBytes));
		System.out.println("Decrypted Private Key (HEX): "
			+ cryptoUtils.bytesToHex(privateKeyBytes));
		// Convert the private key bytes to a HEX string
		String privateKeyHex = org.bitcoinj.core.Utils.HEX.encode(privateKeyBytes);
		System.out.println("Private Key in HEX: " + privateKeyHex);
		System.out.println("Address from priv key: "
			+ cryptoUtils.getAddressFromPrivateKey(privateKeyHex));
	}
}
