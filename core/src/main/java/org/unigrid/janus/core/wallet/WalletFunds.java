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
 * whatever the two disagree by is money awaiting its mint.
 */
public record WalletFunds(BigDecimal total, BigDecimal historyNet, BigDecimal awaitingMint, BigDecimal received,
	BigDecimal sent, BigDecimal rewards, AddressBreakdown breakdown, List<WalletTransaction> transactions,
	List<MonthlyBalance> monthly, SnapshotInfo snapshot) {

	public static WalletFunds of(final Map<String, Optional<AddressBalance>> balances,
		final List<WalletTransaction> transactions, final SnapshotInfo snapshot, final ZoneId zone) {

		final BigDecimal total = balances.values().stream().flatMap(Optional::stream).map(AddressBalance::balance)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		final BigDecimal historyNet = sum(transactions, transaction -> true);

		return new WalletFunds(total, historyNet, total.subtract(historyNet).max(BigDecimal.ZERO),
			sum(transactions, of(Kind.RECEIVED)), sum(transactions, of(Kind.SENT)).negate(),
			sum(transactions, of(Kind.MINED).or(of(Kind.STAKED))), breakdown(balances), transactions,
			monthly(transactions, zone), snapshot
		);
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
