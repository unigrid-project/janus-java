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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.unigrid.janus.core.hedgehog.AddressBalance;
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

	private final HedgehogClient client;
	private final ZoneId zone;
	private final ExecutorService worker = Executors.newSingleThreadExecutor(work -> {
		final Thread thread = new Thread(work, "wallet-ledger");

		thread.setDaemon(true);
		return thread;
	});

	private volatile LedgerState state = LedgerState.IDLE;

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
			state = LedgerState.LOADING;
			worker.execute(() -> state = attempt(wallet));
		}

		return state;
	}

	/** Drops what was read, so that the next load reads the wallet afresh. */
	public synchronized void reset() {
		if (state.phase() != Phase.LOADING) {
			state = LedgerState.IDLE;
		}
	}

	/*
	 * Whatever goes wrong ends as FAILED, so a view waiting on the ledger never waits forever. Only the
	 * wallet file itself can be unreadable; anything after it is Hedgehog's doing.
	 */
	private LedgerState attempt(final Path wallet) {
		final boolean evm = EvmWalletStore.holds(wallet);
		final Collection<String> addresses;

		try {
			addresses = evm ? EvmWalletStore.read(wallet).addresses() : LegacyWallet.addresses(wallet);
		} catch (IllegalArgumentException | UncheckedIOException e) {
			log.warn("The wallet at {} could not be read", wallet, e);
			return LedgerState.failed(e.getMessage(), true);
		}

		try {
			return LedgerState.loaded(evm ? promised(addresses) : read(addresses));
		} catch (RuntimeException e) {
			log.warn("The ledger of the wallet at {} could not be read", wallet, e);
			return LedgerState.failed("Hedgehog stopped answering", false);
		}
	}

	WalletFunds read(final Path backup) {
		return read(LegacyWallet.addresses(backup));
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

	private WalletFunds read(final Collection<String> addresses) {
		final Map<String, Optional<AddressBalance>> balances = new LinkedHashMap<>();
		final Map<String, List<AddressTransaction>> entries = new LinkedHashMap<>();

		for (final String address : addresses) {
			final Optional<AddressBalance> balance = client.balance(address);

			balances.put(address, balance);

			if (balance.map(known -> known.transactionCount() > 0).orElse(false)) {
				entries.put(address, history(address));
			}
		}

		return WalletFunds.of(balances, WalletHistory.of(entries), client.snapshot(), zone);
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
