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

public class RoutesTest {
	private HttpResponse<String> get(final URI uri, final String path) throws Exception {
		return HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri.resolve(path)).build(),
			HttpResponse.BodyHandlers.ofString()
		);
	}

	@Example
	public void shouldServeTheStylesheetAsCss() throws Exception {
		final UiServer server = new UiServer(Routes.create(new Templates(false)));
		final URI uri = server.start();

		try {
			final HttpResponse<String> response = get(uri, "/static/css/janus.css");

			assertEquals(200, response.statusCode());
			assertTrue(response.headers().firstValue("content-type").orElse("").startsWith("text/css"),
				response.headers().toString()
			);
			assertTrue(response.body().contains("--accent"), response.body());
		} finally {
			server.stop();
		}
	}

	@Example
	public void shouldFallThroughToThePageForUnknownPaths() throws Exception {
		final UiServer server = new UiServer(Routes.create(new Templates(false)));
		final URI uri = server.start();

		try {
			assertTrue(get(uri, "/").body().contains("<h1>Janus</h1>"));
			assertTrue(get(uri, "/wallet").body().contains("<h1>Janus</h1>"));
		} finally {
			server.stop();
		}
	}
}
