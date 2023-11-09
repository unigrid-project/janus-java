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
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.unigrid.janus.model.ApiConfig;

import java.io.IOException;

@Data
@EqualsAndHashCode(callSuper = true)
public class WithdrawAddressRequest extends BaseRequest<WithdrawAddressRequest.Response> {
	private static final String ENDPOINT = "cosmos/distribution/v1beta1/delegators/";

	public WithdrawAddressRequest(String delegatorAddress) {
		super("GET", buildUrl(delegatorAddress));
	}

	private static String buildUrl(String delegatorAddress) {
		return ApiConfig.getBASE_URL() + ENDPOINT + delegatorAddress + "/withdraw_address";
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
		@JsonProperty("withdraw_address")
		private String withdrawAddress;
	}
}