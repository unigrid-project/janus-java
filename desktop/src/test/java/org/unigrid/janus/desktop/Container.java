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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A throwaway container driven through the docker command line, which is all the installer tests need of
 * Docker. The folder given to {@link #start} is visible inside at /r, read only.
 */
final class Container implements AutoCloseable {
	record Result(int exit, String output) {
	}

	private static final long PULL_SECONDS = 600;
	private static final long PROBE_SECONDS = 30;

	private final String id;

	private Container(final String id) {
		this.id = id;
	}

	static boolean dockerAvailable() {
		try {
			return run(List.of("docker", "info"), PROBE_SECONDS).exit() == 0;
		} catch (IOException e) {
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	static Container start(final String image, final Path mount) throws IOException, InterruptedException {
		final Result started = run(List.of(
			"docker", "run", "-d", "--rm", "-v", mount + ":/r:ro", image, "sleep", "infinity"
		), PULL_SECONDS);

		if (started.exit() != 0) {
			throw new IOException("Could not start " + image + ": " + started.output());
		}

		return new Container(started.output().lines().reduce((first, last) -> last).orElseThrow().trim());
	}

	Result exec(final String user, final long timeoutSeconds, final String script)
		throws IOException, InterruptedException {

		return run(List.of("docker", "exec", "-u", user, id, "bash", "-ec", script), timeoutSeconds);
	}

	void copyIn(final Path file, final String target) throws IOException, InterruptedException {
		final Result copied = run(List.of("docker", "cp", file.toString(), id + ":" + target), PROBE_SECONDS);

		if (copied.exit() != 0) {
			throw new IOException("Could not copy " + file + ": " + copied.output());
		}
	}

	@Override
	public void close() throws IOException, InterruptedException {
		run(List.of("docker", "rm", "-f", id), PROBE_SECONDS);
	}

	private static Result run(final List<String> command, final long timeoutSeconds)
		throws IOException, InterruptedException {

		final Path output = Files.createTempFile("docker", ".log");

		try {
			final Process process = new ProcessBuilder(new ArrayList<>(command)).redirectErrorStream(true)
				.redirectOutput(output.toFile()).start();

			if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
				process.destroyForcibly();
				throw new IOException("Timed out after " + timeoutSeconds + " s: "
					+ String.join(" ", command));
			}

			return new Result(process.exitValue(), Files.readString(output, StandardCharsets.UTF_8));
		} finally {
			Files.deleteIfExists(output);
		}
	}
}
