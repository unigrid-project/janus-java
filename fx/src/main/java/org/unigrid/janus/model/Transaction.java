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

import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Data
public class Transaction {
	private String account;
	private String address;
	private String category;
	private double amount;
	private double fee;
	private long time;
	private long timereceived;
	private int confirmations;
	private String txid;
	private boolean generated;
	private String generatedfrom;
	private List<Transaction> parts = new ArrayList<Transaction>();

	public Transaction() {
		/* empty on purpose */
	}

	public Transaction(String acct, String addr, String cat, double amt, long tm) {
		this.account = acct;
		this.address = addr;
		this.category = cat;
		this.amount = amt;
		this.time = tm;
	}

	public boolean hasPart(Transaction trans) {
		boolean result = false;
		// it can't be a part unless it has same txid
		if (trans.txid != this.txid) {
			return false;
		}
		for (Transaction t : this.parts) {
			if ((t.address.equals(trans.address))
				&& (t.category.equals(trans.category))
				&& (t.amount == trans.amount)
				&& (t.time == trans.time)) {
				result = true;
				break;
			}
		}
		return result;
	}

	public boolean addPart(Transaction trans) {
		boolean result = false;
		// TODO: add up transaction amount of parent from transaction parts.
		if (!hasPart(trans)) {
			this.parts.add(trans);
			this.amount += trans.getAmount();
			result = true;
		}
		return result;
	}

	public Transaction convertToMultiPart() {
		Transaction result = new Transaction(
			this.account,
			this.address,
			"multipart",
			this.amount,
			this.time);
		result.setTxid(this.txid);
		result.addPart(this);
		return result;
	}

	public boolean equals(Transaction trans) {
		return (trans.txid.equals(this.txid)
			&& trans.address.equals(this.address)
			&& trans.category.equals(this.category)
			&& (trans.amount == this.amount)
			&& (trans.time == this.time));
	}
}
