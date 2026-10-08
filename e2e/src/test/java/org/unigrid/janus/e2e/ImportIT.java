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

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import net.jqwik.api.Example;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImportIT extends BrowserTest {
	private static final String CARD = "main > .card";
	private static final String IMPORT = "[hx-post='/action/import']";
	private static final String BACK = "[hx-post='/action/welcome']";
	private static final String USE_FOUND = "[hx-post='/action/import-found']";
	private static final String CHOOSE_FILE = "[data-choose-file]";
	private static final String CONTINUE = CARD + " .button--primary";
	private static final Pattern SELECTED = Pattern.compile("\\bchoice--selected\\b");

	@Example
	public void shouldSwapTheCardForTheImportStepAndBack() {
		page().click(IMPORT);
		assertThat(page().locator(CARD + " h1")).hasText("Bring your wallet");

		page().click(BACK);
		assertThat(page().locator(CARD + " h1")).hasText("Welcome to Unigrid");
		assertThat(page().locator(CARD)).hasCount(1);
	}

	@Example
	public void shouldUseTheWalletLeftBehind() throws Exception {
		rig().leaveWalletBehind();
		page().click(IMPORT);
		page().click(USE_FOUND);

		assertThat(page().locator(USE_FOUND)).hasClass(SELECTED);
		assertThat(page().locator(CONTINUE)).isEnabled();
	}

	@Example
	public void shouldImportTheFilePickedInTheDialogOfTheHost() throws Exception {
		final Path wallet = rig().keepWalletElsewhere();

		rig().window().picking(wallet);
		page().click(IMPORT);
		page().click(CHOOSE_FILE);

		assertThat(page().locator(CHOOSE_FILE)).hasClass(SELECTED);
		assertThat(page().locator(CHOOSE_FILE + " .choice__note")).hasText(wallet.toString());
		assertThat(page().locator(CARD + " > .step__note").last()).containsText(rig().backups().toString());
		assertEquals(false, page().evaluate("document.body.inert"));
		assertEquals(List.of("choose-file:Choose a wallet.dat or wallet dump"), rig().window().commands());
	}

	@Example
	public void shouldSayThatAnActionFailedRatherThanLookIgnored() {
		expectTroubles();
		page().click(IMPORT);
		assertThat(page().locator(CARD + " h1")).hasText("Bring your wallet");

		page().evaluate("() => htmx.ajax('POST', '/action/open-wallet', {target: 'main > .card'})");

		assertThat(page().locator(CARD + " [data-failure]")).containsText("That did not work");
		assertThat(page().locator(CARD + " h1")).hasText("Bring your wallet");
	}

	/* The page is inert while the dialog is open, and must come back to life however it closed. */
	@Example
	public void shouldLeaveTheCardAsItWasWhenTheDialogIsDismissed() throws Exception {
		page().click(IMPORT);
		page().click(CHOOSE_FILE);
		rig().window().await(1, WAIT);
		page().waitForFunction("() => !document.body.inert");

		assertThat(page().locator(CHOOSE_FILE)).not().hasClass(SELECTED);
		assertThat(page().locator(CONTINUE)).isDisabled();
	}
}
