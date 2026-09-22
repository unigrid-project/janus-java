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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Plays Hedgehog's command line for the service tests: {@code daemon --restport=N} serves the REST calls
 * the service makes, and {@code bootstrap fetch} puts a ledger in place. Files in the folder named by
 * FAKE_HEDGEHOG_HOME steer it and record what it was asked to do.
 */
public final class FakeHedgehog {
	static final String SIGNATURE = "signature";
	static final String LEDGER = "ledger";
	static final String FETCH_FAILS = "fetch-fails";
	static final String FETCH_HANGS = "fetch-hangs";
	static final String DIES = "dies-at-start";
	static final String STARTS = "starts";
	static final String FETCH_PID = "fetch-pid";
	static final String DAEMON_PID = "daemon-pid";
	static final String STOP_REFUSED = "stop-refused";

	private static final String PORT = "--restport=";

	private FakeHedgehog() {
	}

	public static void main(final String[] args) throws IOException, InterruptedException {
		final Path home = Path.of(System.getenv("FAKE_HEDGEHOG_HOME"));

		if ("daemon".equals(args[0])) {
			daemon(home, Integer.parseInt(args[1].substring(PORT.length())));
		} else {
			System.exit(fetch(home));
		}
	}

	/** A launcher that runs this class in a JVM of its own, the way the real executable runs. */
	static Path install(final Path home) throws IOException {
		return install(home, "exec ");
	}

	/** Like the released Hedgehog, whose launcher runs the real one as a child rather than becoming it. */
	static Path installAsLauncher(final Path home) throws IOException {
		return install(home, "");
	}

	private static Path install(final Path home, final String exec) throws IOException {
		final Path java = Path.of(System.getProperty("java.home"), "bin", "java");
		final Path script = home.resolve("hedgehog");

		Files.writeString(script, "#!/bin/sh\nFAKE_HEDGEHOG_HOME='" + home + "' " + exec + "'" + java + "' -cp '"
			+ System.getProperty("java.class.path") + "' " + FakeHedgehog.class.getName() + " \"$@\"\n"
		);
		script.toFile().setExecutable(true);
		return script;
	}

	static String snapshot(final String signature) {
		return "{\"tipHash\":\"ab\",\"tipHeight\":3172666,\"addressCount\":1,\"entryCount\":1,"
			+ "\"transactionCount\":1,\"totalUnspent\":1,\"zerocoinMinted\":0,"
			+ "\"built\":\"2026-09-14T10:15:30Z\",\"signature\":\"" + signature + "\"}";
	}

	private static int fetch(final Path home) throws IOException, InterruptedException {
		Files.writeString(home.resolve(FETCH_PID), Long.toString(ProcessHandle.current().pid()));

		if (Files.exists(home.resolve(FETCH_HANGS))) {
			Thread.sleep(60_000);
		}

		if (Files.exists(home.resolve(FETCH_FAILS))) {
			return 2;
		}

		Files.writeString(home.resolve(LEDGER), Files.readString(home.resolve(SIGNATURE)));
		return 0;
	}

	private static void daemon(final Path home, final int port) throws IOException {
		Files.writeString(home.resolve(STARTS), "started\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		Files.writeString(home.resolve(DAEMON_PID), Long.toString(ProcessHandle.current().pid()));

		if (Files.exists(home.resolve(DIES))) {
			System.exit(3);
		}

		final InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
		final HttpServer server = HttpServer.create(address, 0);

		server.createContext("/version",
			exchange -> answer(exchange, 202, "{\"version\":\"fake\",\"protocols\":[]}")
		);
		server.createContext("/stop", exchange -> {
			if (Files.exists(home.resolve(STOP_REFUSED))) {
				answer(exchange, 503, "");
				return;
			}

			try {
				answer(exchange, 202, "");
			} finally {
				System.exit(0);
			}
		});
		server.createContext("/bootstrap", exchange -> {
			final Path ledger = home.resolve(LEDGER);

			if (Files.exists(ledger)) {
				answer(exchange, 200, snapshot(Files.readString(ledger).trim()));
			} else {
				answer(exchange, 503, "");
			}
		});
		server.start();
	}

	private static void answer(final HttpExchange exchange, final int status, final String body) throws IOException {
		final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);

		try (OutputStream out = exchange.getResponseBody()) {
			if (bytes.length > 0) {
				out.write(bytes);
			}
		}
	}
}
