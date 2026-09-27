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

import com.microsoft.playwright.Locator;
import net.jqwik.api.Example;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ThemeIT extends BrowserTest {
	private static final String TOGGLE = "[data-theme-toggle]";
	private static final String THEME = "data-theme";

	@Example
	public void shouldStartDark() {
		assertThat(page().locator("html")).hasAttribute(THEME, "dark");
	}

	@Example
	public void shouldKeepTheChosenThemeOnceThePageIsLoadedAgain() {
		final Locator html = page().locator("html");

		page().click(TOGGLE);
		assertThat(html).hasAttribute(THEME, "light");

		page().reload();
		assertThat(html).hasAttribute(THEME, "light");

		page().click(TOGGLE);
		assertThat(html).hasAttribute(THEME, "dark");
	}
}
