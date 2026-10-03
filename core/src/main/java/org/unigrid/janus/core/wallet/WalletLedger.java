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

package org.unigrid.janus.core.wallet;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.unigrid.janus.core.hedgehog.AddressBalance;
import org.unigrid.janus.core.evm.EvmWallet;
import org.unigrid.janus.core.evm.EvmWalletStore;
import org.unigrid.janus.core.hedgehog.AddressTransaction;
import org.unigrid.janus.core.hedgehog.HedgehogClient;
import org.unigrid.janus.core.hedgehog.HedgehogService;
import org.unigrid.janus.core.legacy.LegacyWallet;
import org.unigrid.janus.core.wallet.LedgerState.Phase;

/**
 * The chosen wallet as the frozen ledger records it, or for an EVM wallet as the mint storage spork
 * promises it, read from Hedgehog once and kept, since nothing on a frozen chain can change it afterwards.
 * Reading takes a request per address and page, so it runs on a thread of its own and the views ask how
 * far along it is.
 */
@Slf4j
@ApplicationScoped
public class WalletLedger {
	private static final int PAGE = 1000;
	private static final int SIMULTANEOUS = 4;
	private static final int UNTRACKED = -1;

	private final HedgehogClient client;
	private final ZoneId zone;
	private final ExecutorService worker = Executors.newSingleThreadExecutor(
		Thread.ofPlatform().name("wallet-ledger").daemon().factory()
	);

	private volatile LedgerState state = LedgerState.IDLE;
	private Future<?> reading = CompletableFuture.completedFuture(null);
	private int generation;

	@Inject
	public WalletLedger(final HedgehogService hedgehog) {
		this(hedgehog.client(), ZoneId.systemDefault());
	}

	public WalletLedger(final HedgehogClient client, final ZoneId zone) {
		this.client = client;
		this.zone = zone;
	}

	public LedgerState state() {
		return state;
	}

	/** Starts reading the wallet unless that is under way or done; after a failure it reads again. */
	public synchronized LedgerState load(final Path wallet) {
		if (state.phase() == Phase.IDLE || state.phase() == Phase.FAILED) {
			final int read = ++generation;

			state = LedgerState.LOADING;
			reading = worker.submit(() -> settle(read, attempt(wallet, read)));
		}

		return state;
	}

	/**
	 * Drops what was read and stops a read under way, so that the next load reads afresh, whichever wallet it
	 * is given. A read stopped halfway would otherwise land later and stand in for the wallet chosen since.
	 */
	public synchronized void reset() {
		reading.cancel(true);
		generation++;
		state = LedgerState.IDLE;
	}

	private synchronized void settle(final int read, final LedgerState outcome) {
		if (read == generation) {
			state = outcome;
		}
	}

	/*
	 * Whatever goes wrong ends as FAILED, so a view waiting on the ledger never waits forever. Only the
	 * wallet file itself can be unreadable; anything after it is Hedgehog's doing.
	 */
	private LedgerState attempt(final Path wallet, final int read) {
		final boolean promised;
		final Collection<String> addresses;

		try {
			if (EvmWalletStore.holds(wallet)) {
				final EvmWallet evm = EvmWalletStore.read(wallet);

				promised = evm.legacy() == null;
				addresses = promised ? evm.addresses() : evm.legacy().addresses();
			} else {
				promised = false;
				addresses = LegacyWallet.addresses(wallet);
			}
		} catch (IllegalArgumentException | UncheckedIOException e) {
			log.warn("The wallet at {} could not be read", wallet, e);
			return LedgerState.failed(e.getMessage(), true);
		}

		try {
			return LedgerState.loaded(promised ? promised(addresses) : read(addresses, read));
		} catch (RuntimeException e) {
			if (!Thread.currentThread().isInterrupted()) {
				log.warn("The ledger of the wallet at {} could not be read", wallet, e);
			}

			return LedgerState.failed("Hedgehog stopped answering", false);
		}
	}

	WalletFunds read(final Path backup) {
		return read(LegacyWallet.addresses(backup), UNTRACKED);
	}

