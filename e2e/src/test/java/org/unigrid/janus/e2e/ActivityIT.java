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

/** The wallet screens clicked through in a browser: what the filters, the pages and the failures look like to a person. */
public class ActivityIT extends BrowserTest {
	private static final Page.WaitForSelectorOptions LOADED = new Page.WaitForSelectorOptions().setTimeout(15_000);
	private static final String ROWS = "#rows details";
	private static final String ACTIVITY = "button.app__tab:has-text('Activity')";
	private static final String ACTIVE_FILTER = ".filter--active";
	private static final String MORE = ".ledger__more";
	private static final String CARD = "main > .card";

	private static AddressTransaction entry(final String txid, final int minutes, final String amount,
		final EntryKind kind) {

		return new AddressTransaction(txid, Instant.parse("2019-01-01T00:00:00Z").plusSeconds(minutes * 60L),
			minutes, new BigDecimal(amount), kind
		);
	}

	private void chooseTheWalletLeftBehind() {
		page().click("[hx-post='/action/import']");
		page().click("[hx-post='/action/import-found']");
		page().click("[hx-post='/action/open-wallet']");
	}

	private void openActivityOf(final AddressTransaction... entries) throws Exception {
		rig().hedgehog().address(rig().addresses().get(0), "100", entries);
		rig().leaveWalletBehind();
		chooseTheWalletLeftBehind();
		page().waitForSelector(".dashboard__total", LOADED);
		page().click(ACTIVITY);
	}

	private void openActivityOfFourKinds() throws Exception {
		openActivityOf(entry("aa", 1, "10", EntryKind.RECEIVED), entry("bb", 2, "-4", EntryKind.SENT),
			entry("cc", 3, "2", EntryKind.MINED), entry("dd", 4, "3", EntryKind.STAKED)
		);
	}

	private void chooseFilter(final String label) {
		page().click(".filter:has-text('" + label + "')");
	}

	@Example
	public void shouldNarrowTheListToTheKindTheChipNames() throws Exception {
		openActivityOfFourKinds();
		assertThat(page().locator(ROWS)).hasCount(4);

		chooseFilter("Received");
		assertThat(page().locator(ROWS)).hasCount(1);
		assertThat(page().locator(".ledger__txid")).hasText("aa");

		chooseFilter("Sent");
		assertThat(page().locator(ROWS)).hasCount(1);
		assertThat(page().locator(".ledger__txid")).hasText("bb");

		chooseFilter("Rewards");
		assertThat(page().locator(ROWS)).hasCount(2);
		assertThat(page().locator(ACTIVE_FILTER)).hasText("Rewards");

		chooseFilter("All");
		assertThat(page().locator(ROWS)).hasCount(4);
	}

	@Example
	public void shouldKeepTheChosenKindWhenTheTabIsLeftAndComeBackTo() throws Exception {
		openActivityOfFourKinds();
		chooseFilter("Rewards");

		page().click("button.app__tab:has-text('Dashboard')");
		page().click(ACTIVITY);

		assertThat(page().locator(ACTIVE_FILTER)).hasText("Rewards");
		assertThat(page().locator(ROWS)).hasCount(2);
	}

	@Example
	public void shouldSumWhatWasReceivedSentAndEarned() throws Exception {
		openActivityOfFourKinds();
		assertThat(page().locator(".summary__value")).hasText(new String[] {"+10.00", "−4.00", "+5.00", "+11.00"});
	}

	@Example
	public void shouldFilterByTheSummaryThatIsClicked() throws Exception {
		openActivityOfFourKinds();
		page().click(".summary:has-text('Sent')");
		assertThat(page().locator(ACTIVE_FILTER)).hasText("Sent");
		assertThat(page().locator(ROWS)).hasCount(1);
	}

