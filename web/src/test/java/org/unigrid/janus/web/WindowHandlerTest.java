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

import java.util.ArrayList;
import java.util.List;
import net.jqwik.api.Example;
import org.eclipse.jetty.server.Handler;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WindowHandlerTest extends ServedTest {
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

	@Override
	protected Handler routes() {
		return Routes.create(templates(), token(), recorder);
	}

	@Example
	public void shouldForwardEachCommandToTheHost() throws Exception {
		final Client client = admitted();

		assertEquals(204, client.post("/window/minimise").statusCode());
		assertEquals(204, client.post("/window/maximise").statusCode());
		assertEquals(204, client.post("/window/close").statusCode());
		assertEquals(204, client.post("/window/move/start").statusCode());
		assertEquals(204, client.post("/window/move/end").statusCode());
		assertEquals(List.of("minimise", "maximise", "close", "move/start", "move/end"), invoked);
	}

	@Example
	public void shouldNotInventCommandsItDoesNotHave() throws Exception {
		admitted().post("/window/selfdestruct");
		assertEquals(List.of(), invoked);
	}

	@Example
	public void shouldNotActForAnUnknownCaller() throws Exception {
		assertEquals(403, anonymous().post("/window/close").statusCode());
		assertEquals(List.of(), invoked);
	}
}
