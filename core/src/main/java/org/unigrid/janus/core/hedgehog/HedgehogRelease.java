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

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import org.unigrid.janus.core.Release;

/**
 * The one Hedgehog release this Janus is made to run against, and the executable of it that fits this
 * computer. A Hedgehog is the release only when its bytes are exactly the ones pinned by the build.
 */
@ApplicationScoped
public class HedgehogRelease {
	static final String NO_BUILD = "No Hedgehog build for this platform";

	private static final String UNKNOWN = "unknown";
	private static final int CHUNK = 1 << 16;

	private final String version;
	private final Optional<Platform> platform;
	private final Optional<String> sha256;
	private final Path home;

	public HedgehogRelease() {
		this(new Release(), System.getProperty("os.name"), System.getProperty("os.arch"),
			Path.of(System.getProperty("user.home"))
		);
	}

	HedgehogRelease(final Release release, final String os, final String arch, final Path home) {
		this.version = release.hedgehogVersion().orElse(UNKNOWN);
		this.platform = Platform.of(os, arch);
		this.sha256 = platform.flatMap(known -> release.hedgehogSha256(known.key()));
		this.home = home;
	}

	HedgehogRelease(final String version, final Optional<Platform> platform, final Optional<String> sha256,
		final Path home) {

		this.version = version;
		this.platform = platform;
		this.sha256 = sha256;
		this.home = home;
	}

	public String version() {
		return version;
	}

	Platform platform() {
		return platform.orElseThrow(() -> new IllegalStateException(NO_BUILD));
	}

	/** The checksum this computer's executable must have, absent when the build pins none for it. */
	Optional<String> sha256() {
		return sha256;
	}

	/** Where an executable of this release that Janus downloaded is kept. */
	Path installation() {
		return home.resolve(".janus").resolve("hedgehog").resolve(version);
	}

	String asset() {
		return platform().asset(version);
	}

	URI url(final URI releases, final String name) {
		return URI.create(releases + "v" + version + "/" + name);
	}

	/** True only when the file is the release's executable, byte for byte. */
	boolean matches(final Path file) {
		try {
			return sha256.isPresent() && sha256.get().equals(sha256(file));
		} catch (IOException e) {
			return false;
		}
	}

	static String sha256(final Path file) throws IOException {
		final MessageDigest digest = digest();

		try (InputStream in = Files.newInputStream(file)) {
			final byte[] chunk = new byte[CHUNK];

			for (int read = in.read(chunk); read >= 0; read = in.read(chunk)) {
				digest.update(chunk, 0, read);
			}
		}

		return HexFormat.of().formatHex(digest.digest());
	}

	static MessageDigest digest() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("This Java platform offers no SHA-256", e);
		}
	}
}
