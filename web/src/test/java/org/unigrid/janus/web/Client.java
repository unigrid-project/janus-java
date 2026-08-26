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

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;

/** A caller of the interface, keeping its own cookies so that admitted and unknown callers differ. */
public class Client {
	private static final String FORM = "application/x-www-form-urlencoded";

	private final HttpClient http;
	private final URI base;
	private final Map<String, String> headers;

	public Client(final URI base) {
		this(HttpClient.newBuilder().cookieHandler(new CookieManager())
			.followRedirects(HttpClient.Redirect.NORMAL).build(), base, Map.of()
		);
	}

	private Client(final HttpClient http, final URI base, final Map<String, String> headers) {
		this.http = http;
		this.base = base;
		this.headers = headers;
	}

	public Client header(final String name, final String value) {
		final Map<String, String> combined = new LinkedHashMap<>(headers);

		combined.put(name, value);
		return new Client(http, base, combined);
	}

	public HttpResponse<String> get(final String path) throws Exception {
		return send(request(path).GET());
	}

	public HttpResponse<String> post(final String path) throws Exception {
		return send(request(path).POST(HttpRequest.BodyPublishers.noBody()));
	}

	public HttpResponse<String> submit(final String path, final String form) throws Exception {
		return send(request(path).header("Content-Type", FORM).POST(HttpRequest.BodyPublishers.ofString(form)));
	}

	private HttpRequest.Builder request(final String path) {
		final HttpRequest.Builder builder = HttpRequest.newBuilder(base.resolve(path));

		headers.forEach(builder::header);
		return builder;
	}

	private HttpResponse<String> send(final HttpRequest.Builder builder) throws Exception {
		return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}
}
