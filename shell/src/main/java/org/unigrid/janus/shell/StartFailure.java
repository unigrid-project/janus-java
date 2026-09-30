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

import java.util.Objects;

/**
 * What a person sees when the window cannot be opened. A launched application has no console, so without
 * this a damaged browser engine would leave a server running and nothing on the screen.
 */
final class StartFailure {
	private StartFailure() {
	}

	static String message(final CefLocation location, final Throwable cause) {
		final String reason = Objects.requireNonNullElse(cause.getMessage(), cause.getClass().getSimpleName());

		return "Janus could not start its browser engine from " + location.directory() + ".\n\n" + reason
			+ "\n\nInstall Janus again to restore it.";
	}
}
