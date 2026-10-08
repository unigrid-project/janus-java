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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Files and folders that hold or point at private keys, made so nobody but their owner may come near them: with
 * permissions where the file system keeps them, and with an access list naming the owner alone where it keeps
 * those instead, as on Windows.
 */
public final class OwnerOnly {
	private static final String FILE = "rw-------";
	private static final String FOLDER = "rwx------";

	private OwnerOnly() {
	}

	public static Path createFile(final Path file) throws IOException {
		return restricted(Files.createFile(file, permissions(file, FILE)), Set.of());
	}

	public static Path createTempFile(final Path folder, final String prefix, final String suffix)
		throws IOException {

		return restricted(Files.createTempFile(folder, prefix, suffix, permissions(folder, FILE)), Set.of());
	}

	/* Only the folder asked for is listed for its owner; the folders above it are left as they were. */
	public static Path createDirectories(final Path folder) throws IOException {
		final boolean made = Files.notExists(folder);

		Files.createDirectories(folder, permissions(folder, FOLDER));
		return made ? restricted(folder, Set.of(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT)) : folder;
	}

	private static FileAttribute<?>[] permissions(final Path path, final String permissions) {
		if (!path.getFileSystem().supportedFileAttributeViews().contains("posix")) {
			return new FileAttribute<?>[0];
		}

		return new FileAttribute<?>[] {
			PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(permissions))
		};
	}

	/* An access list given while creating is merged with the ones the folder above hands down, so the list is
	   set afterwards, before anything is written, which replaces it whole. The owner is taken from the file
	   itself: the user name the JVM reports does not always resolve to the account that made it on Windows. */
	private static Path restricted(final Path path, final Set<AclEntryFlag> handedDown) throws IOException {
		final AclFileAttributeView view = Files.getFileAttributeView(path, AclFileAttributeView.class);

		if (view == null || path.getFileSystem().supportedFileAttributeViews().contains("posix")) {
			return path;
		}

		view.setAcl(List.of(AclEntry.newBuilder().setType(AclEntryType.ALLOW)
			.setPrincipal(Files.getOwner(path))
			.setPermissions(EnumSet.allOf(AclEntryPermission.class)).setFlags(handedDown).build()
		));

		return path;
	}
}
