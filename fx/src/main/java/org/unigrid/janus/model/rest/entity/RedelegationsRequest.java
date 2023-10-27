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

package org.unigrid.janus.model.rest.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.unigrid.janus.model.ApiConfig;

@Data
@EqualsAndHashCode(callSuper = true)
public class RedelegationsRequest extends BaseRequest<RedelegationsRequest.Response> {
	private static final String ENDPOINT = "cosmos/staking/v1beta1/delegators/";

	public RedelegationsRequest(String delegatorAddress) {
		super("GET", buildUrl(delegatorAddress));
	}

	private static String buildUrl(String delegatorAddress) {
		return ApiConfig.getBASE_URL() + ENDPOINT + delegatorAddress + "/redelegations";
	}

	@Override
	public Response convertResponse(String responseBody) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.readValue(responseBody, Response.class);
		} catch (IOException e) {
			throw new RuntimeException("Failed to convert response", e);
		}
	}

	@Data
	public static class Response {
		@JsonProperty("redelegation_responses")
		private List<RedelegationResponseEntry> redelegationResponses;
		private Pagination pagination;
	}

	@Data
	public static class RedelegationResponseEntry {
		private Redelegation redelegation;
		private List<Entry> entries;

		@Data
		public static class Redelegation {
			@JsonProperty("delegator_address")
			private String delegatorAddress;
			@JsonProperty("validator_src_address")
			private String validatorSrcAddress;
			@JsonProperty("validator_dst_address")
			private String validatorDstAddress;
			private List<Entry> entries;
		}

		@Data
		public static class Entry {
			@JsonProperty("redelegation_entry")
			private RedelegationEntry redelegationEntry;
			private String balance;

			@Data
			public static class RedelegationEntry {
				@JsonProperty("creation_height")
				private long creationHeight;
				@JsonProperty("completion_time")
				private String completionTime;
				@JsonProperty("initial_balance")
				private String initialBalance;
				@JsonProperty("shares_dst")
				private String sharesDst;
				@JsonProperty("unbonding_id")
				private long unbondingId;
			}
		}
	}

	@Data
	public static class Pagination {
		@JsonProperty("next_key")
		private String nextKey;
		private String total;
	}
}
