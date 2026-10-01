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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HedgehogLocationTest {
	private static final byte[] RELEASE = "the pinned release".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] STALE = "an older release".getBytes(StandardCharsets.US_ASCII);

	private Path root;
	private Path installation;
	private Path onPath;
	private Path downloads;

	@BeforeTry
	public void makeFolders() throws IOException {
		root = Files.createTempDirectory("hedgehog");
		installation = Files.createDirectory(root.resolve("installation"));
		onPath = Files.createDirectory(root.resolve("bin"));
		downloads = Files.createDirectories(root.resolve(".janus/hedgehog/0.0.8"));
	}

	@AfterTry
	public void removeThem() throws IOException {
		try (Stream<Path> paths = Files.walk(root)) {
			paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
		}
	}

	private static Path executable(final Path folder, final String name) throws IOException {
		return executable(folder, name, RELEASE);
	}

	private static Path executable(final Path folder, final String name, final byte[] contents) throws IOException {
		final Path file = Files.write(folder.resolve(name), contents);

		file.toFile().setExecutable(true);
		return file;
	}

	/* Where a file system keeps no permissions, as on Windows, a file may be run unless its access list says not. */
	private static void withoutRunning(final Path file) throws IOException {
		if (file.getFileSystem().supportedFileAttributeViews().contains("posix")) {
			Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
			return;
		}

		final Set<AclEntryPermission> allButRunning = EnumSet.allOf(AclEntryPermission.class);

		allButRunning.remove(AclEntryPermission.EXECUTE);
		Files.getFileAttributeView(file, AclFileAttributeView.class).setAcl(List.of(AclEntry.newBuilder()
			.setType(AclEntryType.ALLOW).setPrincipal(Files.getOwner(file)).setPermissions(allButRunning).build()
		));
	}

	private HedgehogRelease release() throws IOException {
		return Releases.pinning(root, RELEASE);
	}

	private HedgehogLocation location(final String configured) throws IOException {
		return new HedgehogLocation(configured, installation, "/nowhere" + File.pathSeparator + onPath, "Linux",
			release()
		);
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
		withoutRunning(Files.write(installation.resolve("hedgehog"), RELEASE));
		assertEquals(Optional.empty(), location(null).find());
	}

	@Example
	public void shouldLookForTheWindowsName() throws IOException {
		final Path found = executable(onPath, "hedgehog.exe");

		assertEquals(Optional.of(found), new HedgehogLocation(null, null, onPath.toString(), "Windows 11",
			release()).find());
	}

	@Example
	public void shouldPassOverAnExecutableThatIsNotThePinnedRelease() throws IOException {
		executable(installation, "hedgehog", STALE);
		executable(onPath, "hedgehog", STALE);
		assertEquals(Optional.empty(), location(null).find());
	}

	@Example
	public void shouldSkipAStaleOneBesideJanusForThePinnedOneOnThePath() throws IOException {
		executable(installation, "hedgehog", STALE);

		final Path found = executable(onPath, "hedgehog");

		assertEquals(Optional.of(found), location(null).find());
	}

	@Example
	public void shouldFindTheOneJanusDownloadedWhenNothingElseIsTheRelease() throws IOException {
		executable(onPath, "hedgehog", STALE);

		final Path downloaded = executable(downloads, "hedgehog");

		assertEquals(Optional.of(downloaded), location(null).find());
	}

	@Example
	public void shouldTakeAConfiguredExecutableWithoutAskingWhatItIs() throws IOException {
		final Path configured = executable(root, "my-hedgehog", STALE);

		assertEquals(Optional.of(configured), location(configured.toString()).find());
	}

	@Example
	public void shouldTrustNoExecutableWhenTheBuildPinsNone() throws IOException {
		executable(onPath, "hedgehog");

		final HedgehogLocation unpinned = new HedgehogLocation(null, null, onPath.toString(), "Linux",
			Releases.unpinned(root)
		);

		assertEquals(Optional.empty(), unpinned.find());
	}
}
