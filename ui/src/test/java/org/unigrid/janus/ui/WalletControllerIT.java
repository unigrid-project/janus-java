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

package org.unigrid.janus.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import org.jsoup.nodes.Element;
import org.unigrid.janus.core.hedgehog.AddressTransaction;
import org.unigrid.janus.core.hedgehog.EntryKind;
import org.unigrid.janus.core.wallet.LedgerState;
import org.unigrid.janus.core.wallet.WalletLedger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.unigrid.janus.ui.FlowSupport.entry;
import static org.unigrid.janus.ui.FlowSupport.entryOn;
import static org.unigrid.janus.ui.FlowSupport.settle;

/** The wallet screens with the container wiring every controller, the way the shell does. */
public class WalletControllerIT {
	private static final String IMPORT = "[hx-post=/action/import]";
	private static final String FOUND = "[hx-post=/action/import-found]";
	private static final String OPEN = "[hx-post=/action/open-wallet]";
	private static final String ACTIVITY = "button.app__tab[hx-post=/action/activity]";
	private static final String DASHBOARD = "button.app__tab[hx-post=/action/dashboard]";
	private static final String RECEIVED = ".filter[hx-vals*=RECEIVED]";
	private static final String SENT = ".filter[hx-vals*=SENT]";
	private static final String REWARDS = ".filter[hx-vals*=REWARDS]";
	private static final String ROWS = "#rows details";
	private static final String SEARCH = "input[name=q]";
	private static final String ACTIVE_FILTER = ".filter--active";
	private static final String SAVE = "[data-save-file]";
	private static final String NOTE = "#export-note";
	private static final String TOTAL = ".dashboard__total";

	private ContainerRig rig;

	@BeforeTry
	public void serve() throws Exception {
		rig = new ContainerRig();
	}

	@AfterTry
	public void stop() throws Exception {
		rig.close();
	}

	private Screen openedWithTheWalletLeftBehind() throws Exception {
		rig.leaveWalletBehind();
		return rig.open().click(IMPORT).click(FOUND).click(OPEN);
	}

	private Screen onTheDashboard(final AddressTransaction... entries) throws Exception {
		rig.hedgehog().address(rig.addresses().get(0), "100", entries);
		return settle(openedWithTheWalletLeftBehind());
	}

	private static int status(final Screen screen, final String action, final String form) throws Exception {
		return screen.client().submit(action, form).statusCode();
	}

	@Example
	public void shouldGoFromTheWelcomeCardToTheDashboardAndOpenThereNextTime() throws Exception {
		rig.hedgehog().address(rig.addresses().get(0), "480", entry("aa", 1, "1020", EntryKind.RECEIVED),
			entry("bb", 9, "-540", EntryKind.SENT)
		);

		assertEquals("480.00", settle(openedWithTheWalletLeftBehind()).find(TOTAL).text());
		assertEquals("480.00", settle(rig.open()).find(TOTAL).text());
		assertEquals(rig.home().resolve(".janus").resolve("backups"),
			rig.chosen().remembered().orElseThrow().getParent()
		);
	}

	@Example
	public void shouldShowTheDashboardAgainWithNothingFilteredWhenAWalletIsOpenedAnew() throws Exception {
		final Screen screen = onTheDashboard(entry("aa", 1, "10", EntryKind.RECEIVED));

		screen.click(ACTIVITY).click(SENT).trigger(SEARCH, Map.of("q", "zz"));
		assertEquals(200, status(screen, "/action/open-wallet", ""));

		final Screen opened = settle(rig.open());

		assertEquals("100.00", opened.find(TOTAL).text());
		opened.click(ACTIVITY);
		assertEquals("All", opened.find(ACTIVE_FILTER).text());
		assertEquals("", opened.find(SEARCH).attr("value"));
		assertEquals(1, opened.document().select(ROWS).size());
	}

	@Example
	public void shouldLetThePersonChooseAnotherWalletWhenTheOneChosenCannotBeRead() throws Exception {
		rig.leaveDamagedWalletBehind();

		final Screen screen = settle(rig.open().click(IMPORT).click(FOUND).click(OPEN));

		screen.click("[hx-post=/action/choose-another]");
		assertEquals("Bring your wallet", screen.find("main > .card h1").text());
		assertTrue(rig.chosen().remembered().isEmpty());
		assertEquals(LedgerState.Phase.IDLE, rig.container().select(WalletLedger.class).get().state().phase());
	}

