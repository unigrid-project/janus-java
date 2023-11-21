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

package org.unigrid.janus.model.setup;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.Data;

@Data
@ApplicationScoped
public class AppConfig {
	//private int port = 52884;
	// TESTNET
	private int port = 39886;

	private String baseUrl = "https://127.0.0.1";

	public String getCollateralRequiredUri() {
		return buildUri("/gridnode/collateral");
	}

	public String getHedgehogVersionUri() {
		return buildUri("/version");
	}

	public String getGridsporkUri() {
		return buildUri("/gridspork");
	}

	private String buildUri(String path) {
		return baseUrl + ":" + port + path;
	}
}
