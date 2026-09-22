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
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Copies of the wallets the user has chosen, kept by Janus in case the daemon spoils the original. */
@ApplicationScoped
public class WalletBackup {
	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
	private static final String OWNER_ONLY_FOLDER = "rwx------";
	private static final String OWNER_ONLY_FILE = "rw-------";

	private final Path folder;
	private final Clock clock;

	public WalletBackup() {
		this(Path.of(System.getProperty("user.home"), ".janus", "backups"), Clock.systemDefaultZone());
	}

	public WalletBackup(final Path folder, final Clock clock) {
		this.folder = folder;
		this.clock = clock;
	}

	/** Copies the wallet under a name of its own and answers where the copy is. */
	public Path backup(final Path wallet) {
		try {
			Files.createDirectories(folder, ownerOnly(OWNER_ONLY_FOLDER));
			return copy(wallet, claimName());
		} catch (IOException e) {
			throw new UncheckedIOException("The wallet at " + wallet + " could not be backed up", e);
		}
	}

	/* Creating the file is what reserves its name, so a copy made in the same second can never
	   take the place of an earlier one. */
	private Path claimName() throws IOException {
		final String stamp = LocalDateTime.now(clock).format(STAMP);

		for (int attempt = 1;; attempt++) {
			final String suffix = attempt == 1 ? "" : "-" + attempt;

			try {
				return Files.createFile(folder.resolve("wallet-" + stamp + suffix + ".dat"),
					ownerOnly(OWNER_ONLY_FILE)
				);
			} catch (FileAlreadyExistsException e) {
				continue;
			}
		}
	}

	private static Path copy(final Path wallet, final Path target) throws IOException {
		try (OutputStream out = Files.newOutputStream(target)) {
			Files.copy(wallet, out);
			return target;
		} catch (IOException e) {
			Files.delete(target);
			throw e;
		}
	}

	/* A wallet holds private keys, so where permissions can be set, nobody else may read them. */
	private FileAttribute<?>[] ownerOnly(final String permissions) {
		if (!folder.getFileSystem().supportedFileAttributeViews().contains("posix")) {
			return new FileAttribute<?>[0];
		}

		return new FileAttribute<?>[] {PosixFilePermissions.asFileAttribute(
			PosixFilePermissions.fromString(permissions)
		)};
	}
}
