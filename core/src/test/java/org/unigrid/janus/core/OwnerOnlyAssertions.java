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
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks that nobody but its owner may come near a file or folder, in the terms its file system has for that. */
public final class OwnerOnlyAssertions {
	private OwnerOnlyAssertions() {
	}

	public static void assertOwnerOnly(final Path path) throws IOException {
		if (path.getFileSystem().supportedFileAttributeViews().contains("posix")) {
			assertEquals(Files.isDirectory(path) ? "rwx------" : "rw-------",
				PosixFilePermissions.toString(Files.getPosixFilePermissions(path)), path.toString()
			);
			return;
		}

		final UserPrincipal user = path.getFileSystem().getUserPrincipalLookupService()
			.lookupPrincipalByName(System.getProperty("user.name"));
		final List<AclEntry> acl = Files.getFileAttributeView(path, AclFileAttributeView.class).getAcl();

		assertFalse(acl.isEmpty(), path + " lets everyone near it");
		assertTrue(acl.stream().allMatch(entry -> entry.type() == AclEntryType.ALLOW
			&& user.equals(entry.principal())), path + " lets others near it: " + acl
		);
	}
}
