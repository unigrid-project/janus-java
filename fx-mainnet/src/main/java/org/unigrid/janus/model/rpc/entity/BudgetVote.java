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

import jakarta.json.bind.annotation.JsonbProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class BudgetVote extends BaseResult<BudgetVote.Result> {
	private static final String METHOD = "gnbudget";
	private Result result;

	public static class Request extends BaseRequest {
		public Request(Object[] args) {
			super(METHOD);
			this.setParams(args);
		}
	}

	@Data
	public static class Result {
		@JsonbProperty("result")
		private Response response;

		public String getOverall() {
			if (response != null) {
				return response.getOverall();
			}
			return null;
		}
	}

	@Data
	public static class Response {
		@JsonbProperty("overall")
		private String overall;
	}
}
