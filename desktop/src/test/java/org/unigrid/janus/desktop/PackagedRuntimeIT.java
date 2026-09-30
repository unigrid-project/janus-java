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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the interface on the Java runtime packaged with Janus, headless and without Docker. A runtime that
 * lacks a module the server needs shows up here as pages that are served without their files.
 */
public class PackagedRuntimeIT {
	private static final Path IMAGE = Path.of("target", "dist", "Unigrid").toAbsolutePath();
	private static final long SECONDS = 60;

	@Test
	public void shouldServeTheStylesheetAndScriptsFromThePackagedRuntime() throws Exception {
		final Path java = IMAGE.resolve("lib").resolve("runtime").resolve("bin").resolve("java");

		Assumptions.assumeTrue(Files.isExecutable(java), "No packaged runtime for this platform in " + IMAGE);

		final Path output = Files.createTempFile("probe", ".log");
		final String classpath = IMAGE.resolve("lib").resolve("app") + File.separator + "*" + File.pathSeparator
			+ Path.of("target", "test-classes").toAbsolutePath();

		try {
			final Process process = new ProcessBuilder(java.toString(), "-cp", classpath,
				StaticFilesProbe.class.getName())
				.redirectErrorStream(true)
				.redirectOutput(output.toFile())
				.start();

			assertTrue(process.waitFor(SECONDS, TimeUnit.SECONDS), "The probe did not finish");

			final String answers = Files.readString(output, StandardCharsets.UTF_8);

			assertEquals(0, process.exitValue(), answers);
			assertTrue(answers.contains("/static/css/janus.css 200 text/css"), answers);

			for (final String asset : StaticFilesProbe.ASSETS) {
				assertTrue(answers.contains(asset + " 200"), "Not served: " + asset + "\n" + answers);
			}
		} finally {
			Files.deleteIfExists(output);
		}
	}
}
