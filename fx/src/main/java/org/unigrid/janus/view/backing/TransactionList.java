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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Data;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import org.unigrid.janus.model.Transaction;
import org.unigrid.janus.model.service.RPCService;
import org.unigrid.janus.model.service.DebugService;
import org.unigrid.janus.model.rpc.entity.ListTransactions;

// TODO: Clean me
@ApplicationScoped
public class TransactionList {
	public static final String TRANSACTION_LIST = "transactionlist";

	@Inject private DebugService debug;
	@Inject private RPCService rpc;

	private static PropertyChangeSupport pcs;
	private static ObservableList<Transaction> transactions = FXCollections.observableArrayList();

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

	public TransactionList() {
		if (pcs != null) {
			return;
		}

		pcs = new PropertyChangeSupport(this);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.removePropertyChangeListener(listener);
	}

	public ObservableList<Transaction> getTransactions() {
		return this.transactions;
	}

	public ObservableList<Transaction> getLatestTransactions(int count) {
		System.out.println(this.transactions.size());
		count = count > transactions.size() ? transactions.size() : count;

		return (ObservableList<Transaction>) FXCollections.observableArrayList(transactions.subList(0, count));
	}

	public int loadNewTransactions() {
		ListTransactions trans = rpc.call(new ListTransactions.Request(0, 10), ListTransactions.class);
		int iNewCount = 0;
		// TODO: merge new trans to beginning of list
		return iNewCount;
	}

	// return index of first duplicate, or -1 if not found.
	public int isDuplicate(Transaction transaction, boolean beginning) {
		int result = -1;

		if (transaction.getTxid() == null) {
			return -1;
		}

		try {
			int index = 0;

			for (Transaction t : transactions) {
				if (t.getTxid() != null && transaction.getTxid() != null) {
					if (t.getTxid().equals(transaction.getTxid())) {
						result = index;
						break;
					}
				}

				index++;
			}
		} catch (Exception e) {
			debug.log(String.format("ERROR: (TransactionList isDuplicate) %s", e.getMessage()));
		}

		debug.print("is duplicate transaction index: " + result, TransactionList.class.getSimpleName());
		return result;
	}

	public Transaction getMultiPart(Transaction transaction, boolean beginning) {
		Transaction result = null;

		for (Transaction t : this.transactions) {
			if (t.getCategory().equals("multipart")
				&& t.getTxid().equals(transaction.getTxid())) {
				result = t;
				break;
			}
		}

		return result;
	}

	/**
	 * Add transaction if its not duplicate, or aggregate into multipart if the new transaction is a part of a multipart.
	 *
	 * @param index If this is not a part of another transaction, insert at index.
	 * @param trans The new transaction to add (maybe)
	 * @return True if added as new, false if added to multipart or not added. This is used to determine if transaction
	 * count changed.
	 */
	public boolean addTransaction(int index, Transaction trans) {
		boolean result = false;
		try {
			//boolean beginning = (index == 0);
			int idx = -1; //isDuplicate(trans, beginning);

			if (idx != -1) {
				// TODO: check for multipart and add, or create multipart if needed.
				Transaction t = transactions.get(idx);

				if (t.getCategory().equals("multipart")) {
					// add part to found transaction if not already there
					// this isn't adding to transactions count, so return false.
					t.addPart(trans);
				} else {
					if (!trans.equals(t)) {
						Transaction multi = t.convertToMultiPart();
						multi.addPart(trans);
						transactions.set(idx, multi);
					}
				}
			} else {
				transactions.add(index, trans);
				result = true;
			}
		} catch (Exception e) {
			debug.log(String.format("ERROR: (TransactionList addTransaction) %s", e.getMessage()));
		}
		return result;
	}

	public LoadReport loadTransactions(int count) {
		LoadReport result = new LoadReport(transactions.size());
		// load a few before the end of this list just in case there were
		// new transactions since the last load
		debug.log(String.format("Transaction count before load: %d", transactions.size()));
		int offset = transactions.size() - 10;

		if (offset < 0) {
			offset = 0;
		}

		int load = (int) Math.round(count * 1.5);
		debug.log(String.format("Offset: %d and count: %d",  offset,  load));
		ListTransactions trans = rpc.call(new ListTransactions.Request(offset, load), ListTransactions.class);

		// TODO: merge new trans to end of list
		for (Transaction t : trans.getResult()) {
			// transactions.add(result.oldSize, t);
			this.addTransaction(result.getOldSize(), t);
		}

		this.processMultipart();
		result.setNewSize(transactions.size());
		debug.log(String.format("New size: %d", result.getNewSize()));
		return result;
	}

	public void setTransactions(ListTransactions transList, int offset) {
		int oldCount = 0;
		transactions.clear();
		int newCount = 0;

		for (Transaction t : transList.getResult()) {
			// add to the beginning to sort in reverse date order
			// transactions.add(0, t);
			this.addTransaction(0, t);
			newCount++;
		}

		processMultipart();
		pcs.firePropertyChange(this.TRANSACTION_LIST, oldCount, newCount);
	}

	public void processMultipart() {
		for (Transaction t : this.transactions) {
			if (t.getCategory().equals("multipart")) {
				String address = "";
				double sendAmount = 0;
				double receiveAmount = 0;
				double fee = 0;

				for (Transaction trans : t.getParts()) {
					if (trans.getCategory().equals("send")) {
						sendAmount += trans.getAmount();
						// fee is going to be the same for each send transactions
						// and there is only one fee
						fee = trans.getFee();
					} else if (trans.getCategory().equals("receive")) {
						receiveAmount += trans.getAmount();
						address = trans.getAddress();
					}
				}

				if (t.getParts().size() == 2) {
					t.setAddress(address);
					t.setCategory("fee");
					t.setAmount(sendAmount + receiveAmount + fee);
					t.setFee(fee);
				} else {
					double total = sendAmount + receiveAmount + fee;

					if (total > 0) {
						t.setAddress(address);
						t.setCategory("receive");
						t.setAmount(receiveAmount);
						t.setFee(fee);
					} else {
						t.setCategory("send");
						t.setAmount(total - fee);
						t.setFee(fee);

						for (Transaction trans : t.getParts()) {
							if (trans.getCategory().equals("send")
								&& !trans.getAddress().equals(address)) {
								t.setAddress(trans.getAddress());
							}
						}
					}
				}
			}
		}
	}
}
