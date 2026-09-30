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

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StartFailureTest {
	private static final Path HOME = Path.of("/home/someone");

	@Test
	public void shouldNameTheFolderTheEngineWasLoadedFrom() {
		final String message = StartFailure.message(new CefLocation("/opt/unigrid/lib/app/jcef", HOME),
			new UnsatisfiedLinkError("libcef.so: cannot open shared object file"));

		assertTrue(message.contains("/opt/unigrid/lib/app/jcef"), message);
	}

	@Test
	public void shouldSayWhatWentWrong() {
		final String message = StartFailure.message(new CefLocation(null, HOME),
			new UnsatisfiedLinkError("libcef.so: cannot open shared object file"));

		assertTrue(message.contains("libcef.so: cannot open shared object file"), message);
	}

	@Test
	public void shouldNameTheHomeFolderWhenNoEngineIsBundled() {
		final String message = StartFailure.message(new CefLocation(null, HOME), new IllegalStateException("x"));

		assertTrue(message.contains(HOME.resolve(".janus/jcef").toString()), message);
	}

	@Test
	public void shouldStillSpeakWhenTheFailureHasNoMessage() {
		final String message = StartFailure.message(new CefLocation(null, HOME), new IllegalStateException());

		assertTrue(message.contains("IllegalStateException"), message);
	}
}
