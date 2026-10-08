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

package org.unigrid.janus.shell;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.unigrid.janus.core.OwnerOnlyAssertions.assertOwnerOnly;

public class LogFolderTest {
	@TempDir
	private Path home;

	@Test
	public void shouldMakeTheLogFolderOnlyItsOwnerMayOpen() throws IOException {
		LogFolder.prepare(home);

		final Path logs = home.resolve(".janus").resolve("logs");

		assertTrue(Files.isDirectory(logs));
		assertOwnerOnly(logs);
	}

	@Test
	public void shouldLeaveAnExistingLogFolderAsItIs() throws IOException {
		final Path kept = Files.createDirectories(home.resolve(".janus").resolve("logs")).resolve("janus.log");

		Files.writeString(kept, "earlier");
		LogFolder.prepare(home);

		assertEquals("earlier", Files.readString(kept));
	}
}
