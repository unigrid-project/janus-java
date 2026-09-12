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

package org.unigrid.janus.ui.view;

import net.jqwik.api.Example;
import org.unigrid.janus.web.Templates;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ViewsTest {
	private final Templates templates = new Templates(false);

	@Example
	public void shouldShowTheTitleInTheTabAndTheTitleBar() {
		final String html = templates.render(new IndexView("Unigrid"));

		assertTrue(html.contains("<title>Unigrid</title>"), html);
		assertTrue(html.contains("<span class=\"titlebar__title\">Unigrid</span>"), html);
	}

	@Example
	public void shouldWelcomeWithTheTwoWaysToGetAWallet() {
		final String html = templates.render(new IndexView("Unigrid"));

		assertTrue(html.contains("Welcome to <span class=\"gradient-text\">Unigrid</span>"), html);
		assertTrue(html.contains("Create a new wallet"), html);
		assertTrue(html.contains("Import existing wallet"), html);
	}

	@Example
	public void shouldOfferTheThemeToggleWithAnIconForEitherTheme() {
		final String html = templates.render(new IndexView("Unigrid"));

		assertTrue(html.contains("data-theme-toggle"), html);
		assertTrue(html.contains("class=\"titlebar__sun\""), html);
		assertTrue(html.contains("class=\"titlebar__moon\""), html);
	}

	@Example
	public void shouldShowTheVersionInTheAboutBlock() {
		final String html = templates.render(new AboutView("9.9.9"));

		assertTrue(html.contains("Janus <span>9.9.9</span>"), html);
		assertTrue(!html.contains("<!DOCTYPE html>"), "only the block should render: " + html);
	}
}
