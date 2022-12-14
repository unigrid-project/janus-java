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

import java.math.BigDecimal;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.unigrid.janus.model.Recipent;

@Data
@EqualsAndHashCode(callSuper = false)
public class SendMany extends BaseResult<String> {
	private static final String METHOD = "sendmany";

	public static class Request extends BaseRequest {
		// TODO: Start using category and recipent
		/* sendmany "tabby" '{"DMJRSsuU9zfyrvxVaAEFQqK4MxZg6vgeS6" 0.1', "HCV3aBdr9MCn1P8p5aWSRCrZHULNFGBoEZ" 0.2}' 6 "testing" */
		public Request(String fromAccount, Map<String, BigDecimal> mapAmount, int minConf, Recipent recipent) {
			super(METHOD);
			setParams(new Object[]{"", mapAmount, minConf, recipent});
		}

		/* sendtoaddress '{"DMJRSsuU9zfyrvxVaAEFQqK4MxZg6vgeS6" 0.1', "HCV3aBdr9MCn1P8p5aWSRCrZHULNFGBoEZ" 0.2}' */
		public Request(String fromAccount, Map<String, BigDecimal> mapAmount) {
			super(METHOD);
			setParams(new Object[]{fromAccount, mapAmount});
		}
	}
}
