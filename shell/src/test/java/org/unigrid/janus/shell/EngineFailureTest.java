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

import java.awt.Dimension;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.unigrid.oriel.server.AppId;
import org.unigrid.oriel.window.Application;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EngineFailureTest {
	/* The property the installer's launcher names the bundled engine with, as desktop/pom.xml passes it. */
	private static final String BUNDLED = "janus.jcef";
	private static final File INSTALLED = new File("/opt/unigrid/lib/app/jcef");
	private static final Application UNIGRID = new Application(AppId.of("janus"), "Unigrid", List.of(),
		new Dimension(1240, 800), new Dimension(900, 640)
	);

	private String kept;

	@BeforeEach
	public void forgetTheBundledEngine() {
		kept = System.getProperty(BUNDLED);
		System.clearProperty(BUNDLED);
	}

	@AfterEach
	public void restoreTheBundledEngine() {
		if (kept == null) {
			System.clearProperty(BUNDLED);
		} else {
			System.setProperty(BUNDLED, kept);
		}
	}

	@Test
	public void shouldNameTheHomeFolderWhenNoEngineIsBundled() {
		final Path home = Path.of(System.getProperty("user.home"), ".janus", "jcef");
		final String message = EngineFailure.message(UNIGRID, new IllegalStateException("x"));

		assertTrue(message.contains(home.toString()), message);
	}

	@Test
	public void shouldNameTheInstalledEngineWhenOneIsBundled() {
		System.setProperty(BUNDLED, INSTALLED.getPath());

		final String message = EngineFailure.message(UNIGRID, new UnsatisfiedLinkError("libcef.so"));

		assertTrue(message.contains(INSTALLED.toString()), message);
	}
}