	/*
	 * An EVM address has no past on the legacy chain; all it holds is what the mint storage spork promises
	 * it, and so all of that awaits its mint. The spork may spell an address in either case.
	 */
	private WalletFunds promised(final Collection<String> addresses) {
		final Map<String, BigDecimal> owed = new HashMap<>();
		final Map<String, Optional<AddressBalance>> balances = new LinkedHashMap<>();

		client.mints().forEach(mint -> owed.merge(mint.address().toLowerCase(Locale.ROOT), mint.amount(),
			BigDecimal::add
		));

		addresses.forEach(address -> balances.put(address, Optional.ofNullable(
			owed.get(address.toLowerCase(Locale.ROOT))).map(amount -> new AddressBalance(address, amount, 0))
		));

		return WalletFunds.of(balances, List.of(), client.snapshot(), zone);
	}

	/** What Hedgehog said of one address: its balance, and its history when it has any. */
	private record Answer(Optional<AddressBalance> balance, List<AddressTransaction> history) {
	}

	/*
	 * A wallet can hold hundreds of thousands of addresses and Hedgehog is asked about each, so a few askers share
	 * the work, each taking the next address that has not been asked about. Every answer lands at the place of its
	 * address, which keeps the result in the order of the wallet however the answers arrive.
	 */
	private WalletFunds read(final Collection<String> addresses, final int read) {
		final List<String> asked = List.copyOf(addresses);
		final Answer[] answers = new Answer[asked.size()];
		final AtomicInteger next = new AtomicInteger();
		final AtomicInteger answered = new AtomicInteger();

		try (ExecutorService askers = Executors.newFixedThreadPool(SIMULTANEOUS,
			Thread.ofPlatform().name("hedgehog-asker-", 1).daemon().factory())) {

			final Runnable asker = () -> {
				for (int i = next.getAndIncrement(); i < answers.length; i = next.getAndIncrement()) {
					answers[i] = ask(asked.get(i));
					progress(read, answered.incrementAndGet(), answers.length);
				}
			};

			await(IntStream.range(0, SIMULTANEOUS).<Future<?>>mapToObj(n -> askers.submit(asker)).toList(),
				next);
		}

		final Map<String, Optional<AddressBalance>> balances = new LinkedHashMap<>();
		final Map<String, List<AddressTransaction>> entries = new LinkedHashMap<>();

		for (int i = 0; i < answers.length; i++) {
			balances.put(asked.get(i), answers[i].balance());

			if (answers[i].history() != null) {
				entries.put(asked.get(i), answers[i].history());
			}
		}

		return WalletFunds.of(balances, WalletHistory.of(entries), client.snapshot(), zone);
	}

	private Answer ask(final String address) {
		final Optional<AddressBalance> balance = client.balance(address);
		final boolean used = balance.map(known -> known.transactionCount() > 0).orElse(false);

		return new Answer(balance, used ? history(address) : null);
	}

	/* The first failure stops the others from taking another address, and is the one that is reported. */
	private static void await(final List<Future<?>> running, final AtomicInteger next) {
		try {
			for (final Future<?> asker : running) {
				asker.get();
			}
		} catch (ExecutionException e) {
			next.set(Integer.MAX_VALUE);

			if (e.getCause() instanceof RuntimeException failure) {
				throw failure;
			}

			throw new IllegalStateException(e.getCause());
		} catch (InterruptedException e) {
			next.set(Integer.MAX_VALUE);
			Thread.currentThread().interrupt();
			throw new IllegalStateException("The read was stopped", e);
		}
	}

	/* Only a change of the whole percent is worth saying, which is a few hundred times for any wallet. */
	private synchronized void progress(final int read, final int answered, final int total) {
		final int percent = (int) (100L * answered / total);

		if (read == generation && !Integer.valueOf(percent).equals(state.progress())) {
			state = LedgerState.loading(percent);
		}
	}

	private List<AddressTransaction> history(final String address) {
		final List<AddressTransaction> all = new ArrayList<>();

		for (int offset = 0;; offset += PAGE) {
			final List<AddressTransaction> page = client.transactions(address, offset, PAGE);

			all.addAll(page);

			if (page.size() < PAGE) {
				return all;
			}
		}
	}

	@PreDestroy
	public void stop() {
		worker.shutdownNow();
	}
}
