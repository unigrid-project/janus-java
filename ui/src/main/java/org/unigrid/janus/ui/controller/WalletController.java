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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.unigrid.janus.core.ChosenWallet;
import org.unigrid.janus.core.WalletChoice;
import org.unigrid.janus.core.hedgehog.HedgehogService;
import org.unigrid.janus.core.hedgehog.HedgehogState;
import org.unigrid.janus.core.wallet.LedgerState;
import org.unigrid.janus.core.wallet.MonthlyBalance;
import org.unigrid.janus.core.wallet.WalletCsv;
import org.unigrid.janus.core.wallet.WalletFunds;
import org.unigrid.janus.core.wallet.WalletLedger;
import org.unigrid.janus.core.wallet.WalletTransaction;
import org.unigrid.janus.core.wallet.WalletTransaction.Kind;
import org.unigrid.janus.ui.view.ActivityView;
import org.unigrid.janus.ui.view.ActivityView.Summary;
import org.unigrid.janus.ui.view.Amounts;
import org.unigrid.janus.ui.view.AppView;
import org.unigrid.janus.ui.view.AppView.Tab;
import org.unigrid.janus.ui.view.DashboardView;
import org.unigrid.janus.ui.view.DashboardView.Bar;
import org.unigrid.janus.ui.view.ExportNoteView;
import org.unigrid.janus.ui.view.Filter;
import org.unigrid.janus.ui.view.ImportView;
import org.unigrid.janus.ui.view.PreparingView;
import org.unigrid.janus.ui.view.PreparingView.Step;
import org.unigrid.janus.ui.view.PreparingView.Step.State;
import org.unigrid.janus.ui.view.RowsView;
import org.unigrid.janus.ui.view.Times;
import org.unigrid.janus.ui.view.WelcomeView;
import org.unigrid.janus.web.action.Action;
import org.unigrid.janus.web.action.Form;
import org.unigrid.janus.web.action.View;

/**
 * The wallet once it is chosen: getting its ledger ready and showing it. Janus has a single window, so
 * the tab, filter and search it shows are kept here rather than carried by every request.
 */
@Slf4j
@ApplicationScoped
public class WalletController {
	private static final int PAGE = 100;
	private static final int RECENT = 5;
	private static final int BARS = 24;
	private static final String PREPARING = "PREPARING";
	private static final String PATH = "path";

	private final ChosenWallet chosen;
	private final WalletChoice choice;
	private final HedgehogService hedgehog;
	private final WalletLedger ledger;
	private final ImportController importer;
	private final ZoneId zone;

	private volatile Tab tab = Tab.DASHBOARD;
	private volatile Filter filter = Filter.ALL;
	private volatile String query = "";
	private volatile String shown = "";

	@Inject
	public WalletController(final ChosenWallet chosen, final WalletChoice choice, final HedgehogService hedgehog,
		final WalletLedger ledger, final ImportController importer) {

		this(chosen, choice, hedgehog, ledger, importer, ZoneId.systemDefault());
	}

	public WalletController(final ChosenWallet chosen, final WalletChoice choice, final HedgehogService hedgehog,
		final WalletLedger ledger, final ImportController importer, final ZoneId zone) {

		this.chosen = chosen;
		this.choice = choice;
		this.hedgehog = hedgehog;
		this.ledger = ledger;
		this.importer = importer;
		this.zone = zone;
	}

	/** What the window opens on: the wallet chosen on an earlier start, or else the welcome card. */
	public View start() {
		shown = "";
		return chosen.remembered().isPresent() ? app() : new WelcomeView();
	}

	@Action("open-wallet")
	public AppView onClickContinue() {
		return open(choice.backup().orElseThrow(() -> new IllegalStateException("No wallet was chosen")));
	}

	/** Opens on the wallet from now on, a wallet.dat backup or an EVM wallet alike, and shows it. */
	public AppView open(final Path wallet) {
		chosen.remember(wallet);
		ledger.reset();
		shown = "";
		tab = Tab.DASHBOARD;
		filter = Filter.ALL;
		query = "";
		return app();
	}

	@Action("wallet-status")
	public AppView onPoll() {
		return app();
	}

	@Action("wallet-retry")
	public AppView onClickRetry() {
		hedgehog.recheck();
		ledger.reset();
		hedgehog.prepare();
		return app();
	}

