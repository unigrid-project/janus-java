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
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import org.unigrid.janus.controller.CosmosController;
import org.unigrid.janus.model.AccountsData;
import org.unigrid.janus.model.AccountsData.Account;
import org.unigrid.janus.model.DataDirectory;
import org.unigrid.janus.model.rest.entity.CollateralRequired;
import org.unigrid.janus.model.rest.entity.DelegationsRequest;
import org.unigrid.janus.model.rest.entity.GridnodeDelegationAmount;
import org.unigrid.janus.model.rest.entity.RedelegationsRequest;
import org.unigrid.janus.model.rest.entity.RewardsRequest;
import org.unigrid.janus.model.rest.entity.UnbondingDelegationsRequest;
import org.unigrid.janus.model.rest.entity.WithdrawAddressRequest;
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
	private ComboBox accountsDropdown;
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

	/* MAIN VIEW */
	public void loadAccounts() {
		try {
			accountsService.loadAccountsFromJson();
		} catch (Exception ex) {
			Logger.getLogger(CosmosController.class.getName()).log(Level.SEVERE, null,
				ex);
		}

		// Clear the existing items from the ComboBox
		accountsDropdown.getItems().clear();

		// Populate the ComboBox with account names
		for (AccountsData.Account account : accountsData.getAccounts()) {
			if (account.getName() != null) {
				accountsDropdown.getItems().add(account.getName());
			} else {
				System.out.println("Account name is null");
			}

		}

		// Set the first account as the default selection
		if (!accountsDropdown.getItems().isEmpty()) {
			accountsDropdown.getSelectionModel().selectFirst();
			String defaultAccountName = (String) accountsDropdown.getValue();
			if (defaultAccountName != null) {
				Optional<Account> defaultAccount = accountsService
					.findAccountByName(defaultAccountName);
				if (defaultAccount.isPresent()) {
					accountsData.setSelectedAccount(defaultAccount.get());
				}
			}
		}

		// Set up an action listener for the ComboBox
		accountsDropdown.setOnAction(event -> {
			String selectedAccountName = (String) accountsDropdown.getValue();
			Optional<Account> selectedAccount = accountsService
				.findAccountByName(selectedAccountName);
			if (selectedAccount.isPresent()) {
				accountsData.setSelectedAccount(selectedAccount.get());
				System.out
					.println("Selected Account:" + accountsData.getSelectedAccount());
				addressLabel.setText(accountsData.getSelectedAccount().getAddress());
				System.out.println("getEncryptedPrivateKey: "
					+ accountsData.getSelectedAccount().getEncryptedPrivateKey());
				// Create a background task for the network call
				Task<Void> fetchDataTask = new Task<Void>() {
					@Override
					protected Void call() throws Exception {
						String accountQuery = cosmosClient.checkBalanceForAddress(
							selectedAccount.get().getAddress());
						Platform.runLater(() -> {
							balanceLabel.setText(accountQuery);
						});
						cosmosTxList.loadTransactions(10);
						System.out.println("cosmosTxList.getTxResponse(): "
							+ cosmosTxList.getTxResponsesList());
						loadAccountData(accountsData.getSelectedAccount().getAddress());
						return null;
					}
				};

				// Handle exceptions
				fetchDataTask.setOnFailed(e -> {
					Throwable exception = fetchDataTask.getException();
					Logger.getLogger(CosmosController.class.getName()).log(Level.SEVERE,
						null, exception);
					// Optionally show an error message to the user
				});

				// Start the task in a new thread
				new Thread(fetchDataTask).start();
			}
		});

	}

	private void loadAccountData(String account) throws IOException, InterruptedException {
		RestService restService = new RestService();

		// Delegations
		DelegationsRequest delegationsRequest = new DelegationsRequest(account);
		DelegationsRequest.Response delegationsResponse = new RestCommand<>(
			delegationsRequest, restService).execute();
		setDelegations(delegationsResponse.getDelegationResponses());
		System.out.println(delegationsResponse);

		// Rewards
		RewardsRequest rewardsRequest = new RewardsRequest(account);
		RewardsRequest.Response rewardsResponse = new RestCommand<>(rewardsRequest,
			restService).execute();
		stakingRewardsValue(rewardsResponse.getTotal());
		System.out.println(rewardsResponse);

		// Unbonding Delegations
		UnbondingDelegationsRequest unbondingDelegationsRequest = new UnbondingDelegationsRequest(
			account);
		UnbondingDelegationsRequest.Response unbondingDelegationsResponse = new RestCommand<>(
			unbondingDelegationsRequest, restService).execute();
		System.out.println(unbondingDelegationsResponse);

		// Redelegations
		RedelegationsRequest redelegationsRequest = new RedelegationsRequest(account);
		RedelegationsRequest.Response redelegationsResponse = new RestCommand<>(
			redelegationsRequest, restService).execute();
		System.out.println(redelegationsResponse);

		// Withdraw Address
		WithdrawAddressRequest withdrawAddressRequest = new WithdrawAddressRequest(
			account);
		WithdrawAddressRequest.Response withdrawAddressResponse = new RestCommand<>(
			withdrawAddressRequest, restService).execute();
		System.out.println(withdrawAddressResponse);

		gridnodeDelegationService.fetchDelegationAmount(account);
		setDelegationAmount();

		updateCollateralDisplay();

	}

	public void setDelegationAmount() {
		GridnodeDelegationAmount.Response response = gridnodeDelegationService
			.getCurrentResponse();
		if (response != null) {
			Platform.runLater(() -> {
				BigDecimal amount = response.getAmount();
				String text = amount.toPlainString() + " UGD";
				delegationAmountLabel.setText(text);
			});
		}
	}

	private void updateCollateralDisplay() {
		if (hedgehog.fetchCollateralRequired()) {
			System.out.println("Collateral Required: " + collateral.getAmount());
		} else {
			System.out.println("Error fetching collateral");
		}
	}
}
