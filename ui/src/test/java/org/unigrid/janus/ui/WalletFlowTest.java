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

import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.unigrid.janus.core.hedgehog.AddressTransaction;
import org.unigrid.janus.core.hedgehog.EntryKind;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.unigrid.janus.ui.FlowSupport.entry;
import static org.unigrid.janus.ui.FlowSupport.settle;

public class WalletFlowTest {
	private static final String ACTIVITY = "button.app__tab[hx-post=/action/activity]";

	private ControlCenterRig rig;

	@BeforeTry
	public void serve() throws Exception {
		rig = new ControlCenterRig();
	}

	@AfterTry
	public void stop() throws Exception {
		rig.close();
	}

	private Screen continueWithTheWalletLeftBehind() throws Exception {
		rig.leaveWalletBehind();
		return Screen.open(rig).click("[hx-post=/action/import]").click("[hx-post=/action/import-found]")
			.click("[hx-post=/action/open-wallet]");
	}

	@Example
	public void shouldGoFromContinueToTheDashboardAndOpenThereNextTime() throws Exception {
		rig.hedgehog().address(rig.addresses().get(0), "480", entry("aa", 1, "1020", EntryKind.RECEIVED),
			entry("bb", 9, "-540", EntryKind.SENT)
		);

		final Screen screen = continueWithTheWalletLeftBehind();

		assertEquals("PREPARING", screen.find(".app__chip").text());
		assertEquals("480.00", settle(screen).find(".dashboard__total").text());
		assertEquals("LEDGER FROZEN · BLOCK 3,172,666 · SIGNED", screen.find(".app__chip").text());
		assertEquals("480.00", settle(Screen.open(rig)).find(".dashboard__total").text());
	}

	@Example
	public void shouldStartOverWhenTheRememberedCopyIsGone() throws Exception {
		settle(continueWithTheWalletLeftBehind());
		Files.delete(rig.chosen().remembered().orElseThrow());

		assertEquals("Welcome to Unigrid", Screen.open(rig).find("main > .card h1").text());
		assertTrue(rig.chosen().remembered().isEmpty());
	}

	@Example
	public void shouldFilterSearchAndPageThroughTheActivity() throws Exception {
		final AddressTransaction[] many = new AddressTransaction[130];

		for (int i = 0; i < many.length; i++) {
			many[i] = entry("t" + i, i, "1", i % 2 == 0 ? EntryKind.MINED : EntryKind.RECEIVED);
		}

		rig.hedgehog().address(rig.addresses().get(0), "130", many);

		final Screen screen = settle(continueWithTheWalletLeftBehind()).click(ACTIVITY);

		assertEquals(100, screen.document().select("#rows details").size());
		screen.click("#rows .ledger__more");
		assertEquals(130, screen.document().select("#rows details").size());
		assertTrue(screen.document().select(".ledger__more").isEmpty());

		screen.click(".filter[hx-vals*=REWARDS]");
		assertEquals("Rewards", screen.find(".filter--active").text());
		assertEquals(65, screen.document().select("#rows details").size());

		screen.trigger("input[name=q]", Map.of("q", "t12"));
		assertEquals(List.of("t128", "t126", "t124", "t122", "t120", "t12"),
			screen.document().select("#rows .ledger__txid").eachText()
		);
	}

	@Example
	public void shouldEchoASearchSafelyAndMatchNothingWithIt() throws Exception {
		rig.hedgehog().address(rig.addresses().get(0), "1", entry("aa", 1, "1", EntryKind.RECEIVED));

		final Screen screen = settle(continueWithTheWalletLeftBehind()).click(ACTIVITY);

		screen.trigger("input[name=q]", Map.of("q", "<\""));
		assertEquals("No transactions match your filter.", screen.find("#rows .ledger__empty").text());

		final HttpResponse<String> redrawn = screen.client().submit("/action/activity", "filter=ALL");

		assertTrue(redrawn.body().contains("value=\"&lt;&quot;\""), redrawn.body());
	}

	@Example
	public void shouldFailOnAnUnsignedLedgerAndRecoverOnRetry() throws Exception {
		rig.hedgehog().unsigned();

		final Screen screen = settle(continueWithTheWalletLeftBehind());

		assertEquals("The ledger is not signed by the Unigrid Foundation",
			screen.find(".preparing__failure").text()
		);
		rig.hedgehog().signed();
		assertEquals("0.00",
			settle(screen.click("[hx-post=/action/wallet-retry]")).find(".dashboard__total").text()
		);
	}

	@Example
	public void shouldExportTheHistoryWhereTheHostSaid() throws Exception {
		rig.hedgehog().address(rig.addresses().get(0), "1", entry("aa", 1, "1", EntryKind.RECEIVED));

		final Screen screen = settle(continueWithTheWalletLeftBehind()).click(ACTIVITY);
		final Path file = rig.data().resolve("history.csv");

		screen.trigger("[data-save-file]", Map.of("path", file.toString()));
		assertEquals("Saved to " + file, screen.find("#export-note").text());
		assertEquals(2, Files.readAllLines(file).size());
	}

	@Example
	public void shouldKnowEveryActionTheWalletCanSend() throws Exception {
		rig.hedgehog().address(rig.addresses().get(0), "1", entry("aa", 1, "1", EntryKind.RECEIVED));

		final Screen screen = settle(continueWithTheWalletLeftBehind());
		final Set<String> sent = new HashSet<>();
		final Deque<Element> reached = new ArrayDeque<>(List.of(screen.document(),
			Jsoup.parseBodyFragment(screen.client().submit("/action/activity", "").body())
		));

		while (!reached.isEmpty()) {
			for (final Element control : reached.pop().select("[hx-post]")) {
				final String action = control.attr("hx-post");

				if (sent.add(action) && !action.equals("/action/choose-another")) {
					final HttpResponse<String> response = screen.client().submit(action, "");

					assertNotEquals(404, response.statusCode(), action);
					reached.push(Jsoup.parseBodyFragment(response.body()));
				}
			}
		}

		assertTrue(sent.containsAll(Set.of("/action/dashboard", "/action/activity", "/action/activity-rows",
			"/action/export-csv")), sent::toString
		);
		assertFalse(sent.isEmpty());
	}
}
