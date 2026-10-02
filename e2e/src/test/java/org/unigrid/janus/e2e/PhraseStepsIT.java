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
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import net.jqwik.api.Example;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** The steps of making and restoring a wallet that go wrong or turn back, as a person meets them in a browser. */
public class PhraseStepsIT extends BrowserTest {
	private static final String ABANDON = String.join(" ", Collections.nCopies(11, "abandon")) + " about";
	private static final String CARD = "main > .card";
	private static final String CONTINUE = CARD + " .button--primary";
	private static final String ERROR = CARD + " .step__error";
	private static final String STATUS = ".verify__status";
	private static final double SERVER_WOULD_HAVE_ANSWERED_MILLIS = 1500;

	private void restoreWith(final String typed) {
		page().click("[hx-post='/action/import']");
		page().click("[hx-post='/action/restore']");
		page().fill("[name=word1]", typed);
		page().click(CONTINUE);
	}

	/* The next tile is only tapped once this one is in the tray, as a tile tapped during the redraw is lost. */
	private void tap(final String word) {
		final Locator picked = page().locator(".tray .tile--picked");
		final int before = picked.count();

		page().locator("button.tile:not([disabled])")
			.filter(new Locator.FilterOptions().setHasText(Pattern.compile("^" + word + "$"))).first().click();
		assertThat(picked).hasCount(before + 1);
	}

	private List<String> writtenDown() {
		page().click("[hx-post='/action/create']");
		assertThat(page().locator(".phrase__word")).hasCount(12);
		return page().locator(".phrase__word > span:not(.phrase__n)").allTextContents();
	}

	@Example
	public void shouldNameTheWordThatIsNoWordAndKeepWhatWasTyped() {
		restoreWith(ABANDON.replace("about", "unigrid"));

		assertThat(page().locator(ERROR)).hasText("\"unigrid\" is not a word recovery phrases use");
		assertThat(page().locator("[name=word1]")).hasValue("abandon");
		assertThat(page().locator("[name=word12]")).hasValue("unigrid");
	}

	@Example
	public void shouldSayHowManyWordsWereGivenWhenThereAreTooFew() {
		restoreWith(ABANDON.substring(0, ABANDON.length() - " about".length()));

		assertThat(page().locator(ERROR)).hasText("A recovery phrase has 12 words, not 11");
	}

	@Example
	public void shouldSayTheOrderIsWrongWhenTheWordsDoNotMakeAPhrase() {
		restoreWith("abandon ".repeat(12));

		assertThat(page().locator(ERROR)).hasText("These words do not make a recovery phrase; check their order");
	}

	@Example
	public void shouldRestoreWordsTypedOnePerBox() {
		page().click("[hx-post='/action/import']");
		page().click("[hx-post='/action/restore']");

		final String[] words = ABANDON.split(" ");

		for (int n = 0; n < words.length; n++) {
			page().fill("[name=word" + (n + 1) + "]", words[n]);
		}

		page().click(CONTINUE);
		assertThat(page().locator(CARD + " h1")).hasText("Choose a password");
	}

	@Example
	public void shouldHoldBackAShortPasswordInTheBrowserAndRefuseTwoThatDiffer() {
		restoreWith(ABANDON);

		page().fill("[name=password]", "1234567");
		page().fill("[name=repeat]", "1234567");
		page().click(CONTINUE);

		/* Had the browser let the short password through, the server would have answered with its own message. */
		page().waitForTimeout(SERVER_WOULD_HAVE_ANSWERED_MILLIS);
		assertThat(page().locator(ERROR)).hasCount(0);
		assertThat(page().locator(CARD + " h1")).hasText("Choose a password");

		page().fill("[name=password]", "correct horse");
		page().fill("[name=repeat]", "correct hose");
		page().click(CONTINUE);
		assertThat(page().locator(ERROR)).hasText("The two passwords differ");
	}

	@Example
	public void shouldGoBackFromTheNewWordsToTheWelcomeCard() {
		writtenDown();
		page().click("[hx-post='/action/create-back']");

		assertThat(page().locator(CARD + " h1")).hasText("Welcome to Unigrid");
	}

	@Example
	public void shouldGoBackToTheSameWordsAndStartPickingAgain() {
		final List<String> words = writtenDown();

		page().click("[hx-post='/action/create-verify']");
		tap(words.get(0));
		assertThat(page().locator(STATUS)).hasText("1/12");

		page().click("[hx-post='/action/create-seed']");
		assertThat(page().locator(".phrase__word > span:not(.phrase__n)")).hasText(words.toArray(String[]::new));

		page().click("[hx-post='/action/create-verify']");
		assertThat(page().locator(".tray__hint")).hasText("select below…");
	}

	@Example
	public void shouldClearWhatWasPickedWhenToldToStartOver() {
		final List<String> words = writtenDown();

		page().click("[hx-post='/action/create-verify']");
		tap(words.get(0));
		tap(words.get(1));
		assertThat(page().locator(STATUS)).hasText("2/12");

		page().click("[hx-post='/action/create-reset']");
		assertThat(page().locator(".tray__hint")).hasText("select below…");
		assertThat(page().locator("button.tile[disabled]")).hasCount(0);
	}

	@Example
	public void shouldKeepTheConfirmedPhraseWhenGoingBackFromThePassword() {
		final List<String> words = writtenDown();

		page().click("[hx-post='/action/create-verify']");
		words.forEach(this::tap);
		page().click(CONTINUE);
		assertThat(page().locator(CARD + " h1")).hasText("Choose a password");

		page().click("[hx-post='/action/phrase-back']");
		assertThat(page().locator(STATUS)).hasText("✓ confirmed");
	}
}