	@Example
	public void shouldPageThroughALongHistoryAMoreAtATime() throws Exception {
		final AddressTransaction[] many = new AddressTransaction[250];

		for (int i = 0; i < many.length; i++) {
			many[i] = entry("t" + i, i, "1", i % 2 == 0 ? EntryKind.MINED : EntryKind.RECEIVED);
		}

		openActivityOf(many);
		assertThat(page().locator(ROWS)).hasCount(100);

		page().click(MORE);
		assertThat(page().locator(ROWS)).hasCount(200);

		page().click(MORE);
		assertThat(page().locator(ROWS)).hasCount(250);
		assertThat(page().locator(MORE)).hasCount(0);

		chooseFilter("Received");
		assertThat(page().locator(ROWS)).hasCount(100);
		page().click(MORE);
		assertThat(page().locator(ROWS)).hasCount(125);
		assertThat(page().locator(MORE)).hasCount(0);
	}

	@Example
	public void shouldFindATransactionByItsAddressWhateverTheCase() throws Exception {
		openActivityOf(entry("aa", 1, "10", EntryKind.RECEIVED), entry("bb", 2, "5", EntryKind.RECEIVED));

		assertThat(page().locator(ROWS)).hasCount(2);
		searchFor(rig().addresses().get(0).substring(2, 9).toLowerCase(), 2);
		searchFor("nothing like it", 0);
		assertThat(page().locator("#rows .ledger__empty")).hasText("No transactions match your filter.");
	}

	@Example
	public void shouldSayThatTheHistoryCouldNotBeSavedWhereTheHostSaid() throws Exception {
		openActivityOf(entry("aa", 1, "10", EntryKind.RECEIVED));

		rig().window().picking(rig().data());
		page().click("[data-save-file]");
		assertThat(page().locator("#export-note")).hasText("Could not save to " + rig().data());
		assertThat(page().locator("#export-note")).hasClass(Pattern.compile(".*export-note--failed.*"));
	}

	@Example
	public void shouldSaveTheWholeHistoryWhateverTheFilterShows() throws Exception {
		openActivityOfFourKinds();

		final Path file = rig().data().resolve("history.csv");

		rig().window().picking(file);
		chooseFilter("Sent");
		page().click("[data-save-file]");
		assertThat(page().locator("#export-note")).hasText("Saved to " + file);
		assertThat(page().locator(ROWS)).hasCount(1);
		assertEquals(5, Files.readAllLines(file).size());
	}

	@Example
	public void shouldTryAgainAfterTheLedgerWasNotSignedAndReachTheDashboard() throws Exception {
		rig().hedgehog().unsigned();
		rig().hedgehog().address(rig().addresses().get(0), "100", entry("aa", 1, "100", EntryKind.RECEIVED));
		rig().leaveWalletBehind();
		chooseTheWalletLeftBehind();

		assertThat(page().locator(".preparing__failure"))
			.hasText("The ledger is not signed by the Unigrid Foundation");

		rig().hedgehog().signed();
		page().click("[hx-post='/action/wallet-retry']");
		page().waitForSelector(".dashboard__total", LOADED);
		assertThat(page().locator(".dashboard__total")).hasText("100.00");
	}

	@Example
	public void shouldOfferAnotherWalletWhenTheOneChosenCannotBeRead() throws Exception {
		rig().leaveDamagedWalletBehind();
		chooseTheWalletLeftBehind();

		page().waitForSelector("[hx-post='/action/choose-another']", LOADED);
		page().click("[hx-post='/action/choose-another']");
		assertThat(page().locator(CARD + " h1")).hasText("Bring your wallet");
		assertTrue(rig().chosen().remembered().isEmpty());
	}

	@Example
	public void shouldSayHowFarTheLedgerHasGotWhileItIsDownloaded() throws Exception {
		rig().hedgehog().fetching(30);
		rig().leaveWalletBehind();
		chooseTheWalletLeftBehind();

		page().waitForSelector(".preparing__percent", LOADED);
		assertThat(page().locator(".preparing__percent")).hasText("30 %");
	}
}
