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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.unigrid.janus.model.AccountsData;
import org.unigrid.janus.model.AccountsData.Account;
import org.unigrid.janus.model.DataDirectory;
import org.unigrid.janus.model.rest.entity.CollateralRequired;
import org.unigrid.janus.view.backing.CosmosTxList;

@ApplicationScoped
public class AccountsService {

	@Inject
	private AccountsData accountsData;
	@Inject
	private AccountsService accountsService;
	@Inject
	private GridnodeDelegationService gridnodeDelegationService;
	@Inject
	private Hedgehog hedgehog;
	@Inject
	private CollateralRequired collateral;

	@FXML
	private Label addressLabel;
	@Inject
	private CosmosTxList cosmosTxList;
	@FXML
	private Label balanceLabel;
	@FXML
	private Label delegationAmountLabel;

	public void loadAccountsFromJson() throws Exception {
		File accountsFile = DataDirectory.getAccountsFile();
		ObjectMapper objectMapper = new ObjectMapper();
		AccountsData loadedData = objectMapper.readValue(accountsFile, AccountsData.class);

		for (Account account : loadedData.getAccounts()) {
			addAccountIfNotPresent(account);
		}
	}

	public void addAccountIfNotPresent(Account newAccount) {
		Optional<Account> existingAccount = findAccountByName(newAccount.getName());
		if (!existingAccount.isPresent()) {
			accountsData.getAccounts().add(newAccount);
		}
	}

	public Optional<Account> findAccountByName(String accountName) {
		if (accountName == null) {
			return Optional.empty();
		}

		return accountsData.getAccounts().stream()
			.filter(account -> accountName.equals(account.getName()))
			.findFirst();
	}

	public boolean isAccountsJsonEmpty() {
		File accountsFile = DataDirectory.getAccountsFile();
		return !accountsFile.exists() || accountsFile.length() == 0;
	}

	
	
}
