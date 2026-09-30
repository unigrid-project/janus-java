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

package org.unigrid.janus.desktop;

import java.io.File;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler;

/**
 * Fetches the browser engine at build time, into the folder the installer carries, so that an installed Janus
 * starts without a download. The release and the layout are the ones jcefmaven expects at run time.
 */
public final class PrepareCef {
	private PrepareCef() {
	}

	public static void main(final String[] arguments) throws Exception {
		final CefAppBuilder builder = new CefAppBuilder();

		builder.setInstallDir(new File(arguments[0]));
		builder.setProgressHandler(new ConsoleProgressHandler());
		builder.install();
	}
}
