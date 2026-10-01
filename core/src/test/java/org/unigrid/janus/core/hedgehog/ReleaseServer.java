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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/** The release page of Hedgehog, serving whatever files a test puts on it. */
final class ReleaseServer implements AutoCloseable {
	private final HttpServer server;
	private final Map<String, byte[]> files = new ConcurrentHashMap<>();
	private volatile boolean withoutLength;
	private volatile CountDownLatch held = new CountDownLatch(0);

	ReleaseServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/", this::handle);
		server.start();
	}

	URI uri() {
		return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
	}

	/** Puts a file of the pinned release on the page, as the release of that version would carry it. */
	ReleaseServer serve(final String name, final byte[] contents) {
		files.put("/v" + Releases.VERSION + "/" + name, contents);
		return this;
	}

	/** Makes the page send its files without saying how large they are. */
	ReleaseServer withoutLength() {
		withoutLength = true;
		return this;
	}

	/** Keeps every answer back until the latch is counted down, so a test can look at a download under way. */
	ReleaseServer holdingBack(final CountDownLatch latch) {
		held = latch;
		return this;
	}

	private void handle(final HttpExchange exchange) throws IOException {
		final byte[] contents = files.get(exchange.getRequestURI().getPath());

		try {
			held.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		try (exchange; OutputStream out = exchange.getResponseBody()) {
			if (contents == null) {
				exchange.sendResponseHeaders(404, -1);
				return;
			}

			exchange.sendResponseHeaders(200, withoutLength ? 0 : contents.length);
			out.write(contents);
		}
	}

	@Override
	public void close() {
		server.stop(0);
	}
}
