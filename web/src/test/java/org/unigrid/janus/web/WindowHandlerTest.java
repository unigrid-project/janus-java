/*
    The Janus Wallet
    Copyright © 2021-2026 Stiftelsen The Unigrid Foundation

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
import java.util.ArrayList;
import java.util.List;
import net.jqwik.api.Example;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WindowHandlerTest {
	private final List<String> invoked = new ArrayList<>();

	private final WindowControl recorder = new WindowControl() {
		@Override
		public void minimise() {
			invoked.add("minimise");
		}

		@Override
		public void toggleMaximise() {
			invoked.add("maximise");
		}

		@Override
		public void close() {
			invoked.add("close");
		}

		@Override
		public void beginMove() {
			invoked.add("move/start");
		}

		@Override
		public void endMove() {
			invoked.add("move/end");
		}
	};

	private int post(final URI uri, final String path) throws Exception {
		return HttpClient.newHttpClient().send(
			HttpRequest.newBuilder(uri.resolve(path)).POST(HttpRequest.BodyPublishers.noBody()).build(),
			HttpResponse.BodyHandlers.discarding()
		).statusCode();
	}

	@Example
	public void shouldForwardEachCommandToTheHost() throws Exception {
		final UiServer server = new UiServer(Routes.create(new Templates(false), recorder));
		final URI uri = server.start();

		try {
			assertEquals(204, post(uri, "/window/minimise"));
			assertEquals(204, post(uri, "/window/maximise"));
			assertEquals(204, post(uri, "/window/close"));
			assertEquals(204, post(uri, "/window/move/start"));
			assertEquals(204, post(uri, "/window/move/end"));
			assertEquals(List.of("minimise", "maximise", "close", "move/start", "move/end"), invoked);
		} finally {
			server.stop();
		}
	}

	@Example
	public void shouldNotInventCommandsItDoesNotHave() throws Exception {
		final UiServer server = new UiServer(Routes.create(new Templates(false), recorder));
		final URI uri = server.start();

		try {
			post(uri, "/window/selfdestruct");
			assertEquals(List.of(), invoked);
		} finally {
			server.stop();
		}
	}
}
