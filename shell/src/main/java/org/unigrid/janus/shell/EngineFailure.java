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

import org.unigrid.oriel.window.Application;
import org.unigrid.oriel.window.StartFailure;
import org.unigrid.oriel.window.engine.CefLocation;

/* What Janus tells a person whose browser engine would not start. It names the folder the engine was looked for
   in: the installer's when Janus was installed, the home folder when it was not. */
final class EngineFailure {
	private EngineFailure() {
	}

	static String message(final Application application, final Throwable cause) {
		return StartFailure.message(application, new CefLocation(application.id()).directory(), cause);
	}
}
