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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HedgehogLocationTest {
	private Path root;
	private Path installation;
	private Path onPath;

	@BeforeTry
	public void makeFolders() throws IOException {
		root = Files.createTempDirectory("hedgehog");
		installation = Files.createDirectory(root.resolve("installation"));
		onPath = Files.createDirectory(root.resolve("bin"));
	}

	@AfterTry
	public void removeThem() throws IOException {
		try (Stream<Path> paths = Files.walk(root)) {
			paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
		}
	}

	private static Path executable(final Path folder, final String name) throws IOException {
		final Path file = Files.createFile(folder.resolve(name));

		file.toFile().setExecutable(true);
		return file;
	}

	private HedgehogLocation location(final String configured) {
		return new HedgehogLocation(configured, installation, "/nowhere" + File.pathSeparator + onPath, "Linux");
	}

	@Example
	public void shouldUseOnlyTheConfiguredExecutableWhenOneIsNamed() throws IOException {
		final Path configured = executable(root, "my-hedgehog");

		executable(installation, "hedgehog");
		assertEquals(Optional.of(configured), location(configured.toString()).find());
	}

	@Example
	public void shouldFindNothingWhenTheConfiguredExecutableIsMissing() throws IOException {
		executable(installation, "hedgehog");
		assertEquals(Optional.empty(), location(root.resolve("gone").toString()).find());
	}

	@Example
	public void shouldPreferTheOneBesideJanusOverThePath() throws IOException {
		final Path beside = executable(installation, "hedgehog");

		executable(onPath, "hedgehog");
		assertEquals(Optional.of(beside), location(null).find());
	}

	@Example
	public void shouldFallBackToThePath() throws IOException {
		final Path found = executable(onPath, "hedgehog");

		assertEquals(Optional.of(found), location(null).find());
	}

	@Example
	public void shouldPassOverAFileThatCannotBeRun() throws IOException {
		Files.createFile(installation.resolve("hedgehog"));
		assertEquals(Optional.empty(), location(null).find());
	}

	@Example
	public void shouldLookForTheWindowsName() throws IOException {
		final Path found = executable(onPath, "hedgehog.exe");

		assertEquals(Optional.of(found), new HedgehogLocation(null, null, onPath.toString(), "Windows 11").find());
	}
}
