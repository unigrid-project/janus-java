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
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	/* What a computed style says of a scrollbar's thumb depends on the Chromium asked and on where its pointer is,
	   and its parsed rule has lost the variable it names, so the thumb's rules are read from the stylesheet as it
	   is served, and the colours they name from the theme. */
	private String thumbBackground(final boolean hovered) {
		return (String) page().evaluate("async () => { const css = await (await fetch('/static/css/janus.css'))"
			+ ".text(); return [...css.matchAll(/([^{}]*::-webkit-scrollbar-thumb[^{]*)\\{([^}]*)\\}/g)]"
			+ ".filter(rule => rule[1].includes(':hover') === " + hovered + ")"
			+ ".map(rule => rule[2]).join(' '); }");
	}

	private String colour(final String variable) {
		return (String) page().evaluate("() => { const probe = document.createElement('div');"
			+ " probe.style.color = 'var(" + variable + ")'; document.body.append(probe);"
			+ " const colour = getComputedStyle(probe).color; probe.remove(); return colour; }");
	}

	@Example
	public void shouldDrawTheScrollbarSlimAndInTheColoursOfTheTheme() {
		assertEquals("14px", scrollbar("", "width"));
		assertTrue(thumbBackground(false).contains("background: var(--border-strong)"), thumbBackground(false));
		assertTrue(thumbBackground(true).contains("background: var(--accent)"), thumbBackground(true));

		final String accent = colour("--accent");
		final String idle = colour("--border-strong");

		assertEquals("rgb(254, 116, 22)", accent);

		page().click(TOGGLE);
		assertNotEquals(accent, colour("--accent"));
		assertNotEquals(idle, colour("--border-strong"));
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
