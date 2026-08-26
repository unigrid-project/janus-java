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

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.unigrid.janus.web.action.Actions;

public final class Routes {
	private static final String ASSETS = "static/";
	private static final String ASSET_PATH = "/static";
	private static final String WINDOW_PATH = "/window";
	private static final String ACTION_PATH = "/action";

	private Routes() {
	}

	public static Handler create(final Templates templates, final SessionToken token) {
		return create(templates, token, WindowControl.NONE, Actions.of());
	}

	public static Handler create(final Templates templates, final SessionToken token,
		final WindowControl window, final Actions actions) {

		final ResourceHandler assets = new ResourceHandler();

		assets.setBaseResource(ResourceFactory.of(assets).newClassLoaderResource(ASSETS));
		assets.setDirAllowed(false);

		/* Anything the asset handler does not recognise falls through to the pages,
		   so an unknown path renders the interface rather than a Jetty error page. */
		return new Guard(new Handler.Sequence(
			new ContextHandler(assets, ASSET_PATH),
			new ContextHandler(new WindowHandler(window), WINDOW_PATH),
			new ContextHandler(new ActionHandler(actions, templates), ACTION_PATH),
			new UiHandler(templates)
		), token);
	}
}
