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

//import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Transaction {
	@NonNull @EqualsAndHashCode.Include private String account;
	@NonNull @EqualsAndHashCode.Include private String address;
	@NonNull @EqualsAndHashCode.Include private String category;
	@NonNull @EqualsAndHashCode.Include private Double amount;
	@NonNull @EqualsAndHashCode.Include private Long time;

	private double fee;
	private long timereceived;
	private int confirmations;
	private boolean generated;
	private String generatedfrom;
	private List<Transaction> parts;

	// TODO this doesnt work
	// had to revert for now to get a hotfix released
	//@JsonProperty("txid")
	private String txid;

	public boolean hasPart(Transaction transaction) {
		boolean result = false;

		if (txid.equals(transaction.txid) && Objects.nonNull(parts)) {
			for (Transaction t : parts) {
				if ((t.address.equals(transaction.address)) && (t.category.equals(transaction.category))
					&& (t.amount == transaction.amount) && (t.time == transaction.time)) {

					result = true;
					break;
				}
			}
		}

		return result;
	}

	public boolean addPart(Transaction trans) {
		boolean result = false;

		// TODO: add up transaction amount of parent from transaction parts.
		if (!hasPart(trans)) {
			parts.add(trans);
			amount += trans.getAmount();
			result = true;
		}

		return result;
	}

	public Transaction convertToMultiPart() {
		final Transaction transaction = new Transaction(account, address, "multipart", amount, time);
		transaction.setTxid(txid);
		transaction.addPart(this);

		return transaction;
	}
}
