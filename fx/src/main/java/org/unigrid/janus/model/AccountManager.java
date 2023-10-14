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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.unigrid.janus.model.AccountsData.Account;

@ApplicationScoped
public class AccountManager {

	@Inject private CryptoUtils cryptoUtils;

	public void createAccountFile(AccountModel accountModel) throws IOException {
		if (accountModel == null) {
			throw new IllegalStateException("accountModel is not injected! AccountManager");
		}

		// Create an account
		Account account = new Account();
		account.setName(accountModel.getName());
		account.setAddress(accountModel.getAddress());
		account.setPublicKey(cryptoUtils.bytesToHex(accountModel.getPublicKey()));
		account.setEncryptedMnemonic(accountModel.getEncryptedMnemonic());
		// Load existing accounts or initialize a new list
		List<Account> accounts;
		File accountsFile = DataDirectory.getAccountsFile();
		ObjectMapper objectMapper = new ObjectMapper();
		if (accountsFile.exists() && accountsFile.length() != 0) {
			AccountsData existingData = objectMapper.readValue(accountsFile, AccountsData.class);
			accounts = existingData.getAccounts();
		} else {
			accounts = new ArrayList<>();
		}

		// Add the new account to the list
		accounts.add(account);

		// Wrap the list in an AccountsData
		AccountsData data = new AccountsData();
		data.setAccounts(accounts);

		// Convert the data to JSON and save it to the file
		objectMapper.writeValue(accountsFile, data);
	}
}
