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

package org.unigrid.janus.model.service.external;

import jakarta.ws.rs.client.ClientBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import mockit.Mock;
import mockit.MockUp;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyInvocation;
import org.glassfish.jersey.client.JerseyWebTarget;

public class WebTargetMockUp extends MockUp<JerseyWebTarget> {
	public static class SneakyBuilder extends JerseyInvocation.Builder {
		public SneakyBuilder() {
			super(URI.create("http://localhost:4242"),
				(ClientConfig) ClientBuilder.newClient().getConfiguration()
			);
		}
	}

	@Mock
	public JerseyInvocation.Builder request() throws URISyntaxException {
		final SneakyBuilder builder = new SneakyBuilder();
		return builder;
	}
}
