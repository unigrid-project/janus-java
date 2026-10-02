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
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The script that a copy unpacked from the tar.gz uses to add itself to the application menu, run against a
 * folder laid out like the tar.gz, in a path that holds a space as the portable copy's does.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(value = 2, unit = TimeUnit.MINUTES)
public class InstallDesktopScriptIT {
	private static final Path PACKAGING = Path.of("src", "main", "packaging", "linux").toAbsolutePath();

	@TempDir
	private Path scratch;

	private Path unpacked;
	private Path home;
	private Path entry;

	@BeforeEach
	void unpack() throws IOException {
		unpacked = Files.createDirectories(scratch.resolve("with space").resolve("Unigrid"));
		home = Files.createDirectories(scratch.resolve("home"));
		entry = home.resolve(".local").resolve("share").resolve("applications").resolve("unigrid.desktop");

		Files.copy(PACKAGING.resolve("install-desktop.sh"), unpacked.resolve("install-desktop.sh"));
		unpacked.resolve("install-desktop.sh").toFile().setExecutable(true);
		Files.createDirectories(unpacked.resolve("lib"));
		Files.copy(PACKAGING.resolve("Unigrid.desktop"), unpacked.resolve("lib").resolve("Unigrid.desktop.in"),
			StandardCopyOption.REPLACE_EXISTING);
	}

	private int run(final String... arguments) throws IOException, InterruptedException {
		final String[] command = new String[arguments.length + 1];

		command[0] = unpacked.resolve("install-desktop.sh").toString();
		System.arraycopy(arguments, 0, command, 1, arguments.length);

		final ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);

		builder.environment().put("HOME", home.toString());
		builder.environment().remove("XDG_DATA_HOME");

		final Process process = builder.start();

		process.getInputStream().readAllBytes();
		return process.waitFor();
	}

	@Test
	public void shouldWriteAMenuEntryThatQuotesTheLauncherAndFillsInEveryPlaceholder() throws Exception {
		assertEquals(0, run());

		final String written = Files.readString(entry, StandardCharsets.UTF_8);

		assertTrue(written.contains("\nExec=\"" + unpacked + "/bin/Unigrid\"\n"), written);
		assertTrue(written.contains("\nIcon=" + unpacked + "/lib/Unigrid.png\n"), written);
		assertTrue(written.contains("\nCategories=Network;\n"), written);
		assertTrue(written.contains("\nStartupWMClass=org-unigrid-janus-shell-Janus\n"), written);
		assertFalse(written.contains("APPLICATION_"), written);
		assertFalse(written.contains("DESKTOP_MIMES"), written);
	}

	@Test
	public void shouldTakeTheMenuEntryOutAgain() throws Exception {
		assertEquals(0, run());
		assertTrue(Files.exists(entry));

		assertEquals(0, run("--uninstall"));
		assertFalse(Files.exists(entry));
	}

	@Test
	public void shouldRefuseWhatItDoesNotKnow() throws Exception {
		assertEquals(1, run("--bogus"));
		assertFalse(Files.exists(entry));
	}
}