	@Example
	public void shouldShowOnlyWhatTheFilterAsksForAndKeepItWhenTheTabIsPressedAgain() throws Exception {
		final Screen screen = onTheDashboard(entry("aa", 1, "10", EntryKind.RECEIVED),
			entry("bb", 2, "-4", EntryKind.SENT), entry("cc", 3, "2", EntryKind.MINED),
			entry("dd", 4, "3", EntryKind.STAKED)
		).click(ACTIVITY);

		assertEquals(4, screen.document().select(ROWS).size());
		assertEquals(List.of("aa"), screen.click(RECEIVED).document().select(".ledger__txid").eachText());
		assertEquals(List.of("bb"), screen.click(SENT).document().select(".ledger__txid").eachText());
		assertEquals(List.of("dd", "cc"), screen.click(REWARDS).document().select(".ledger__txid").eachText());

		screen.click(DASHBOARD).click(ACTIVITY);
		assertEquals("Rewards", screen.find(ACTIVE_FILTER).text());
		assertEquals(2, screen.document().select(ROWS).size());
	}

	@Example
	public void shouldSumWhatWasReceivedSentAndEarned() throws Exception {
		final Screen screen = onTheDashboard(entry("aa", 1, "10", EntryKind.RECEIVED),
			entry("bb", 2, "-4", EntryKind.SENT), entry("cc", 3, "2", EntryKind.MINED),
			entry("dd", 4, "3", EntryKind.STAKED)
		).click(ACTIVITY);

		assertEquals(List.of("+10.00", "−4.00", "+5.00", "+11.00"),
			screen.document().select(".summary__value").eachText()
		);
	}

	@Example
	public void shouldFindATransactionByTheAddressItTouchedWhateverTheCaseAndPadding() throws Exception {
		final String address = rig.addresses().get(0);
		final Screen screen = onTheDashboard(entry("aa", 1, "10", EntryKind.RECEIVED),
			entry("bb", 2, "5", EntryKind.RECEIVED)
		).click(ACTIVITY);

		screen.trigger(SEARCH, Map.of("q", "  " + address.substring(2, 9).toLowerCase(Locale.ROOT) + "  "));
		assertEquals(2, screen.document().select(ROWS).size());

		screen.trigger(SEARCH, Map.of("q", "Hx9"));
		assertEquals("No transactions match your filter.", screen.find("#rows .ledger__empty").text());
	}

	@Example
	public void shouldPageThroughWhatTheFilterLeavesAndNotWhatItHides() throws Exception {
		final AddressTransaction[] many = new AddressTransaction[250];

		for (int i = 0; i < many.length; i++) {
			many[i] = entry("t" + i, i, "1", i % 2 == 0 ? EntryKind.MINED : EntryKind.RECEIVED);
		}

		final Screen screen = onTheDashboard(many).click(ACTIVITY).click(RECEIVED);

		assertEquals(100, screen.document().select(ROWS).size());
		screen.click("#rows .ledger__more");
		assertEquals(125, screen.document().select(ROWS).size());
		assertTrue(screen.document().select(".ledger__more").isEmpty());
	}

	@Example
	public void shouldTurnDownInputNoScreenSends() throws Exception {
		final Screen screen = onTheDashboard(entry("aa", 1, "10", EntryKind.RECEIVED));

		assertEquals(500, status(screen, "/action/activity", "filter=NOPE"));
		assertEquals(500, status(screen, "/action/activity-more", "offset=abc"));
		assertEquals(500, status(screen, "/action/activity-more", ""));
		assertEquals(500, status(screen, "/action/export-csv", ""));
		assertEquals(200, status(screen, "/action/activity", "filter=ALL"));
		assertEquals("100.00", settle(rig.open()).click(DASHBOARD).find(TOTAL).text());
	}

	@Example
	public void shouldTurnDownEverythingThatNeedsTheLedgerWhileItIsNotThere() throws Exception {
		final Path file = rig.data().resolve("history.csv");

		rig.hedgehog().unsigned();

		final Screen screen = settle(openedWithTheWalletLeftBehind());

		assertEquals("The ledger is not signed by the Unigrid Foundation",
			screen.find(".preparing__failure").text()
		);
		assertEquals(500, status(screen, "/action/activity-rows", "q=a"));
		assertEquals(500, status(screen, "/action/activity-more", "offset=0"));
		assertEquals(500, status(screen, "/action/export-csv", "path=" + file));
		assertFalse(Files.exists(file));
	}

