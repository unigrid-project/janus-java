/*
	The Janus Wallet
	Copyright © 2021 The Unigrid Foundation

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

import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import org.unigrid.janus.model.rpc.entity.ListTransactions;

public class TransactionList {
	public static final String TRANSACTION_LIST = "transactionlist";
	private static PropertyChangeSupport pcs;

	public TransactionList() {
		if (this.pcs != null) {
			return;
		}
		this.pcs = new PropertyChangeSupport(this);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.removePropertyChangeListener(listener);
	}

	private static ObservableList<Transaction> transactions = FXCollections.observableArrayList();

	public ObservableList<Transaction> getTransactions() {
		return this.transactions;
	}

	public void setTransactions(ListTransactions transList, int offset) {
		int oldCount = 0;
		transactions.clear();
		int newCount = 0;
		for (Transaction t : transList.getResult()) {
			transactions.add(t);
			newCount++;
		}

		this.pcs.firePropertyChange(this.TRANSACTION_LIST, oldCount, newCount);
	}

}
