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

package org.unigrid.janus.shell;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unigrid.janus.web.Routes;
import org.unigrid.janus.web.SessionToken;
import org.unigrid.janus.web.Templates;
import org.unigrid.janus.web.UiServer;

public final class Janus {
	private static final Logger LOG = LoggerFactory.getLogger(Janus.class);

	private Janus() {
	}

	public static void main(final String[] args) throws Exception {
		final BrowserWindow window = new BrowserWindow();
		final SessionToken token = SessionToken.random();
		final UiServer server = new UiServer(Routes.create(new Templates(false), token, window.control()));
		final URI uri = server.start();

		LOG.info("Janus is serving its interface at {}", uri);

		/* The token rides on the first navigation only; the server swaps it for a cookie and
		   redirects, so it does not linger in the address. */
		window.open(uri.resolve("/?" + SessionToken.PARAMETER + "=" + token.value()));
	}
}