	@Example
	public void shouldExportTheWholeHistoryWhateverIsFilteredOrSearchedFor() throws Exception {
		final Path file = rig.data().resolve("history.csv");
		final Screen screen = onTheDashboard(entry("aa", 1, "10", EntryKind.RECEIVED),
			entry("bb", 2, "-4", EntryKind.SENT), entry("cc", 3, "2", EntryKind.MINED)
		).click(ACTIVITY).click(SENT).trigger(SEARCH, Map.of("q", "bb"));

		screen.trigger(SAVE, Map.of("path", file.toString()));
		assertEquals("Saved to " + file, screen.find(NOTE).text());
		assertEquals(4, Files.readAllLines(file).size());
	}

	@Example
	public void shouldSayWhenTheHistoryCannotBeSavedWhereTheHostSaid() throws Exception {
		final Screen screen = onTheDashboard(entry("aa", 1, "10", EntryKind.RECEIVED)).click(ACTIVITY);

		screen.trigger(SAVE, Map.of("path", rig.data().toString()));
		assertEquals("Could not save to " + rig.data(), screen.find(NOTE).text());
		assertTrue(screen.find(NOTE).hasClass("export-note--failed"));
	}

	@Example
	public void shouldDrawNoMoreThanTwoYearsOfBars() throws Exception {
		final AddressTransaction[] months = new AddressTransaction[30];

		for (int i = 0; i < months.length; i++) {
			months[i] = entryOn("m" + i, LocalDate.of(2019, 1, 15).plusMonths(i), "1", EntryKind.RECEIVED);
		}

		final Screen screen = onTheDashboard(months);

		assertEquals(24, screen.document().select(".dashboard__bar").size());
		assertEquals("30", screen.find(".stat__value").text());
	}

	@Example
	public void shouldShowTheMostRecentFiveTransactionsOnTheDashboard() throws Exception {
		final AddressTransaction[] eight = new AddressTransaction[8];

		for (int i = 0; i < eight.length; i++) {
			eight[i] = entry("t" + i, i, "1", EntryKind.RECEIVED);
		}

		final Screen screen = onTheDashboard(eight);

		assertEquals(List.of("t7", "t6", "t5", "t4", "t3"),
			screen.document().select(".dashboard__recent .ledger__txid").eachText()
		);
	}

	@Example
	public void shouldSayHowMuchIsStillAwaitingItsMint() throws Exception {
		rig.hedgehog().address(rig.addresses().get(0), "500", entry("aa", 1, "100", EntryKind.RECEIVED));

		assertEquals("Includes 400.00 UGD awaiting mint",
			settle(openedWithTheWalletLeftBehind()).find(".dashboard__awaiting").text()
		);
	}

	@Example
	public void shouldSayNothingIsActiveInAWalletWithoutHistory() throws Exception {
		final Screen screen = settle(openedWithTheWalletLeftBehind());

		assertEquals("0.00", screen.find(TOTAL).text());
		assertEquals("—", screen.find(".stat__value--small").text());
		assertEquals("This wallet has no transactions.", screen.find(".dashboard__recent .ledger__empty").text());
		assertTrue(screen.document().select(".dashboard__awaiting").isEmpty());
	}

	@Example
	public void shouldSayHowFarTheLedgerHasGotWhileHedgehogDownloadsIt() throws Exception {
		rig.hedgehog().fetching(30);

		final Screen screen = openedWithTheWalletLeftBehind();

		for (int i = 0; i < 400 && screen.document().select(".preparing__percent").isEmpty(); i++) {
			Thread.sleep(25);
			screen.trigger("#app", Map.of());
		}

		final Element step = screen.find(".preparing__percent").parent();

		assertEquals("30 %", step.select(".preparing__percent").text());
		assertTrue(step.text().startsWith("Downloading the legacy ledger"), step.text());
	}

	@Example
	public void shouldSayHowFarTheReadingOfTheWalletHistoryHasGot() throws Exception {
		rig.hedgehog().slowedBy(Duration.ofMillis(60));

		final Screen screen = openedWithTheWalletLeftBehind();
		Element reading = null;

		for (int i = 0; i < 400 && reading == null; i++) {
			Thread.sleep(25);
			screen.trigger("#app", Map.of());
			reading = screen.document().select(".preparing__step").stream()
				.filter(step -> step.text().startsWith("Reading wallet history"))
				.filter(step -> !step.select(".preparing__percent").isEmpty()).findFirst().orElse(null);
		}

		assertTrue(reading != null, "The step showed no percentage");
		assertTrue(reading.select(".preparing__percent").text().matches("\\d{1,3} %"), reading.text());
	}
}
