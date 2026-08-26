/*
    The Janus Wallet
    Copyright © 2021-2022 Stiftelsen The Unigrid Foundation

    This program is free software: you can redistribute it and/or modify it under the terms of the
    addended GNU Affero General Public License as published by the Free Software Foundation, version 3
    of the License (see COPYING and COPYING.addendum).

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received an addended copy of the GNU Affero General Public License with this program.
    If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.web;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import net.jqwik.api.Example;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiServerTest {
	private HttpResponse<String> get(final URI uri) throws Exception {
		return HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri).build(),
			HttpResponse.BodyHandlers.ofString()
		);
	}

	@Example
	public void shouldServeTheRenderedPage() throws Exception {
		final UiServer server = new UiServer(new UiHandler(new Templates(false)));
		final URI uri = server.start();

		try {
			final HttpResponse<String> response = get(uri);

			assertEquals(200, response.statusCode());
			assertTrue(response.body().contains("<h1>Janus</h1>"), response.body());
		} finally {
			server.stop();
		}
	}

	@Example
	public void shouldBindToLoopbackOnly() throws Exception {
		final UiServer server = new UiServer(new UiHandler(new Templates(false)));
		final URI uri = server.start();

		try {
			assertEquals("127.0.0.1", uri.getHost());
			assertTrue(uri.getPort() > 0, "an ephemeral port should have been assigned");
		} finally {
			server.stop();
		}
	}
}
