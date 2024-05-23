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

package org.unigrid.janus.model.rest.entity;

import lombok.Data;

@Data
public abstract class BaseRequest<T> {
	private String method;
	private String url;
	private T params;

	// Constructor for RPC calls
	protected BaseRequest(String method) {
		this.method = method;
	}

	// Constructor for REST calls
	public BaseRequest(String method, String url) {
		this.method = method;
		this.url = url;
	}

	// Constructor for REST calls with parameters
	public BaseRequest(String method, String url, T params) {
		this.method = method;
		this.url = url;
		this.params = params;
	}

	public abstract T convertResponse(String responseBody);

}
