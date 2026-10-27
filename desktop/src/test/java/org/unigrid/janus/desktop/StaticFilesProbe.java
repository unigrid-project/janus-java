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

package org.unigrid.janus.desktop;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.function.Supplier;
import org.unigrid.oriel.server.AppId;
import org.unigrid.oriel.server.Routes;
import org.unigrid.oriel.server.SessionToken;
import org.unigrid.oriel.server.UiServer;
import org.unigrid.oriel.server.action.Actions;
import org.unigrid.oriel.server.action.View;
import org.unigrid.oriel.server.template.Templates;
import org.unigrid.oriel.server.window.WindowControl;

/**
 * Serves the interface from whatever Java runs it, asks for the files the pages are drawn with, and prints the
 * status and type of each answer. The installer tests run it on the runtime that is packaged with Janus, which
 * has only the JDK modules the build chose to include.
 */
public final class StaticFilesProbe {
	static final List<String> ASSETS = List.of(
		"/static/css/janus.css", "/oriel/htmx.min.js", "/static/fonts/sora-latin.woff2"
	);

	private StaticFilesProbe() {
	}

	public static void main(final String[] arguments) throws Exception {
		final Supplier<View> noPage = () -> {
			throw new IllegalStateException("The request reached the page instead of being served as a file");
		};
		final SessionToken token = SessionToken.random();
		final URI base = new UiServer(Routes.create(AppId.of("janus"), new Templates(false), token,
			WindowControl.NONE, Actions.of(), noPage)).start();
		final HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager())
			.followRedirects(HttpClient.Redirect.NORMAL).build();

		for (final String asset : ASSETS) {
			final URI address = base.resolve(asset + "?" + SessionToken.PARAMETER + "=" + token.value());
			final HttpResponse<Void> response = http.send(HttpRequest.newBuilder(address).build(),
				HttpResponse.BodyHandlers.discarding());

			System.out.println(asset + " " + response.statusCode() + " "
				+ response.headers().firstValue("content-type").orElse("-"));
		}

		System.exit(0);
	}
}