	@Action("choose-another")
	public ImportView onClickChooseAnother() {
		chosen.forget();
		ledger.reset();
		return importer.onClickImport();
	}

	@Action("dashboard")
	public AppView onClickDashboard() {
		tab = Tab.DASHBOARD;
		return app();
	}

	@Action("activity")
	public AppView onClickActivity(final Form form) {
		tab = Tab.ACTIVITY;

		if (form.has("filter")) {
			filter = Filter.valueOf(form.get("filter"));
		}

		return app();
	}

	@Action("activity-rows")
	public RowsView onSearch(final Form form) {
		query = Objects.requireNonNullElse(form.get("q"), "").strip();
		return rows(loaded(), 0, false);
	}

	@Action("activity-more")
	public RowsView onClickMore(final Form form) {
		return rows(loaded(), Integer.parseInt(form.get("offset")), true);
	}

	@Action("export-csv")
	public ExportNoteView onExport(final Form form) {
		if (!form.has(PATH)) {
			throw new IllegalArgumentException("No file was named");
		}

		final Path file = Path.of(form.get(PATH));

		try {
			WalletCsv.write(file, loaded().transactions());
			return new ExportNoteView("Saved to " + file, false);
		} catch (UncheckedIOException e) {
			log.warn("The history could not be saved to {}", file, e);
			return new ExportNoteView("Could not save to " + file, true);
		}
	}

	private WalletFunds loaded() {
		final WalletFunds funds = ledger.state().funds();

		if (funds == null) {
			throw new IllegalStateException("The wallet is not loaded yet");
		}

		return funds;
	}

	/*
	 * Every look at the window moves the preparation along: Hedgehog is started if nothing has tried yet,
	 * and the ledger is read as soon as Hedgehog is ready. A failure is left alone until Retry is pressed,
	 * so the window does not start Hedgehog over and over on its own.
	 */
	private AppView app() {
		final Path backup = chosen.remembered().orElseThrow(() -> new IllegalStateException("No wallet was chosen"));
		HedgehogState service = hedgehog.state();

		if (service.phase() == HedgehogState.Phase.IDLE) {
			service = hedgehog.prepare();
		}

		LedgerState read = ledger.state();

		if (service.phase() == HedgehogState.Phase.READY && read.phase() == LedgerState.Phase.IDLE) {
			read = ledger.load(backup);
		}

		if (read.phase() == LedgerState.Phase.LOADED) {
			final View screen = tab == Tab.DASHBOARD ? dashboard(read.funds()) : activity(read.funds());

			return new AppView(tab, chip(read.funds()), false, arriving(screen), screen);
		}

		final String failure = service.phase() == HedgehogState.Phase.FAILED ? service.reason()
			: read.phase() == LedgerState.Phase.FAILED ? read.reason() : null;
		final View screen = new PreparingView(steps(service, hedgehog.downloadedHedgehog(), read), failure,
			read.unreadable()
		);

		return new AppView(tab, PREPARING, failure == null, arriving(screen), screen);
	}

	/* A screen drawn again where it already stood, by a poll or a filter, must not move in again. */
	private boolean arriving(final View screen) {
		final String now = tab + ":" + screen.getClass().getSimpleName();
		final boolean arriving = !now.equals(shown);

		shown = now;
		return arriving;
	}

	/* Hedgehog is only a step of its own when this run had to download it. */
	private static List<Step> steps(final HedgehogState service, final boolean hedgehogDownloaded,
		final LedgerState read) {

		final boolean installing = service.phase() == HedgehogState.Phase.DOWNLOADING_HEDGEHOG;
		final boolean ready = service.phase() == HedgehogState.Phase.READY;
		final boolean fetching = service.phase() == HedgehogState.Phase.FETCHING;
		final State starting = ready || fetching ? State.DONE : installing ? State.PENDING : State.ACTIVE;
		final State downloading = ready ? State.DONE : fetching ? State.ACTIVE : State.PENDING;
		final List<Step> steps = new ArrayList<>();

		if (hedgehogDownloaded) {
			steps.add(new Step("Downloading Hedgehog (~90 MB)", installing ? State.ACTIVE : State.DONE,
				installing ? service.progress() : null
			));
		}

		steps.add(new Step("Starting Hedgehog", starting));
		steps.add(new Step("Downloading the legacy ledger (~314 MB)", downloading,
			fetching ? service.progress() : null
		));
		steps.add(new Step("Reading wallet history", read.phase() == LedgerState.Phase.LOADED ? State.DONE
			: ready ? State.ACTIVE : State.PENDING
		));
		return steps;
	}

