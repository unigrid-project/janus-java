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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Installs the package the build made for this computer the way a person would, and says where its launcher is.
 * Installing changes the computer for good, so only the installed-wallet test on a CI runner uses it.
 */
final class Installation {
	static final boolean WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
	static final boolean MAC = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac");

	private static final long INSTALL_SECONDS = 600;

	private Installation() {
	}

	static Path install(final Path dist, final Path scratch, final Path logs)
		throws IOException, InterruptedException {

		final Path launcher;

		if (WINDOWS) {
			run("msiexec", "/i", one(dist, ".msi").toString(), "/qn", "/l*v",
				logs.resolve("msiexec.log").toString());
			launcher = Path.of(System.getenv("LOCALAPPDATA"), "Unigrid", "Unigrid.exe");
		} else if (MAC) {
			final Path mount = Files.createDirectories(scratch.resolve("mount"));

			run("hdiutil", "attach", "-nobrowse", "-mountpoint", mount.toString(), one(dist, ".dmg").toString());

			try {
				run("cp", "-R", mount.resolve("Unigrid.app").toString(), scratch.toString());
			} finally {
				run("hdiutil", "detach", mount.toString());
			}

			launcher = scratch.resolve("Unigrid.app").resolve("Contents").resolve("MacOS").resolve("Unigrid");
		} else {
			run("sudo", "apt-get", "install", "-y", one(dist, ".deb").toString());
			launcher = Path.of("/opt/unigrid/bin/Unigrid");
		}

		if (!Files.isExecutable(launcher)) {
			throw new IOException("The installation left no launcher at " + launcher);
		}

		return launcher;
	}

	/** Takes an installed package off the computer again; a copy made from the disk image has nothing to remove. */
	static void uninstall(final Path dist, final Path logs) throws IOException, InterruptedException {
		if (WINDOWS) {
			run("msiexec", "/x", one(dist, ".msi").toString(), "/qn", "/l*v",
				logs.resolve("msiexec-remove.log").toString());
		} else if (!MAC) {
			run("sudo", "apt-get", "remove", "-y", "unigrid");
		}
	}

	private static Path one(final Path dist, final String suffix) throws IOException {
		try (Stream<Path> files = Files.list(dist)) {
			final List<Path> found = files.filter(file -> file.getFileName().toString().endsWith(suffix))
				.toList();

			if (found.size() != 1) {
				throw new IOException("Expected one " + suffix + " in " + dist + ", found " + found);
			}

			return found.getFirst();
		}
	}

	private static void run(final String... command) throws IOException, InterruptedException {
		final Process process = new ProcessBuilder(command).inheritIO().start();

		if (!process.waitFor(INSTALL_SECONDS, TimeUnit.SECONDS)) {
			process.destroyForcibly();
			throw new IOException("Timed out: " + String.join(" ", command));
		}

		if (process.exitValue() != 0) {
			throw new IOException("Exit " + process.exitValue() + ": " + String.join(" ", command));
		}
	}
}
