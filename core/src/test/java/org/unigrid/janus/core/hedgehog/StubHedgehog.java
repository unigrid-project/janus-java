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
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/** A stand-in for Hedgehog's REST server that answers each path the way a test tells it to. */
class StubHedgehog implements AutoCloseable {
	private static final String THROWAWAY = "throwaway";
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

	/** A stand-in behind a self-signed certificate made for it alone, the way Hedgehog makes one each start. */
	static StubHedgehog secure() throws IOException, GeneralSecurityException, InterruptedException {
		final Path store = Files.createTempFile("hedgehog", ".p12");

		Files.delete(store);

		final String keytoolPath = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
		final Process keytool = new ProcessBuilder(keytoolPath,
			"-genkeypair", "-alias", "hedgehog", "-keyalg", "RSA", "-keysize", "2048", "-dname", "CN=localhost",
			"-validity", "1", "-storetype", "PKCS12", "-keystore", store.toString(), "-storepass", THROWAWAY,
			"-keypass", THROWAWAY
		).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();

		if (keytool.waitFor() != 0) {
			throw new IllegalStateException("keytool could not make a certificate");
		}

		final KeyStore keys = KeyStore.getInstance("PKCS12");

		try (InputStream in = Files.newInputStream(store)) {
			keys.load(in, THROWAWAY.toCharArray());
		} finally {
			Files.delete(store);
		}

		final KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		final SSLContext context = SSLContext.getInstance("TLS");
		final HttpsServer server = HttpsServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);

		factory.init(keys, THROWAWAY.toCharArray());
		context.init(factory.getKeyManagers(), null, null);
		server.setHttpsConfigurator(new HttpsConfigurator(context));
		return new StubHedgehog(server, "https");
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
