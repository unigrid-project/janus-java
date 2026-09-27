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

package org.unigrid.janus.core;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;

/**
 * The wallet Janus opens on at start, remembered as the backup copy it made rather than the original,
 * which may move or be spoiled after it was chosen.
 */
@ApplicationScoped
public class ChosenWallet {
	private static final String OWNER_ONLY = "rw-------";

	private final Path file;

	public ChosenWallet() {
		this(Path.of(System.getProperty("user.home"), ".janus", "wallet"));
	}

	public ChosenWallet(final Path file) {
		this.file = file;
	}

	public void remember(final Path backup) {
		try {
			Files.createDirectories(file.getParent());
			Files.deleteIfExists(file);
			Files.writeString(Files.createFile(file, ownerOnly()), backup.toAbsolutePath().toString(),
				StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING
			);
		} catch (IOException e) {
			throw new UncheckedIOException("The chosen wallet could not be remembered in " + file, e);
		}
	}

	/* A backup that has gone since is not worth opening on, and keeping its name would only fail again. */
	public Optional<Path> remembered() {
		try {
			if (!Files.isRegularFile(file)) {
				return Optional.empty();
			}

			final Path backup = Path.of(Files.readString(file, StandardCharsets.UTF_8).strip());

			if (Files.isRegularFile(backup)) {
				return Optional.of(backup);
			}

			forget();
			return Optional.empty();
		} catch (IOException e) {
			throw new UncheckedIOException("The chosen wallet could not be read from " + file, e);
		}
	}

	public void forget() {
		try {
			Files.deleteIfExists(file);
		} catch (IOException e) {
			throw new UncheckedIOException("The chosen wallet could not be forgotten in " + file, e);
		}
	}

	private FileAttribute<?>[] ownerOnly() {
		if (!file.getFileSystem().supportedFileAttributeViews().contains("posix")) {
			return new FileAttribute<?>[0];
		}

		return new FileAttribute<?>[] {PosixFilePermissions.asFileAttribute(
			PosixFilePermissions.fromString(OWNER_ONLY)
		)};
	}
}
