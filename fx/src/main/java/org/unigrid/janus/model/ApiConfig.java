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

package org.unigrid.janus.model;

import lombok.Getter;

public class ApiConfig {
	@Getter
	private static final String BASE_URL = "https://rest-two-testnet.unigrid.org/";
	//private static final String BASE_URL = "https://rest-devnet.unigrid.org/";
	@Getter
	//private static final String CHAIN_ID = "unigrid-devnet-2";
	private static final String CHAIN_ID = "unigrid-testnet-5";
	@Getter
	//private static final String GRPC_IP = "173.212.208.212"; // devnet
	private static final String GRPC_IP = "207.180.254.48"; // testnet
	@Getter
	private static final String DENOM = "uugd";
	@Getter
	static final String UUGD_VALUE = "100000000";
}
