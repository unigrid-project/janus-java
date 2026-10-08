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
import java.nio.file.Path;
import org.unigrid.janus.core.OwnerOnly;

/**
 * Where the log is kept. Logging creates the folder itself with the permissions of the user's defaults, so it
 * is made here first, before logging starts; the files put in it are then the owner's alone.
 */
final class LogFolder {
	private LogFolder() {
	}

	/* Logging is not up yet, so a folder that could not be made is reported on the console, and logging then
	   makes one of its own rather than the wallet refusing to start. */
	static void prepare(final Path home) {
		try {
			OwnerOnly.createDirectories(home.resolve(".janus").resolve("logs"));
		} catch (IOException e) {
			System.err.println("The log folder could not be made private: " + e);
		}
	}
}
