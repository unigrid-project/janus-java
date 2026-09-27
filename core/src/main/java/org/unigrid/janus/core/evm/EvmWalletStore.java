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

package org.unigrid.janus.core.evm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;

/** Where Janus keeps the EVM wallets it made or restored, one file each, named after its first address. */
@ApplicationScoped
public class EvmWalletStore {
	public static final String EXTENSION = ".json";

	private static final String OWNER_ONLY_FOLDER = "rwx------";
	private static final String OWNER_ONLY_FILE = "rw-------";
	private static final Jsonb JSON = JsonbBuilder.create();

	private final Path folder;

	public EvmWalletStore() {
		this(Path.of(System.getProperty("user.home"), ".janus", "wallets"));
	}

	public EvmWalletStore(final Path folder) {
		this.folder = folder;
	}

	/*
	 * Restoring a phrase that is already here replaces its file, so the password last given is the one
	 * that opens it. The file is written aside and moved in, so an interrupted save never leaves half a
	 * wallet under the name.
	 */
	public Path save(final EvmWallet wallet) {
		final Path target = folder.resolve("evm-" + wallet.addresses().get(0) + EXTENSION);

		try {
			Files.createDirectories(folder, ownerOnly(OWNER_ONLY_FOLDER));

			final Path written = Files.createTempFile(folder, "evm-", ".part", ownerOnly(OWNER_ONLY_FILE));

			Files.writeString(written, JSON.toJson(wallet), StandardCharsets.UTF_8);
			return Files.move(written, target, StandardCopyOption.REPLACE_EXISTING,
				StandardCopyOption.ATOMIC_MOVE
			);
		} catch (IOException e) {
			throw new UncheckedIOException("The wallet could not be saved to " + target, e);
		}
	}

	public static EvmWallet read(final Path file) {
		try {
			return JSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), EvmWallet.class);
		} catch (IOException e) {
			throw new UncheckedIOException("The wallet at " + file + " could not be read", e);
		} catch (JsonbException e) {
			throw new IllegalArgumentException(file + " is not a wallet Janus can read", e);
		}
	}

	public static boolean holds(final Path file) {
		return file.getFileName().toString().endsWith(EXTENSION);
	}

	private FileAttribute<?>[] ownerOnly(final String permissions) {
		if (!folder.getFileSystem().supportedFileAttributeViews().contains("posix")) {
			return new FileAttribute<?>[0];
		}

		return new FileAttribute<?>[] {PosixFilePermissions.asFileAttribute(
			PosixFilePermissions.fromString(permissions)
		)};
	}
}
