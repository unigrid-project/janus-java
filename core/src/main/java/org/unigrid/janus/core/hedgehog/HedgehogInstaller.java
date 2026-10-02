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
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.OptionalLong;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.unigrid.janus.core.ReleaseKey;

/**
 * Downloads the Hedgehog release Janus is made for. What arrives is used only when it is exactly the
 * executable the build pins and is also signed by the Unigrid Foundation: the pin says which release it
 * is, the signature says who made it.
 */
@Slf4j
@ApplicationScoped
public class HedgehogInstaller {
	public static final URI RELEASES = URI.create("https://github.com/unigrid-project/hedgehog/releases/download/");

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
	private static final String SIGNATURE = ".asc";
	private static final int CHUNK = 1 << 16;
	private static final int OK = 200;

	private final HedgehogRelease release;
	private final URI releases;
	private final BiPredicate<Path, byte[]> signed;

	@Inject
	public HedgehogInstaller(final HedgehogRelease release) {
		this(release, RELEASES, ReleaseKey::verify);
	}

	HedgehogInstaller(final HedgehogRelease release, final URI releases, final BiPredicate<Path, byte[]> signed) {
		this.release = release;
		this.releases = releases;
		this.signed = signed;
	}

	/**
	 * Puts the executable in place and returns it. The progress hears each new percentage of the
	 * transfer, and null while its size is unknown.
	 */
	public Path install(final Consumer<Integer> progress) {
		final Platform platform = release.platform();
		final String pinned = release.sha256().orElseThrow(() -> new IllegalStateException(
			"This Janus knows no checksum of Hedgehog " + release.version() + " for this platform"
		));
		final Path target = release.installation().resolve(platform.executable());
		final Path partial = target.resolveSibling(platform.executable() + ".part");

		try {
			Files.createDirectories(target.getParent());

			try (HttpClient http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT)
				.followRedirects(HttpClient.Redirect.NORMAL).build()) {

				final byte[] signature = signature(http, release.url(releases, release.asset() + SIGNATURE));

				final URI asset = release.url(releases, release.asset());

				if (!pinned.equals(download(http, asset, partial, progress))) {
					throw new IllegalStateException(
						"The downloaded Hedgehog is not the release this Janus is made for");
				}

				if (!signed.test(partial, signature)) {
					throw new IllegalStateException(
						"The downloaded Hedgehog is not signed by the Unigrid Foundation");
				}
			}

			partial.toFile().setExecutable(true);
			Files.move(partial, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			return target;
		} catch (IOException e) {
			throw new IllegalStateException("Hedgehog could not be downloaded: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Janus is shutting down", e);
		} finally {
			delete(partial);
		}
	}

	private static byte[] signature(final HttpClient http, final URI source) throws IOException, InterruptedException {
		final HttpResponse<byte[]> response = http.send(request(source), HttpResponse.BodyHandlers.ofByteArray());

		if (response.statusCode() != OK) {
			throw new IOException(source + " answered " + response.statusCode());
		}

		return response.body();
	}

	/* Returns the checksum of what was written, worked out on the way so the file is not read again. */
	private static String download(final HttpClient http, final URI source, final Path partial,
		final Consumer<Integer> progress) throws IOException, InterruptedException {

		final HttpResponse<InputStream> response = http.send(request(source),
			HttpResponse.BodyHandlers.ofInputStream()
		);

		try (InputStream body = response.body()) {
			if (response.statusCode() != OK) {
				throw new IOException(source + " answered " + response.statusCode());
			}

			final OptionalLong length = response.headers().firstValueAsLong("content-length");
			final MessageDigest digest = HedgehogRelease.digest();

			progress.accept(null);
			copy(new DigestInputStream(body, digest), partial, length, progress);
			return HexFormat.of().formatHex(digest.digest());
		}
	}

	private static void copy(final InputStream in, final Path partial, final OptionalLong length,
		final Consumer<Integer> progress) throws IOException {

		final byte[] chunk = new byte[CHUNK];
		long copied = 0;
		int reported = -1;

		try (OutputStream out = Files.newOutputStream(partial, StandardOpenOption.CREATE,
			StandardOpenOption.TRUNCATE_EXISTING)) {

			for (int read = in.read(chunk); read >= 0; read = in.read(chunk)) {
				out.write(chunk, 0, read);
				copied += read;

				if (length.isPresent() && length.getAsLong() > 0) {
					final int percent = (int) Math.min(100, copied * 100 / length.getAsLong());

					if (percent != reported) {
						reported = percent;
						progress.accept(percent);
					}
				}
			}
		}
	}

	private static HttpRequest request(final URI source) {
		return HttpRequest.newBuilder(source).timeout(REQUEST_TIMEOUT).GET().build();
	}

	private static void delete(final Path partial) {
		try {
			Files.deleteIfExists(partial);
		} catch (IOException e) {
			log.debug("Could not remove {}", partial, e);
		}
	}
}
