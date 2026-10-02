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
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import net.jqwik.api.Example;
import org.unigrid.janus.core.hedgehog.AddressTransaction;
import org.unigrid.janus.core.hedgehog.EntryKind;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** The wallet dump the legacy daemon's dumpwallet writes, picked in the dialog of the host and opened. */
public class DumpIT extends BrowserTest {
	private static final Page.WaitForSelectorOptions LOADED = new Page.WaitForSelectorOptions().setTimeout(15_000);
	private static final String CHOOSE_FILE = "[data-choose-file]";
	private static final Pattern SELECTED = Pattern.compile("\\bchoice--selected\\b");

	private static AddressTransaction received(final String txid, final int minutes, final String amount) {
		return new AddressTransaction(txid, Instant.parse("2019-01-01T00:00:00Z").plusSeconds(minutes * 60L),
			minutes, new BigDecimal(amount), EntryKind.RECEIVED
		);
	}

	private Path pickTheDump() throws Exception {
		final Path dump = rig().keepDumpElsewhere();

		rig().window().picking(dump);
		page().click("[hx-post='/action/import']");
		page().click(CHOOSE_FILE);
		return dump;
	}

	@Example
	public void shouldTakeTheDumpPickedInTheDialogOfTheHost() throws Exception {
		final Path dump = pickTheDump();

		assertThat(page().locator(CHOOSE_FILE)).hasClass(SELECTED);
		assertThat(page().locator(CHOOSE_FILE + " .choice__note")).hasText(dump.toString());
		assertThat(page().locator("main > .card .button--primary")).isEnabled();
		assertEquals(List.of("choose-file:Choose a wallet.dat or wallet dump"), rig().window().commands());
	}

	@Example
	public void shouldReachTheDashboardWithTheFundsOfTheKeysInTheDump() throws Exception {
		final List<String> addresses = rig().dumpAddresses();

		rig().hedgehog().address(addresses.get(0), "30", received("aa", 1, "30"))
			.address(addresses.get(5), "12", received("bb", 2, "12"));
		pickTheDump();
		page().click("[hx-post='/action/open-wallet']");
		page().waitForSelector(".dashboard__total", LOADED);

		assertThat(page().locator(".dashboard__total")).hasText("42.00");
	}
}
