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

package org.unigrid.janus.model.external;

import jakarta.json.bind.JsonbBuilder;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import mockit.Mock;
import mockit.MockUp;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.unigrid.janus.model.rpc.entity.GetNewAddress;
import org.unigrid.janus.model.rpc.entity.ListAddressBalances;

@Data
public class ResponseMockUp extends MockUp<OutboundJaxrsResponse> {

	@Mock
	public <T> T readEntity(Class<T> clazz) {
		switch (clazz.getSimpleName()) {
			case "GetNewAddress" -> {
				return (T) getNewAddress((Class<String>) clazz);
			}
			case "ListAddressBalances" -> {
				return (T) listAddressBalances((Class<List>) clazz, null);
			}
			default -> {
				return null;
			}
		}
	}

	public GetNewAddress getNewAddress(Class<String> clazz) {
		final GetNewAddress gna = new GetNewAddress();
		gna.setResult(JsonbBuilder.create().fromJson(ResponseMockUp.class
			.getResourceAsStream("get_new_address.json"), String.class));
		return gna;
	}

	public ListAddressBalances listAddressBalances(Class<List> clazz, List list) {
		final ListAddressBalances addr = new ListAddressBalances();
		addr.setResult(JsonbBuilder.create().fromJson(
			ResponseMockUp.class.getResourceAsStream("list_address_balances.json"),
			new ArrayList<ListAddressBalances.Result>() {
			}
				.getClass().getGenericSuperclass()));
		return addr;
	}
}
