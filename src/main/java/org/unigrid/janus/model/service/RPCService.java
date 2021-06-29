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

package org.unigrid.janus.model.service;

import java.net.URI;
import javax.annotation.PostConstruct;
//import javax.ejb.Stateless;
//import javax.ws.rs.client.ClientBuilder;
//import javax.ws.rs.client.WebTarget;
//import org.unigrid.janus.model.rpc.JsonConfiguration;

//@Stateless
public class RPCService {
	//private WebTarget target;

	private URI findDaemonEndpoint() {
		return null;
	}

	@PostConstruct
	private void init() {
		/*target = ClientBuilder.newBuilder()
			.register(new JsonConfiguration())
			.build().target(findDaemonEndpoint());*/
	}

	public <T> T call() {
		//target.path(path);
		return null;
	}
}
