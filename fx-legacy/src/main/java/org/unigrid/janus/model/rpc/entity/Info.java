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

package org.unigrid.janus.model.rpc.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.json.bind.annotation.JsonbProperty;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)
public class Info extends BaseResult<Info.Result> {
	public static final String METHOD = "getinfo";

	public static class Request extends BaseRequest {

		public Request() {
			super(METHOD);
		}
	}

	@Data
	public static class Result {
		private int version;
		@JsonbProperty("walletversion")
		private int walletVersion;
		@JsonbProperty("protocolversion")
		private int protocolVersion;
		private float totalbalance;
		private BigDecimal balance;
		@JsonbProperty("unlocked_until")
		private long unlockUntil = 4999;
		// unpacked from nested bootstrapping
		private double moneysupply;
		private double blacklisted;
		private int blocks;
		@JsonbProperty("connections")
		private int connections;
		private String status;
		private Bootstrap bootstrapping;
	}

	@Data
	public static class Bootstrap {
		private String walletstatus;
		private String status;
		private String progress;
	}
}
