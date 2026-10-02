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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ThemeIT extends BrowserTest {
	private static final String TOGGLE = "[data-theme-toggle]";
	private static final String THEME = "data-theme";

	@Example
	public void shouldStartDark() {
		assertThat(page().locator("html")).hasAttribute(THEME, "dark");
	}

	private String scrollbar(final String part, final String property) {
		return (String) page().evaluate("() => getComputedStyle(document.documentElement, '::-webkit-scrollbar"
			+ part + "')." + property);
	}

	/* Chromium answers for a pseudo-element without regard to the pointer, so the colour it reports is that of the
	   last rule that matches, which is the theme's accent. What matters is that it is the theme's own. */
	@Example
	public void shouldDrawTheScrollbarSlimAndInTheColoursOfTheTheme() {
		final String dark = scrollbar("-thumb", "backgroundColor");

		assertEquals("14px", scrollbar("", "width"));
		assertEquals("rgb(254, 116, 22)", dark);

		page().click(TOGGLE);
		assertNotEquals(dark, scrollbar("-thumb", "backgroundColor"));
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
