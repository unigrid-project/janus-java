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

import com.microsoft.playwright.Page;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.regex.Pattern;
import net.jqwik.api.Example;
import org.unigrid.janus.core.hedgehog.AddressTransaction;
import org.unigrid.janus.core.hedgehog.EntryKind;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WalletIT extends BrowserTest {
	private static final Page.WaitForSelectorOptions LOADED = new Page.WaitForSelectorOptions().setTimeout(15_000);

	private static AddressTransaction entry(final String txid, final int minutes, final String amount,
		final EntryKind kind) {

		return new AddressTransaction(txid, Instant.parse("2019-01-01T00:00:00Z").plusSeconds(minutes * 60L),
			minutes, new BigDecimal(amount), kind
		);
	}

	private void openTheWalletLeftBehind() throws Exception {
		rig().hedgehog().address(rig().addresses().get(0), "480", entry("aa", 1, "1020", EntryKind.RECEIVED),
			entry("bb", 9, "-540", EntryKind.SENT)
		);
		rig().leaveWalletBehind();
		page().click("[hx-post='/action/import']");
		page().click("[hx-post='/action/import-found']");
		page().click("[hx-post='/action/open-wallet']");
		page().waitForSelector(".dashboard__total", LOADED);
	}

	@Example
	public void shouldReachTheDashboardOnItsOwnWhileTheLedgerIsPrepared() throws Exception {
		openTheWalletLeftBehind();
		assertThat(page().locator(".dashboard__total")).hasText("480.00");
		assertThat(page().locator("#app")).not().hasAttribute("hx-trigger", Pattern.compile(".*"));
	}

	/* The window used to shrink and centre itself, a leftover of the onboarding layout it swaps out of. */
	@Example
	public void shouldFillTheWindowRatherThanShrinkToItsContent() throws Exception {
		openTheWalletLeftBehind();

		final double window = page().viewportSize().width;
		final double app = page().locator("#app").boundingBox().width;

		assertEquals(window, app, 1.0);
	}

	@Example
	public void shouldOpenARowOntoItsDetails() throws Exception {
		openTheWalletLeftBehind();
		page().click("button.app__tab:has-text('Activity')");
		page().locator("#rows summary").first().click();
		assertThat(page().locator("#rows details").first()).hasAttribute("open", "");
		assertThat(page().locator("#rows details").first().locator(".ledger__txid")).hasText("bb");
	}

	@Example
	public void shouldNarrowTheListAsTheSearchIsTyped() throws Exception {
		openTheWalletLeftBehind();
		page().click("button.app__tab:has-text('Activity')");
		page().locator("input[name=q]").pressSequentially("aa");
		assertThat(page().locator("#rows details")).hasCount(1);
		assertThat(page().locator("#rows .ledger__txid")).hasText("aa");
	}

	@Example
	public void shouldSaveTheHistoryWhereTheHostsDialogSaid() throws Exception {
		openTheWalletLeftBehind();

		final Path file = rig().data().resolve("history.csv");

		rig().window().picking(file);
		page().click("button.app__tab:has-text('Activity')");
		page().click("[data-save-file]");
		assertThat(page().locator("#export-note")).hasText("Saved to " + file);
		assertEquals(3, Files.readAllLines(file).size());
		assertTrue(rig().window().commands()
			.contains("save-file:Save the history as CSV:unigrid-legacy-history.csv"));
	}

	@Example
	public void shouldLeaveTheLaterTabsAlone() throws Exception {
		openTheWalletLeftBehind();
		assertThat(page().locator("button.app__tab:has-text('Gridnodes')")).isDisabled();
		assertThat(page().locator("button.app__tab:has-text('Settings')")).isDisabled();
	}
}
