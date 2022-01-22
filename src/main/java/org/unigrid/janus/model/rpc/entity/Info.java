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
import jakarta.json.bind.annotation.JsonbProperty;

@Data
@EqualsAndHashCode(callSuper = false)
public class Info extends BaseResult<Info.Result> {
	private static final String METHOD = "getinfo";

	public static class Request extends BaseRequest {
		public Request() {
			super(METHOD);
		}
	}

	@Data
	public static class Result {
		@JsonbProperty("version")
		private int version;
		@JsonbProperty("walletversion")
		private int walletVersion;
		@JsonbProperty("protocolversion")
		private int protocolVersion;
		@JsonbProperty("totalbalance")
		private float totalbalance;
		@JsonbProperty("balance")
		private float balance;

		// unpacked from nested bootstrapping
		@JsonbProperty("moneysupply")
		private double moneysupply;
		@JsonbProperty("blacklisted")
		private double blacklisted;
		@JsonbProperty("blocks")
		private int blocks;
		@JsonbProperty("connections")
		private int connections;

		@JsonbProperty("bootstrapping")
		private Bootstrap bootstrapping;
	}

	@Data
	public static class Bootstrap {
		private String walletstatus = "none";
	}
}
