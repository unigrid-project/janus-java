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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import org.unigrid.janus.core.Release;
import org.unigrid.janus.core.ReleaseKey;
import org.unigrid.janus.core.SigningKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HedgehogInstallerTest {
	private static final byte[] EXECUTABLE = "#!/bin/sh\necho hedgehog\n".getBytes(StandardCharsets.US_ASCII);
	private static final String ASSET = "hedgehog-0.0.8-x86_64-linux-gnu.bin";

	private Path home;
	private ReleaseServer page;
	private SigningKey key;
	private final List<Integer> progress = new ArrayList<>();

	@BeforeTry
	public void publish() throws IOException {
		home = Files.createTempDirectory("installer");
		key = new SigningKey();
		page = new ReleaseServer().serve(ASSET, EXECUTABLE).serve(ASSET + ".asc", key.sign(EXECUTABLE));
		progress.clear();
	}

	@AfterTry
	public void clean() throws IOException {
		page.close();

		try (Stream<Path> paths = Files.walk(home)) {
			paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
		}
	}

	private HedgehogInstaller installer(final HedgehogRelease release, final SigningKey trusted) {
		return new HedgehogInstaller(release, page.uri(),
			(file, signature) -> ReleaseKey.verify(file, signature, trusted.ring())
		);
	}

	private Path install(final HedgehogRelease release) {
		return installer(release, key).install(progress::add);
	}

	@Example
	public void shouldInstallTheExecutableOfThePinnedRelease() throws IOException {
		final HedgehogRelease release = Releases.pinning(home, EXECUTABLE);
		final Path installed = install(release);

		assertEquals(home.resolve(".janus/hedgehog/0.0.8/hedgehog"), installed);
		assertEquals(new String(EXECUTABLE, StandardCharsets.US_ASCII), Files.readString(installed));
		assertTrue(Files.isExecutable(installed));
		assertTrue(release.matches(installed));
	}

	@Example
	public void shouldReportTheProgressOfTheTransfer() throws IOException {
		install(Releases.pinning(home, EXECUTABLE));

		assertNull(progress.getFirst(), "nothing is known before the transfer starts");
		assertEquals(100, progress.getLast());
	}

	@Example
	public void shouldReportNothingButUnknownWhenTheSizeIsNotSaid() throws IOException {
		page.withoutLength();
		install(Releases.pinning(home, EXECUTABLE));

		assertEquals(Collections.singletonList(null), progress);
	}

	@Example
	public void shouldRefuseAnExecutableThatIsNotThePinnedOne() throws IOException {
		final byte[] another = "another release".getBytes(StandardCharsets.US_ASCII);
		final HedgehogRelease release = Releases.pinning(home, another);
		final IllegalStateException failure = assertThrows(IllegalStateException.class, () -> install(release));

		assertEquals("The downloaded Hedgehog is not the release this Janus is made for", failure.getMessage());
		assertNothingKept();
	}

	@Example
	public void shouldRefuseAnExecutableThatTheFoundationDidNotSign() throws IOException {
		final HedgehogRelease release = Releases.pinning(home, EXECUTABLE);
		final HedgehogInstaller installer = installer(release, new SigningKey());
		final IllegalStateException failure = assertThrows(IllegalStateException.class,
			() -> installer.install(progress::add)
		);

		assertEquals("The downloaded Hedgehog is not signed by the Unigrid Foundation", failure.getMessage());
		assertNothingKept();
	}

	@Example
	public void shouldFailWhenTheReleaseHasNoSuchFile() throws IOException {
		try (ReleaseServer empty = new ReleaseServer()) {
			final HedgehogRelease release = Releases.pinning(home, EXECUTABLE);
			final HedgehogInstaller installer = new HedgehogInstaller(release, empty.uri(), (_, _) -> true);
			final IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> installer.install(progress::add)
			);

			assertTrue(failure.getMessage().startsWith("Hedgehog could not be downloaded: "),
				failure.getMessage());
		}
	}

	@Example
	public void shouldFailWhenTheBuildPinsNoChecksum() {
		final IllegalStateException failure = assertThrows(IllegalStateException.class,
			() -> install(Releases.unpinned(home))
		);

		assertEquals("This Janus knows no checksum of Hedgehog 0.0.8 for this platform", failure.getMessage());
	}

	@Example
	public void shouldFailWhenThereIsNoBuildForThePlatform() {
		final HedgehogRelease release = new HedgehogRelease(new Release(), "FreeBSD", "amd64", home);
		final IllegalStateException failure = assertThrows(IllegalStateException.class, () -> install(release));

		assertEquals("No Hedgehog build for this platform", failure.getMessage());
	}

	private void assertNothingKept() throws IOException {
		final Path folder = home.resolve(".janus/hedgehog/0.0.8");

		try (Stream<Path> kept = Files.list(folder)) {
			assertFalse(kept.findAny().isPresent(), "a refused download must not stay behind");
		}
	}
}
