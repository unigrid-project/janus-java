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

package org.unigrid.janus.ui.controller;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import org.unigrid.janus.core.ChosenWallet;
import org.unigrid.janus.core.DataDirectory;
import org.unigrid.janus.core.WalletBackup;
import org.unigrid.janus.core.WalletChoice;
import org.unigrid.janus.core.hedgehog.AddressTransaction;
import org.unigrid.janus.core.hedgehog.EntryKind;
import org.unigrid.janus.core.hedgehog.HedgehogService;
import org.unigrid.janus.core.hedgehog.HedgehogStand;
import org.unigrid.janus.core.wallet.WalletLedger;
import org.unigrid.janus.ui.view.ActivityView;
import org.unigrid.janus.ui.view.AppView;
import org.unigrid.janus.ui.view.DashboardView;
import org.unigrid.janus.ui.view.ExportNoteView;
import org.unigrid.janus.ui.view.ImportView;
import org.unigrid.janus.ui.view.PreparingView;
import org.unigrid.janus.ui.view.RowView;
import org.unigrid.janus.ui.view.RowsView;
import org.unigrid.janus.ui.view.WelcomeView;
import org.unigrid.janus.web.action.ActionExtension;
import org.unigrid.janus.web.action.Actions;
import org.unigrid.janus.web.action.Form;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WalletControllerTest {
	private static final String FIXTURE = "/org/unigrid/janus/core/legacy/plain-wallet";

	private Path home;
	private HedgehogStand stand;
	private HedgehogService hedgehog;
	private WalletLedger ledger;
	private ChosenWallet chosen;
	private WalletChoice choice;
	private WalletController controller;
	private List<String> addresses;

	@BeforeTry
	public void prepare() throws IOException {
		home = Files.createTempDirectory("janus");
		stand = HedgehogStand.start().signed();
		hedgehog = stand.service();
		ledger = new WalletLedger(stand.client(), ZoneOffset.UTC);
		chosen = new ChosenWallet(home.resolve("chosen"));
		choice = new WalletChoice(new WalletBackup(home.resolve("backups"), Clock.systemUTC()));
		controller = new WalletController(chosen, choice, hedgehog,
			ledger, new ImportController(new DataDirectory(home), choice), ZoneOffset.UTC
		);

		try (InputStream in = getClass().getResourceAsStream(FIXTURE + ".addresses")) {
			addresses = new String(in.readAllBytes()).lines().toList();
		}
	}

	@AfterTry
	public void clean() throws IOException {
		ledger.stop();
		hedgehog.stop();
		stand.close();

		try (Stream<Path> paths = Files.walk(home)) {
			for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(path);
			}
		}
	}

	private void chooseTheFixture() throws IOException {
		final Path wallet = home.resolve("wallet.dat");

		try (InputStream in = getClass().getResourceAsStream(FIXTURE + ".dat")) {
			Files.copy(in, wallet);
		}

		choice.choose(wallet);
	}

	private static AddressTransaction entry(final String txid, final int minutes, final String amount,
		final EntryKind kind) {

		return new AddressTransaction(txid, Instant.parse("2019-01-01T00:00:00Z").plusSeconds(minutes * 60L),
			minutes, new BigDecimal(amount), kind
		);
	}

	private AppView settle(final AppView first) throws InterruptedException {
		AppView view = first;

		for (int i = 0; i < 200 && view.preparing(); i++) {
			Thread.sleep(25);
			view = controller.onPoll();
		}

		return view;
	}

	@Example
	public void shouldOpenOnWelcomeWithNothingRemembered() {
		assertInstanceOf(WelcomeView.class, controller.start());
	}

	@Example
	public void shouldRefuseToOpenAWalletThatWasNeverChosen() {
		assertThrows(IllegalStateException.class, () -> controller.onClickContinue());
	}

	@Example
	public void shouldRememberTheWalletAndShowItsDashboardOnceLoaded() throws Exception {
		chooseTheFixture();
		stand.address(addresses.get(0), "480", entry("aa", 1, "1020", EntryKind.RECEIVED),
			entry("bb", 40, "-540", EntryKind.SENT)
		);

		final AppView opened = controller.onClickContinue();

		assertTrue(opened.preparing());
		assertEquals(choice.backup(), chosen.remembered());

		final AppView loaded = settle(opened);
		final DashboardView dashboard = assertInstanceOf(DashboardView.class, loaded.screen());

		assertEquals("LEDGER FROZEN · BLOCK 3,172,666 · SIGNED", loaded.chip());
		assertEquals("480.00", dashboard.total());
		assertEquals("2", dashboard.transactions());
		assertEquals(List.of("bb", "aa"), dashboard.recent().stream().map(RowView::txid).toList());
		assertInstanceOf(AppView.class, controller.start());
	}

	@Example
	public void shouldNarrowTheActivityByKindAndSearch() throws Exception {
		chooseTheFixture();
		stand.address(addresses.get(0), "480", entry("aa", 1, "1020", EntryKind.RECEIVED),
			entry("bb", 40, "-540", EntryKind.SENT), entry("cc", 70, "5", EntryKind.STAKED)
		);
		settle(controller.onClickContinue());

		final ActivityView sent = assertInstanceOf(ActivityView.class,
			controller.onClickActivity(Form.parse("filter=SENT")).screen()
		);

		assertEquals(List.of("bb"), sent.rows().rows().stream().map(RowView::txid).toList());

		final RowsView searched = controller.onSearch(Form.parse("q=CC"));

		assertTrue(searched.rows().isEmpty());
		assertTrue(searched.filtered());
		assertEquals(List.of("cc"), assertInstanceOf(ActivityView.class,
			controller.onClickActivity(Form.parse("filter=ALL")).screen()
		).rows().rows().stream().map(RowView::txid).toList());
	}

	@Example
	public void shouldHandTheLedgerOutAHundredAtATimeWithoutRepeatingAMonth() throws Exception {
		final AddressTransaction[] many = new AddressTransaction[250];

		for (int i = 0; i < many.length; i++) {
			many[i] = entry("t" + i, i, "1", EntryKind.MINED);
		}

		chooseTheFixture();
		stand.address(addresses.get(0), "250", many);
		settle(controller.onClickContinue());

		final RowsView first = controller.onSearch(Form.parse("q="));
		final RowsView second = controller.onClickMore(Form.parse("offset=100"));
		final RowsView third = controller.onClickMore(Form.parse("offset=200"));

		assertEquals(100, first.rows().size());
		assertEquals(200, second.nextOffset());
		assertNull(third.nextOffset());
		assertEquals(50, third.rows().size());
		assertTrue(second.continuation());
		assertEquals("January 2019", first.rows().get(0).heading());
		assertNull(second.rows().get(0).heading());
	}

	@Example
	public void shouldSayWhyItCouldNotLoadAndOfferRetry() throws Exception {
		stand.close();
		stand = HedgehogStand.start().unsigned();
		hedgehog.stop();
		hedgehog = stand.service();
		ledger.stop();
		ledger = new WalletLedger(stand.client(), ZoneOffset.UTC);
		controller = new WalletController(chosen, choice, hedgehog, ledger,
			new ImportController(new DataDirectory(home), choice), ZoneOffset.UTC
		);
		chooseTheFixture();

		final AppView failed = settle(controller.onClickContinue());
		final PreparingView panel = assertInstanceOf(PreparingView.class, failed.screen());

		assertFalse(failed.preparing());
		assertEquals("The ledger is not signed by the Unigrid Foundation", panel.failure());

		stand.signed();
		assertInstanceOf(DashboardView.class, settle(controller.onClickRetry()).screen());
	}

	@Example
	public void shouldForgetTheWalletWhenAnotherIsToBeChosen() throws Exception {
		chooseTheFixture();
		controller.onClickContinue();
		assertInstanceOf(ImportView.class, controller.onClickChooseAnother());
		assertTrue(chosen.remembered().isEmpty());
	}

	@Example
	public void shouldSaveTheWholeHistoryAsCsv() throws Exception {
		chooseTheFixture();
		stand.address(addresses.get(0), "1", entry("aa", 1, "1", EntryKind.RECEIVED));
		settle(controller.onClickContinue());

		final Path file = home.resolve("history.csv");
		final ExportNoteView note = controller.onExport(Form.parse("path=" + file));

		assertEquals("Saved to " + file, note.message());
		assertEquals(2, Files.readAllLines(file).size());
		assertTrue(controller.onExport(Form.parse("path=" + home.resolve("missing/dir/h.csv"))).failed());
	}

	@Example
	public void shouldBeFoundAndWiredByTheContainer() {
		try (SeContainer container = SeContainerInitializer.newInstance().initialize()) {
			final Actions actions = Actions.discovered(
				container.getBeanManager().getExtension(ActionExtension.class)
			);

			for (final String action : List.of("open-wallet", "wallet-status", "wallet-retry", "choose-another",
				"dashboard", "activity", "activity-rows", "activity-more", "export-csv")) {
				assertTrue(actions.knows(action), action);
			}
		}
	}
}
