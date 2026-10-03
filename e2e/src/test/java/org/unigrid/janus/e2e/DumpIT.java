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
import com.microsoft.playwright.Page;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import net.jqwik.api.Example;
import org.unigrid.janus.core.hedgehog.AddressTransaction;
import org.unigrid.janus.core.hedgehog.EntryKind;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** The wallet dump the legacy daemon's dumpwallet writes, picked in the host's dialog and sealed behind a new phrase. */
public class DumpIT extends BrowserTest {
	private static final Page.WaitForSelectorOptions LOADED = new Page.WaitForSelectorOptions().setTimeout(20_000);
	private static final String CARD = "main > .card";
	private static final String CHOOSE_FILE = "[data-choose-file]";
	private static final String CONTINUE = CARD + " .button--primary";
	private static final Pattern SELECTED = Pattern.compile("\\bchoice--selected\\b");

	private static AddressTransaction received(final String txid, final int minutes, final String amount) {
		return new AddressTransaction(txid, Instant.parse("2019-01-01T00:00:00Z").plusSeconds(minutes * 60L),
			minutes, new BigDecimal(amount), EntryKind.RECEIVED
		);
	}

	private Path pick(final Path file) {
		rig().window().picking(file);
		page().click("[hx-post='/action/import']");
		page().click(CHOOSE_FILE);
		return file;
	}

	private void tapTheWordsBack(final List<String> words) {
		for (final String word : words) {
			final Locator tile = page().locator("button.tile:not([disabled])")
				.filter(new Locator.FilterOptions().setHasText(Pattern.compile("^" + word + "$"))).first();

			tile.click();
			assertThat(page().locator(".tray .tile--picked").last()).containsText(word);
		}
	}

	@Example
	public void shouldTakeTheDumpPickedInTheDialogOfTheHostAndWarnAboutIt() throws Exception {
		final Path dump = pick(rig().keepDumpElsewhere());

		assertThat(page().locator(CHOOSE_FILE)).hasClass(SELECTED);
		assertThat(page().locator(CHOOSE_FILE + " .choice__note")).hasText(dump.toString());
		assertThat(page().locator(CARD + " > .step__note").last()).containsText("private keys unprotected");
		assertThat(page().locator(CONTINUE)).hasAttribute("hx-post", "/action/import-dump");
		assertEquals(List.of("choose-file:Choose a wallet.dat or wallet dump"), rig().window().commands());
		assertEquals(false, Files.exists(rig().backups()));
	}

	@Example
	public void shouldReachTheDashboardThroughANewPhraseWithTheFundsOfTheKeys() throws Exception {
		final List<String> addresses = rig().dumpAddresses();

		rig().hedgehog().address(addresses.get(0), "30", received("aa", 1, "30"))
			.address(addresses.get(5), "12", received("bb", 2, "12"));
		pick(rig().keepDumpElsewhere());
		page().click(CONTINUE);
		assertThat(page().locator(".phrase__word")).hasCount(12);

		final List<String> words = page().locator(".phrase__word > span:not(.phrase__n)").allTextContents();

		page().click("[hx-post='/action/create-verify']");
		tapTheWordsBack(words);
		page().click(CONTINUE);
		assertThat(page().locator(CARD + " .step__note")).containsText("imported keys");
		assertThat(page().locator("input[type=password][name=password]")).hasCount(1);
		assertThat(page().locator("input[type=password][name=repeat]")).hasCount(1);
		choosePassword("correct horse", "correct horse");
		page().click(CONTINUE);
		page().waitForSelector(".dashboard__total", LOADED);

		assertThat(page().locator(".dashboard__total")).hasText("42.00");
	}

	@Example
	public void shouldNotAskForAPasswordWhenAWalletDatIsImported() throws Exception {
		rig().hedgehog().address(rig().addresses().get(0), "30", received("aa", 1, "30"));
		pick(rig().keepWalletElsewhere());
		assertThat(page().locator(CONTINUE)).hasAttribute("hx-post", "/action/open-wallet");
		page().click(CONTINUE);
		page().waitForSelector(".dashboard__total", LOADED);

		assertThat(page().locator(".dashboard__total")).hasText("30.00");
		assertThat(page().locator("input[type=password]")).hasCount(0);
		assertEquals(false, Files.exists(rig().wallets()));
	}

	@Example
	public void shouldSayWhatIsWrongWithAFileThatIsNoWallet() throws Exception {
		pick(Files.writeString(rig().keepDumpElsewhere().resolveSibling("notes.txt"), "no keys here"));

		assertThat(page().locator(CARD + " .step__error")).containsText("line 1 holds no private key");
		assertThat(page().locator(CONTINUE)).isDisabled();
	}
}
