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

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import net.jqwik.api.Example;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.unigrid.janus.core.OwnerOnlyAssertions.assertOwnerOnly;

/* Each kind of file system Janus meets is stood in for by one in memory, so both ways of keeping others out are
   tested wherever the tests run. */
public class OwnerOnlyTest {
	private static FileSystem posix() {
		return Jimfs.newFileSystem(Configuration.unix().toBuilder().setAttributeViews("owner", "posix").build());
	}

	/* The way Windows keeps others out: an access list naming who may do what. */
	private static FileSystem acl() {
		return Jimfs.newFileSystem(Configuration.unix().toBuilder().setAttributeViews("owner", "acl").build());
	}

	@Example
	public void shouldMakeAFileOnlyItsOwnerMayReadWherePermissionsAreKept() throws IOException {
		try (FileSystem files = posix()) {
			assertOwnerOnly(OwnerOnly.createFile(files.getPath("/wallet")));
		}
	}

	@Example
	public void shouldMakeAFileOnlyItsOwnerMayReadWhereAccessIsListed() throws IOException {
		try (FileSystem files = acl()) {
			assertOwnerOnly(OwnerOnly.createFile(files.getPath("/wallet")));
		}
	}

	@Example
	public void shouldMakeFoldersOnlyTheirOwnerMayOpenWherePermissionsAreKept() throws IOException {
		try (FileSystem files = posix()) {
			assertOwnerOnly(OwnerOnly.createDirectories(files.getPath("/home/ann/wallets")));
		}
	}

	@Example
	public void shouldMakeFoldersOnlyTheirOwnerMayOpenWhereAccessIsListed() throws IOException {
		try (FileSystem files = acl()) {
			assertOwnerOnly(OwnerOnly.createDirectories(files.getPath("/home/ann/wallets")));
		}
	}

	@Example
	public void shouldMakeATemporaryFileOnlyItsOwnerMayRead() throws IOException {
		try (FileSystem files = acl()) {
			final Path folder = Files.createDirectory(files.getPath("/wallets"));
			final Path made = OwnerOnly.createTempFile(folder, "evm-", ".part");

			assertTrue(made.getFileName().toString().startsWith("evm-"));
			assertOwnerOnly(made);
		}
	}
}
