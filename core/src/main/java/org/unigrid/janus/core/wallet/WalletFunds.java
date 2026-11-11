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

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.unigrid.janus.core.hedgehog.AddressBalance;
import org.unigrid.janus.core.hedgehog.SnapshotInfo;
import org.unigrid.janus.core.wallet.WalletTransaction.Kind;

/**
 * Everything the wallet's views show, worked out once. The total comes from the balances Hedgehog
 * reports, which include mints the chain has not made yet; the history cannot know about those, so
 * whatever the two disagree by is money awaiting its mint. The snapshot addresses are every legacy address of
 * the wallet Hedgehog knows, in the order of the wallet: those in the snapshot, funded or empty, and those owed
 * a pending mint, each with its snapshot balance plus what it is owed. A request for verification lists all of
 * them, so the board sees the holder's whole past on the chain and not only what is left of it.
 */
public record WalletFunds(BigDecimal total, BigDecimal historyNet, BigDecimal awaitingMint, BigDecimal received,
	BigDecimal sent, BigDecimal rewards, AddressBreakdown breakdown, List<AddressBalance> funded,
	List<AddressBalance> snapshotAddresses, List<WalletTransaction> transactions, List<MonthlyBalance> monthly,
	SnapshotInfo snapshot) {

	/**
	 * Works out the funds of a legacy wallet from what Hedgehog said of each of its addresses.
	 *
	 * @param balances every address of the wallet in its order, with the balance Hedgehog reported, or empty when
	 *	Hedgehog knows nothing of the address
	 * @param transactions the wallet's history, newest first
	 * @param snapshot the snapshot the balances come from
	 * @param zone the zone the months of the history are counted in
	 * @return the wallet's funds, with every address Hedgehog knows as a snapshot address
	 */
	public static WalletFunds of(final Map<String, Optional<AddressBalance>> balances,
		final List<WalletTransaction> transactions, final SnapshotInfo snapshot, final ZoneId zone) {

		return build(balances, known(balances), transactions, snapshot, zone);
	}

	/**
	 * Works out the funds of an EVM wallet from the mints the mint storage spork promises its addresses. Those
	 * addresses were never on the legacy chain, so the wallet has no snapshot addresses, whatever it is owed.
	 *
	 * @param balances every address of the wallet in its order, with what it is owed, or empty when it is owed
	 *	nothing
	 * @param snapshot the snapshot Hedgehog holds
	 * @param zone the zone the months of the history are counted in
	 * @return the wallet's funds, with no history and no snapshot addresses
	 */
	public static WalletFunds promised(final Map<String, Optional<AddressBalance>> balances,
		final SnapshotInfo snapshot, final ZoneId zone) {

		return build(balances, List.of(), List.of(), snapshot, zone);
	}

	/** Works out the funds from the balances and history, with the snapshot addresses as they were found. */
	private static WalletFunds build(final Map<String, Optional<AddressBalance>> balances,
		final List<AddressBalance> snapshotAddresses, final List<WalletTransaction> transactions,
		final SnapshotInfo snapshot, final ZoneId zone) {

		final BigDecimal total = balances.values().stream().flatMap(Optional::stream).map(AddressBalance::balance)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		final BigDecimal historyNet = sum(transactions, transaction -> true);

		return new WalletFunds(total, historyNet, total.subtract(historyNet).max(BigDecimal.ZERO),
			sum(transactions, of(Kind.RECEIVED)), sum(transactions, of(Kind.SENT)).negate(),
			sum(transactions, of(Kind.MINED).or(of(Kind.STAKED))), breakdown(balances), funded(balances),
			snapshotAddresses, transactions, monthly(transactions, zone), snapshot
		);
	}

	/**
	 * Every address Hedgehog knows, in the order of the wallet. Hedgehog answers for an address that is in the
	 * snapshot or owed a pending mint, and for no other, and the map keeps the order the wallet gave.
	 */
	private static List<AddressBalance> known(final Map<String, Optional<AddressBalance>> balances) {
		return balances.values().stream().flatMap(Optional::stream).toList();
	}

	private static List<AddressBalance> funded(final Map<String, Optional<AddressBalance>> balances) {
		return balances.values().stream().flatMap(Optional::stream).filter(known -> known.balance().signum() > 0)
			.sorted(Comparator.comparing(AddressBalance::balance).reversed()).toList();
	}

	private static Predicate<WalletTransaction> of(final Kind kind) {
		return transaction -> transaction.kind() == kind;
	}

	private static BigDecimal sum(final List<WalletTransaction> transactions, final Predicate<WalletTransaction> which) {
		return transactions.stream().filter(which).map(WalletTransaction::amount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static AddressBreakdown breakdown(final Map<String, Optional<AddressBalance>> balances) {
		int withFunds = 0;
		int historyOnly = 0;

		for (final Optional<AddressBalance> balance : balances.values()) {
			if (balance.map(known -> known.balance().signum() > 0).orElse(false)) {
				withFunds++;
			} else if (balance.map(known -> known.transactionCount() > 0).orElse(false)) {
				historyOnly++;
			}
		}

		return new AddressBreakdown(withFunds, historyOnly, balances.size() - withFunds - historyOnly);
	}

	private static List<MonthlyBalance> monthly(final List<WalletTransaction> transactions, final ZoneId zone) {
		final List<MonthlyBalance> months = new ArrayList<>();

		if (transactions.isEmpty()) {
			return months;
		}

		final List<WalletTransaction> oldestFirst = transactions.reversed();
		final YearMonth last = month(transactions.getFirst(), zone);
		BigDecimal running = BigDecimal.ZERO;
		int next = 0;

		for (YearMonth month = month(oldestFirst.getFirst(), zone); !month.isAfter(last);
			month = month.plusMonths(1)) {

			while (next < oldestFirst.size() && !month(oldestFirst.get(next), zone).isAfter(month)) {
				running = running.add(oldestFirst.get(next++).amount());
			}

			months.add(new MonthlyBalance(month, running));
		}

		return months;
	}

	private static YearMonth month(final WalletTransaction transaction, final ZoneId zone) {
		return YearMonth.from(transaction.time().atZone(zone));
	}
}
