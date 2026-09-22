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

import java.nio.file.Path;
import java.util.regex.Pattern;
import net.jqwik.api.Example;
import org.unigrid.janus.web.Templates;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ViewsTest {
	private static final Path DIRECTORY = Path.of("/data/unigrid");
	private static final Path FOUND = DIRECTORY.resolve("wallet.dat");
	private static final Path PICKED = Path.of("/mnt/backup/wallet.dat");
	private static final String LIT_DOT = "steps__dot--lit";
	private static final String FOUND_CHOICE = "type=\"button\" hx-post=\"/action/import-found\"";
	private static final String FILE_CHOICE = "type=\"button\" data-choose-file";

	private final Templates templates = new Templates(false);

	private static int count(final String html, final String token) {
		return html.split(Pattern.quote(token), -1).length - 1;
	}

	private static String primary(final String choice) {
		return "class=\"choice choice--primary\" " + choice;
	}

	private static String selected(final String choice) {
		return "class=\"choice choice--selected\" " + choice;
	}

	private static String plain(final String choice) {
		return "class=\"choice\" " + choice;
	}

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
	public void shouldLeadFromTheWelcomeToTheImportStep() {
		final String html = templates.render(new IndexView("Unigrid"));

		assertTrue(html.contains("hx-post=\"/action/import\""), html);
		assertEquals(1, count(html, LIT_DOT), html);
	}

	@Example
	public void shouldRenderTheWelcomeStepOnItsOwnWhenComingBack() {
		final String html = templates.render(new WelcomeView());

		assertTrue(html.contains("Welcome to <span class=\"gradient-text\">Unigrid</span>"), html);
		assertFalse(html.contains("<!DOCTYPE html>"), "only the card should render: " + html);
	}

	@Example
	public void shouldOfferTheFoundWalletFirst() {
		final String html = templates.render(new ImportView(DIRECTORY, FOUND, null));

		assertTrue(html.contains("Import wallet"), html);
		assertTrue(html.contains(primary(FOUND_CHOICE)), html);
		assertTrue(html.contains("Use the wallet on this computer"), html);
		assertTrue(html.contains(FOUND.toString()), html);
		assertEquals(2, count(html, LIT_DOT), html);
	}

	@Example
	public void shouldSayWhereItLookedWhenNothingWasFound() {
		final String html = templates.render(new ImportView(DIRECTORY, null, null));

		assertTrue(html.contains("No wallet was found in"), html);
		assertTrue(html.contains(DIRECTORY.toString()), html);
		assertFalse(html.contains("import-found"), html);
		assertTrue(html.contains(primary(FILE_CHOICE)), html);
	}

	@Example
	public void shouldHoldTheContinueButtonBackUntilAWalletIsChosen() {
		final String html = templates.render(new ImportView(DIRECTORY, FOUND, null));

		assertTrue(html.contains("class=\"button button--primary\" type=\"button\" disabled=\"disabled\""), html);
		assertFalse(html.contains("choice--selected"), html);
	}

	@Example
	public void shouldMarkTheFoundWalletOnceItIsChosen() {
		final String html = templates.render(new ImportView(DIRECTORY, FOUND, FOUND));

		assertTrue(html.contains(selected(FOUND_CHOICE)), html);
		assertTrue(html.contains(plain(FILE_CHOICE)), html);
		assertTrue(html.contains("class=\"button button--primary\" type=\"button\">Continue"), html);
	}

	@Example
	public void shouldShowThePickedFileOnItsChoice() {
		final String html = templates.render(new ImportView(DIRECTORY, FOUND, PICKED));

		assertTrue(html.contains(selected(FILE_CHOICE)), html);
		assertTrue(html.contains(PICKED.toString()), html);
		assertTrue(html.contains(plain(FOUND_CHOICE)), "only the choice made should stand out: " + html);
	}

	@Example
	public void shouldSendThePickedFileToTheImportAction() {
		final String html = templates.render(new ImportView(DIRECTORY, null, null));

		assertTrue(html.contains("hx-post=\"/action/import-file\""), html);
		assertTrue(html.contains("hx-trigger=\"file-chosen\""), html);
		assertTrue(html.contains("hx-vals=\"js:{path: event.detail.path}\""), html);
	}

	@Example
	public void shouldOfferTheRecoveryPhraseAsWell() {
		final String html = templates.render(new ImportView(DIRECTORY, null, null));

		assertTrue(html.contains("Restore from recovery phrase"), html);
	}

	@Example
	public void shouldGoBackToTheWelcomeStep() {
		final String html = templates.render(new ImportView(DIRECTORY, null, null));

		assertTrue(html.contains("hx-post=\"/action/welcome\""), html);
		assertFalse(html.contains("<!DOCTYPE html>"), "only the card should render: " + html);
	}

	@Example
	public void shouldOfferTheThemeToggleWithAnIconForEitherTheme() {
		final String html = templates.render(new IndexView("Unigrid"));

		assertTrue(html.contains("data-theme-toggle"), html);
		assertTrue(html.contains("class=\"titlebar__sun\""), html);
		assertTrue(html.contains("class=\"titlebar__moon\""), html);
	}

	@Example
	public void shouldCarryResizeHandlesOnThreeEdgesAndTheCorner() {
		final String html = templates.render(new IndexView("Unigrid"));

		assertTrue(html.contains("data-resize=\"left\""), html);
		assertTrue(html.contains("data-resize=\"right\""), html);
		assertTrue(html.contains("data-resize=\"bottom\""), html);
		assertTrue(html.contains("data-resize=\"bottom-right\""), html);
	}

	@Example
	public void shouldShowTheVersionInTheAboutBlock() {
		final String html = templates.render(new AboutView("9.9.9"));

		assertTrue(html.contains("Janus <span>9.9.9</span>"), html);
		assertFalse(html.contains("<!DOCTYPE html>"), "only the block should render: " + html);
	}
}
