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

import java.util.concurrent.atomic.AtomicInteger;
import net.jqwik.api.Example;
import org.eclipse.jetty.server.Handler;
import org.unigrid.janus.web.action.Actions;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiHandlerTest extends ServedTest {
	private final AtomicInteger visits = new AtomicInteger();

	@Override
	protected Handler routes() {
		return Routes.create(templates(), token(), WindowControl.NONE, Actions.of(),
			() -> new Page("Visit " + visits.incrementAndGet())
		);
	}

	/* What the page opens on can change while Janus runs, such as once a wallet has been chosen. */
	@Example
	public void shouldWorkOutThePageAgainForEveryRequest() throws Exception {
		final Client client = admitted();

		assertTrue(client.get("/").body().contains("Visit 2"));
		assertTrue(client.get("/").body().contains("Visit 3"));
	}
}
