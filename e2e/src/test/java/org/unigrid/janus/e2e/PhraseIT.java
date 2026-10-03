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
import com.microsoft.playwright.assertions.LocatorAssertions;
import java.util.List;
import java.util.regex.Pattern;
import net.jqwik.api.Example;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PhraseIT extends BrowserTest {
	private static final String ABANDON = "abandon abandon abandon abandon abandon abandon abandon abandon abandon "
		+ "abandon abandon about";
	private static final String CARD = "main > .card";
	private static final String CONTINUE = CARD + " .button--primary";
	private static final String APP = "#app";

	/* Deriving the keys of a new wallet and opening it takes seconds on a loaded CI runner, past the default five. */
	private static final LocatorAssertions.IsVisibleOptions OPENED = new LocatorAssertions.IsVisibleOptions()
		.setTimeout(20_000);

	@Example
	public void shouldMakeAWalletFromTheWordsTappedBackInOrder() {
		page().click("[hx-post='/action/create']");
		assertThat(page().locator(".phrase__word")).hasCount(12);

		final List<String> words = page().locator(".phrase__word > span:not(.phrase__n)").allTextContents();

		page().click("[hx-post='/action/create-verify']");
		assertThat(page().locator(CONTINUE)).isDisabled();

		for (final String word : words) {
			final Locator tile = page().locator("button.tile:not([disabled])")
				.filter(new Locator.FilterOptions().setHasText(Pattern.compile("^" + word + "$"))).first();

			tile.click();
			assertThat(page().locator(".tray .tile--picked").last()).containsText(word);
		}

		page().click(CONTINUE);
		seal("correct horse");
		assertThat(page().locator(APP)).isVisible(OPENED);
	}

	@Example
	public void shouldRestoreAPhrasePastedIntoTheFirstBox() {
		page().click("[hx-post='/action/import']");
		page().click("[hx-post='/action/restore']");
		page().fill("[name=word1]", ABANDON);
		page().click(CONTINUE);

		assertThat(page().locator(CARD + " h1")).hasText("Choose a password");
		seal("correct horse");
		assertThat(page().locator(APP)).isVisible(OPENED);
		assertEquals("evm-0x9858EfFD232B4033E47d90003D41EC34EcaEda94.json",
			rig().chosen().remembered().orElseThrow().getFileName().toString()
		);
	}

	@Example
	public void shouldGoBackFromThePasswordWithoutSendingIt() {
		page().click("[hx-post='/action/import']");
		page().click("[hx-post='/action/restore']");
		page().fill("[name=word1]", ABANDON);
		page().click(CONTINUE);
		page().fill("[name=password]", "correct horse");
		page().click("[hx-post='/action/phrase-back']");

		assertThat(page().locator("[name=word12]")).hasValue("about");
	}

	private void seal(final String password) {
		choosePassword(password, password);
		page().click(CONTINUE);
	}
}
