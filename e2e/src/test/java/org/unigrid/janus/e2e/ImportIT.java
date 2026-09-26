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

package org.unigrid.janus.e2e;

import net.jqwik.api.Example;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ImportIT extends BrowserTest {
	private static final String CARD = "main > .card";
	private static final String IMPORT = "[hx-post='/action/import']";
	private static final String BACK = "[hx-post='/action/welcome']";

	@Example
	public void shouldSwapTheCardForTheImportStepAndBack() {
		page().click(IMPORT);
		assertThat(page().locator(CARD + " h1")).hasText("Bring your wallet");

		page().click(BACK);
		assertThat(page().locator(CARD + " h1")).hasText("Welcome to Unigrid");
		assertThat(page().locator(CARD)).hasCount(1);
	}
}
