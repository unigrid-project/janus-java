/*
    The Janus Wallet
    Copyright © 2021-2022 The Unigrid Foundation, UGD Software AB

    This program is free software: you can redistribute it and/or modify it under the terms of the
    addended GNU Affero General Public License as published by the Free Software Foundation, version 3
    of the License (see COPYING and COPYING.addendum).

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received an addended copy of the GNU Affero General Public License with this program.
    If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.model.service;

import java.awt.Desktop;
import java.net.URI;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.unigrid.janus.model.cdi.Eager;

@Eager
@ApplicationScoped
@RequiredArgsConstructor
public class WindowService {
	@Inject private DebugService debug;

	private static WindowService serviceInstance = null;

	public static WindowService getInstance() {
		if (serviceInstance == null) {
			serviceInstance = new WindowService();
		}
		return serviceInstance;
	}

	public void browseURL(String url) {
		try {
			String os = System.getProperty("os.name").toLowerCase();
			if (os.indexOf("win") >= 0) {
				if (Desktop.isDesktopSupported()
					&& Desktop.getDesktop().isSupported(
						Desktop.Action.BROWSE)) {
					Desktop.getDesktop().browse(
						new URI(url));
				}
			} else if (os.indexOf("mac") >= 0) {
				Runtime rt = Runtime.getRuntime();
				rt.exec("open " + url);
			} else { // linux
				new ProcessBuilder("x-www-browser", url).start();
			}
		} catch (Exception ex) {
			debug.log(String.format(
				"ERROR: (browse url) %s",
				ex.getMessage()));
		}
	}
}
