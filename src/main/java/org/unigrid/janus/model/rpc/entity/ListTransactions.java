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

package org.unigrid.janus.model.rpc.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ListTransactions extends BaseResult<ListTransactions.Result> {
	private static final String METHOD = "listtransactions";

	public static class Request extends BaseRequest {
		public Request() {
			super(METHOD);
			this.setParams(new Object[]{"*", 10, 0, true});
		}
	}

	@Data
	public static class Result {

	}

	@Data
	public static class Transaction {
		private String account;
		private String address;
		private String category;
		private double amount;
		private String blockhash;
		private int blockindex;
		private int blocktime;
		private String txid;
		private int time;
		private int timereceived;

		public Transaction(String acct, String addr, String cat, double amt, int tm) {
			this.account = acct;
			this.address = addr;
			this.category = cat;
			this.amount = amt;
			this.time = tm;
			this.timereceived = tm;
		}
	}
}
