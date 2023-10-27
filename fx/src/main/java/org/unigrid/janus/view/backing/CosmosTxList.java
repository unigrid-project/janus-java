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

package org.unigrid.janus.view.backing;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import lombok.Data;
import lombok.Getter;

import org.unigrid.janus.model.AccountsData;
import org.unigrid.janus.model.rpc.entity.TransactionResponse;
import org.unigrid.janus.model.rpc.entity.TransactionResponse.TxResponse;
import org.unigrid.janus.model.service.CosmosRestClient;
import org.unigrid.janus.model.service.DebugService;

@ApplicationScoped
public class CosmosTxList {
	public static final String COSMOS_TX_LIST = "cosmosTxList";

	@Inject
	private DebugService debug;
	@Inject
	private CosmosRestClient restClient;
	@Inject
	private AccountsData accountsData;
	@Getter
	@JsonProperty("tx_responses")
	private List<TxResponse> txResponsesList = new ArrayList<>();

	@Data
	public static class LoadReport {
		private int oldSize;
		private int newCount;
		private int newSize;

		public LoadReport(int size) {
			this.oldSize = size;
			this.newCount = 0;
			this.newSize = size;
		}
	}

//	public void setTxList(GridnodeList list) {
//		txResponse.clear();
//
//		// if (list != null && list.getResult() != null) {
//		// for (TxResponse g : list.getResult()) {
//		// txResponse.add(g);
//		// }
//		// }
//	}
	public LoadReport loadTransactions(int count) {
		LoadReport result = new LoadReport(txResponsesList.size());

		// Log the transaction count before loading new ones
		debug.print(String.format("Transaction count before load: %d", txResponsesList.size()),
			CosmosTxList.class.getSimpleName());

		try {
			// Fetch the latest transactions for the selected account's address
			System.out.println("calling txResponse ");
			TransactionResponse response = restClient
				.txResponse(accountsData.getSelectedAccount().getAddress());
			System.out.println("response.getResult(): " + response);

			// Check if the response is valid and has results
			if (response != null && response.getResult() != null) {
				// Convert the List to an ObservableList and add the new transactions to txResponsesList
				txResponsesList.addAll(FXCollections.observableArrayList(response.getResult()));
				System.out.println("After adding: " + txResponsesList);
			} else {
				System.out.println("response is null: " + response);
			}

			// Update the report with the new size
			result.setNewSize(txResponsesList.size());
			debug.log(String.format("New size: %d", result.getNewSize()));
		} catch (IOException | InterruptedException e) {
			debug.log("Error fetching transactions: " + e.getMessage());
			e.printStackTrace();
		}

		return result;
	}

}
