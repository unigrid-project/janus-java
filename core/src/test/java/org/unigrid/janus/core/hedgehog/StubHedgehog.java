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

package org.unigrid.janus.core.hedgehog;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** A stand-in for Hedgehog's REST server that answers each path the way a test tells it to. */
class StubHedgehog implements AutoCloseable {
	private static final Answer NOT_FOUND = new Answer(404, "", Duration.ZERO);

	private record Answer(int status, String body, Duration delay) {
	}

	private final HttpServer server;
	private final String scheme;
	private final Map<String, Answer> answers = new ConcurrentHashMap<>();
	private final List<URI> requests = new CopyOnWriteArrayList<>();

	StubHedgehog() throws IOException {
		this(HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0), "http");
	}

	StubHedgehog(final HttpServer server, final String scheme) {
		this.server = server;
		this.scheme = scheme;
		server.createContext("/", this::handle);
		server.start();
	}

	void answer(final String rawPath, final int status, final String body) {
		answers.put(rawPath, new Answer(status, body, Duration.ZERO));
	}

	void stall(final String rawPath, final Duration delay) {
		answers.put(rawPath, new Answer(200, "{}", delay));
	}

	List<URI> requests() {
		return requests;
	}

	URI uri() {
		return URI.create(scheme + "://127.0.0.1:" + server.getAddress().getPort());
	}

	private void handle(final HttpExchange exchange) throws IOException {
		requests.add(exchange.getRequestURI());

		final Answer answer = answers.getOrDefault(exchange.getRequestURI().getRawPath(), NOT_FOUND);
		final byte[] body = answer.body().getBytes(StandardCharsets.UTF_8);

		try {
			Thread.sleep(answer.delay().toMillis());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.sendResponseHeaders(answer.status(), body.length == 0 ? -1 : body.length);

		try (OutputStream out = exchange.getResponseBody()) {
			out.write(body);
		}
	}

	@Override
	public void close() {
		server.stop(0);
	}
}
