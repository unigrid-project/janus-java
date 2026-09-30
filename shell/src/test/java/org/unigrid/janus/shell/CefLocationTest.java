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

import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CefLocationTest {
	private static final Path HOME = Path.of("/home/someone");

	@Test
	public void shouldKeepTheEngineInTheHomeFolderWhenNoneIsBundled() {
		final CefLocation location = new CefLocation(null, HOME);

		assertFalse(location.isBundled());
		assertEquals(HOME.resolve(".janus/jcef").toFile(), location.directory());
	}

	@Test
	public void shouldUseTheFolderTheLauncherNames() {
		final CefLocation location = new CefLocation("/opt/unigrid/lib/app/jcef", HOME);

		assertTrue(location.isBundled());
		assertEquals(new File("/opt/unigrid/lib/app/jcef"), location.directory());
	}

	@Test
	public void shouldIgnoreAnEmptyOrBlankProperty() {
		for (final String value : new String[] {"", "  "}) {
			final CefLocation location = new CefLocation(value, HOME);

			assertFalse(location.isBundled());
			assertEquals(HOME.resolve(".janus/jcef").toFile(), location.directory());
		}
	}

	@Test
	public void shouldTrustTheBundledFolderEvenWhenItIsMissing() {
		final CefLocation location = new CefLocation("/nowhere/jcef", HOME);

		assertTrue(location.isBundled());
		assertFalse(location.directory().exists());
	}

	@Test
	public void shouldPreferTheBundledEngineOverOneInTheHomeFolder() {
		final Path home = Path.of(System.getProperty("java.io.tmpdir"));
		final CefLocation location = new CefLocation("/opt/unigrid/lib/app/jcef", home);

		assertEquals(new File("/opt/unigrid/lib/app/jcef"), location.directory());
	}
}
