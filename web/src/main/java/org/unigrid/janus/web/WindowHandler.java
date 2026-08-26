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

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public class WindowHandler extends Handler.Abstract {
	private final WindowControl window;

	public WindowHandler(final WindowControl window) {
		this.window = window;
	}

	@Override
	public boolean handle(final Request request, final Response response, final Callback callback) {
		final String command = Request.getPathInContext(request).substring(1);

		switch (command) {
			case "minimise" -> window.minimise();
			case "maximise" -> window.toggleMaximise();
			case "close" -> window.close();
			case "move/start" -> window.beginMove();
			case "move/end" -> window.endMove();
			default -> {
				return false;
			}
		}

		response.setStatus(HttpStatus.NO_CONTENT_204);
		callback.succeeded();
		return true;
	}
}
