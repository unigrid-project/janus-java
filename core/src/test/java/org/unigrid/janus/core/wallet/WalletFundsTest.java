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
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import net.jqwik.api.Example;
import org.unigrid.janus.core.hedgehog.AddressBalance;
import org.unigrid.janus.core.hedgehog.SignatureStatus;
import org.unigrid.janus.core.hedgehog.SnapshotInfo;
import org.unigrid.janus.core.wallet.WalletTransaction.Kind;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WalletFundsTest {
	private static final SnapshotInfo SNAPSHOT = new SnapshotInfo("ab", 3172666, 1, 1, 1, BigDecimal.ONE,
		BigDecimal.ZERO, Instant.parse("2026-09-14T10:15:30Z"), SignatureStatus.SIGNED
	);

	private static WalletTransaction tx(final String txid, final String when, final String amount, final Kind kind) {
		return new WalletTransaction(txid, Instant.parse(when), 1, new BigDecimal(amount), kind,
			new TreeSet<>(List.of("Hone"))
		);
	}

	private static Optional<AddressBalance> balance(final String address, final String amount, final int count) {
		return Optional.of(new AddressBalance(address, new BigDecimal(amount), count));
	}

	private static void same(final String expected, final BigDecimal actual) {
		assertEquals(0, new BigDecimal(expected).compareTo(actual), () -> expected + " but was " + actual);
	}

	@Example
	public void shouldAddUpWhatCameInWentOutAndWasEarned() {
		final WalletFunds funds = WalletFunds.of(Map.of("Hone", balance("Hone", "72", 3)), List.of(
			tx("c", "2019-03-03T00:00:00Z", "12", Kind.STAKED),
			tx("b", "2019-03-02T00:00:00Z", "-40", Kind.SENT),
			tx("a", "2019-03-01T00:00:00Z", "100", Kind.RECEIVED)
		), SNAPSHOT, ZoneOffset.UTC);

		same("72", funds.total());
		same("72", funds.historyNet());
		same("0", funds.awaitingMint());
		same("100", funds.received());
		same("40", funds.sent());
		same("12", funds.rewards());
	}

	/* An address owed a mint has a balance but no history, so its money is in the total and nowhere else. */
	@Example
	public void shouldShowMoneyAwaitingMintAndCountItsAddressAsHoldingFunds() {
		final Map<String, Optional<AddressBalance>> balances = new LinkedHashMap<>();

		balances.put("Hone", balance("Hone", "10", 1));
		balances.put("Hmint", balance("Hmint", "25", 0));

		final WalletFunds funds = WalletFunds.of(balances,
			List.of(tx("a", "2019-03-01T00:00:00Z", "10", Kind.RECEIVED)), SNAPSHOT, ZoneOffset.UTC
		);

		same("35", funds.total());
		same("25", funds.awaitingMint());
		assertEquals(new AddressBreakdown(2, 0, 0), funds.breakdown());
		assertEquals(List.of("Hmint", "Hone"), funds.funded().stream().map(AddressBalance::address).toList());
	}

	@Example
	public void shouldSortAddressesIntoHoldingHistoryOnlyAndNeverUsed() {
		final Map<String, Optional<AddressBalance>> balances = new LinkedHashMap<>();

		balances.put("Hfunded", balance("Hfunded", "1", 2));
		balances.put("Hspent", balance("Hspent", "0", 4));
		balances.put("Hpool", Optional.empty());
		balances.put("Hzero", balance("Hzero", "0", 0));

		final WalletFunds funds = WalletFunds.of(balances, List.of(), SNAPSHOT, ZoneOffset.UTC);

		assertEquals(new AddressBreakdown(1, 1, 2), funds.breakdown());
		assertEquals(4, funds.breakdown().total());
		assertEquals(List.of("Hfunded"), funds.funded().stream().map(AddressBalance::address).toList());
		assertEquals(List.of(), funds.monthly());
	}

	@Example
	public void shouldCarryTheBalanceThroughMonthsWithoutTransactions() {
		final WalletFunds funds = WalletFunds.of(Map.of("Hone", balance("Hone", "70", 2)), List.of(
			tx("b", "2019-04-20T00:00:00Z", "-30", Kind.SENT),
			tx("a", "2019-01-15T00:00:00Z", "100", Kind.RECEIVED)
		), SNAPSHOT, ZoneOffset.UTC);

		assertEquals(List.of(YearMonth.of(2019, 1), YearMonth.of(2019, 2), YearMonth.of(2019, 3),
			YearMonth.of(2019, 4)), funds.monthly().stream().map(MonthlyBalance::month).toList()
		);
		same("100", funds.monthly().get(2).balance());
		same("70", funds.monthly().get(3).balance());
	}
}