	private static String chip(final WalletFunds funds) {
		return "LEDGER FROZEN · BLOCK " + Amounts.count(funds.snapshot().tipHeight()) + " · SIGNED";
	}

	private DashboardView dashboard(final WalletFunds funds) {
		final List<MonthlyBalance> months = funds.monthly().subList(Math.max(0, funds.monthly().size() - BARS),
			funds.monthly().size()
		);
		final BigDecimal fullest = months.stream().map(MonthlyBalance::balance).max(Comparator.naturalOrder())
			.orElse(BigDecimal.ZERO);
		final List<WalletTransaction> transactions = funds.transactions();
		final int tip = funds.snapshot().tipHeight();

		return new DashboardView(Amounts.plain(funds.total()),
			funds.awaitingMint().signum() > 0 ? Amounts.plain(funds.awaitingMint()) : null,
			months.stream().map(month -> bar(month, fullest)).toList(),
			months.isEmpty() ? "" : Times.shortMonth(months.get(0).month()),
			months.isEmpty() ? "" : Times.shortMonth(months.get(months.size() - 1).month()),
			Amounts.count(tip), funds.breakdown().withFunds(), funds.breakdown().historyOnly(),
			funds.breakdown().neverUsed(), Amounts.count(transactions.size()), active(transactions),
			LedgerRows.of(transactions, 0, Math.min(RECENT, transactions.size()), tip, zone, false)
		);
	}

	private static Bar bar(final MonthlyBalance month, final BigDecimal fullest) {
		return new Bar(percent(month.balance(), fullest), Times.shortMonth(month.month()) + ": "
			+ Amounts.plain(month.balance()) + " UGD"
		);
	}

	/* A month with anything in it keeps a sliver of a bar, so it never reads as empty. */
	private static int percent(final BigDecimal balance, final BigDecimal fullest) {
		if (fullest.signum() <= 0 || balance.signum() <= 0) {
			return 0;
		}

		final BigDecimal share = balance.multiply(BigDecimal.valueOf(100)).divide(fullest, 0, RoundingMode.HALF_UP);

		return Math.max(1, share.intValue());
	}

	private String active(final List<WalletTransaction> transactions) {
		if (transactions.isEmpty()) {
			return "—";
		}

		return Times.date(transactions.get(transactions.size() - 1).time(), zone) + " – "
			+ Times.date(transactions.get(0).time(), zone);
	}

	private ActivityView activity(final WalletFunds funds) {
		return new ActivityView(filter, query, List.of(
			new Summary(Filter.RECEIVED, "Received", Amounts.signed(funds.received()), "received"),
			new Summary(Filter.SENT, "Sent", Amounts.signed(funds.sent().negate()), "sent"),
			new Summary(Filter.REWARDS, "Rewards", Amounts.signed(funds.rewards()), "rewards"),
			new Summary(Filter.ALL, "Net", Amounts.signed(funds.historyNet()), "net")
		), rows(funds, 0, false), ExportNoteView.NONE);
	}

	private RowsView rows(final WalletFunds funds, final int offset, final boolean continuation) {
		final List<WalletTransaction> shown = funds.transactions().stream().filter(matching()).toList();
		final int end = Math.min(shown.size(), offset + PAGE);

		return new RowsView(LedgerRows.of(shown, offset, end, funds.snapshot().tipHeight(), zone, true),
			end < shown.size() ? end : null, filter != Filter.ALL || !query.isEmpty(), continuation
		);
	}

	private Predicate<WalletTransaction> matching() {
		final String sought = query.toLowerCase(Locale.ROOT);

		return transaction -> kinded(transaction.kind()) && (sought.isEmpty() || mentions(transaction, sought));
	}

	private static boolean mentions(final WalletTransaction transaction, final String sought) {
		return Stream.concat(Stream.of(transaction.txid()), transaction.addresses().stream())
			.anyMatch(text -> text.toLowerCase(Locale.ROOT).contains(sought));
	}

	private boolean kinded(final Kind kind) {
		return switch (filter) {
			case ALL -> true;
			case RECEIVED -> kind == Kind.RECEIVED;
			case SENT -> kind == Kind.SENT;
			case REWARDS -> kind == Kind.MINED || kind == Kind.STAKED;
		};
	}
}
