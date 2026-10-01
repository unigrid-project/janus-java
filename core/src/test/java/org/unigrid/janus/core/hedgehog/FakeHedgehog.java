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
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

/**
 * Plays Hedgehog's command line for the service tests: {@code daemon --restport=N} serves the REST calls
 * the service makes. Files in the folder named by FAKE_HEDGEHOG_HOME steer it and record what it was asked
 * to do: a ledger appears when the test writes one, and while the test keeps a downloading file the daemon
 * says it is downloading, with the progress the file holds.
 */
public final class FakeHedgehog {
	static final String LEDGER = "ledger";
	static final String DOWNLOADING = "downloading";
	static final String DIES = "dies-at-start";
	static final String STARTS = "starts";
	static final String DAEMON_PID = "daemon-pid";
	static final String STOP_REFUSED = "stop-refused";
	private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");

	private static final String PORT = "--restport=";
	private static final String PROGRAM = """
		using System.Diagnostics;

		class Hedgehog {
			static int Main(string[] args) {
				var start = new ProcessStartInfo(@"%s", @"-cp ""%s"" %s " + string.Join(" ", args));

				start.UseShellExecute = false;
				start.EnvironmentVariables["FAKE_HEDGEHOG_HOME"] = @"%s";

				using (var java = Process.Start(start)) {
					java.WaitForExit();
					return java.ExitCode;
				}
			}
		}
		""";
	private static final String TOKEN_VARIABLE = "HEDGEHOG_REST_TOKEN";

	private FakeHedgehog() {
	}

	public static void main(final String[] args) throws IOException, InterruptedException {
		final Path home = Path.of(System.getenv("FAKE_HEDGEHOG_HOME"));

		daemon(home, Integer.parseInt(args[1].substring(PORT.length())));
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
		final Path launcher = Files.write(home.resolve(WINDOWS ? "hedgehog.exe" : "hedgehog"),
			executable(home, exec)
		);

		launcher.toFile().setExecutable(true);
		return launcher;
	}

	/** What the launcher is made of, for a test that has the service download it instead. */
	static byte[] executable(final Path home) throws IOException {
		return executable(home, "exec ");
	}

	/* Windows runs no shell scripts, so there the launcher is a small program that starts this class as a child
	   of its own, which also makes every launcher there one that runs the real Hedgehog rather than becoming it. */
	private static byte[] executable(final Path home, final String exec) throws IOException {
		final String classpath = System.getProperty("java.class.path");

		if (WINDOWS) {
			final Path java = Path.of(System.getProperty("java.home"), "bin", "java.exe");

			return compiled(PROGRAM.formatted(java, classpath, FakeHedgehog.class.getName(), home));
		}

		return ("#!/bin/sh\nFAKE_HEDGEHOG_HOME='" + home + "' " + exec + "'"
			+ Path.of(System.getProperty("java.home"), "bin", "java") + "' -cp '" + classpath + "' "
			+ FakeHedgehog.class.getName() + " \"$@\"\n").getBytes(StandardCharsets.UTF_8);
	}

	/* Compiled with the C# compiler that comes with every Windows. */
	private static byte[] compiled(final String source) throws IOException {
		final Path code = Files.writeString(Files.createTempFile("hedgehog", ".cs"), source);
		final Path program = code.resolveSibling(code.getFileName() + ".exe");
		final Path compiler = Path.of(System.getenv("WINDIR"), "Microsoft.NET", "Framework64", "v4.0.30319",
			"csc.exe");

		try {
			final Process compiling = new ProcessBuilder(compiler.toString(), "/nologo", "/out:" + program,
				code.toString()).redirectErrorStream(true).start();
			final String said = new String(compiling.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

			if (compiling.waitFor() != 0) {
				throw new IOException("The stand-in for Hedgehog could not be compiled:\n" + said);
			}

			return Files.readAllBytes(program);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while compiling the stand-in for Hedgehog", e);
		} finally {
			Files.deleteIfExists(code);
			Files.deleteIfExists(program);
		}
	}

	static String snapshot(final String signature) {
		return "{\"tipHash\":\"ab\",\"tipHeight\":3172666,\"addressCount\":1,\"entryCount\":1,"
			+ "\"transactionCount\":1,\"totalUnspent\":1,\"zerocoinMinted\":0,"
			+ "\"built\":\"2026-09-14T10:15:30Z\",\"signature\":\"" + signature + "\"}";
	}

	private static void daemon(final Path home, final int port) throws IOException {
		Files.writeString(home.resolve(STARTS), "started\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		Files.writeString(home.resolve(DAEMON_PID), Long.toString(ProcessHandle.current().pid()));

		if (Files.exists(home.resolve(DIES))) {
			System.exit(3);
		}

		final InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
		final HttpServer server = HttpServer.create(address, 0);

		server.createContext("/version", withToken(
			exchange -> answer(exchange, 202, "{\"version\":\"fake\",\"protocols\":[]}")
		));
		server.createContext("/status", withToken(exchange -> answer(exchange, 200, status(home))));
		server.createContext("/stop", withToken(exchange -> {
			if (Files.exists(home.resolve(STOP_REFUSED))) {
				answer(exchange, 503, "");
				return;
			}

			try {
				answer(exchange, 202, "");
			} finally {
				System.exit(0);
			}
		}));
		server.createContext("/bootstrap", withToken(exchange -> {
			final Path ledger = home.resolve(LEDGER);

			if (Files.exists(ledger)) {
				answer(exchange, 200, snapshot(Files.readString(ledger).trim()));
			} else {
				answer(exchange, 503, "");
			}
		}));
		server.start();
	}

	/* Like Hedgehog, it answers only a caller that presents the token it was started with, and none without one. */
	private static HttpHandler withToken(final HttpHandler handler) {
		final String token = System.getenv(TOKEN_VARIABLE);
		final String credentials = "Bearer " + token;

		return exchange -> {
			if (token != null && credentials.equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
				handler.handle(exchange);
			} else {
				answer(exchange, 401, "");
			}
		};
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

	private static String status(final Path home) throws IOException {
		final Path downloading = home.resolve(DOWNLOADING);

		if (!Files.exists(downloading)) {
			return "{\"status\":\"running\",\"progress\":100}";
		}

		final String progress = Files.readString(downloading).trim();

		return "{\"status\":\"downloading\",\"progress\":" + (progress.isEmpty() ? "null" : progress) + "}";
	}
}
