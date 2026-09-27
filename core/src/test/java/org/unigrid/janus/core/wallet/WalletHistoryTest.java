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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import net.jqwik.api.Example;
import org.unigrid.janus.core.hedgehog.AddressTransaction;
import org.unigrid.janus.core.hedgehog.EntryKind;
import org.unigrid.janus.core.wallet.WalletTransaction.Kind;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WalletHistoryTest {
	private static final Instant MARCH = Instant.parse("2019-03-01T10:00:00Z");

	private static AddressTransaction entry(final String txid, final int height, final String amount,
		final EntryKind kind) {

		return new AddressTransaction(txid, MARCH.plusSeconds(height * 60L), height, new BigDecimal(amount), kind);
	}

	private static Map<String, List<AddressTransaction>> of(final Object... addressAndEntries) {
		final Map<String, List<AddressTransaction>> map = new LinkedHashMap<>();

		for (int i = 0; i < addressAndEntries.length; i += 2) {
			@SuppressWarnings("unchecked")
			final List<AddressTransaction> entries = (List<AddressTransaction>) addressAndEntries[i + 1];

			map.put((String) addressAndEntries[i], entries);
		}

		return map;
	}

	@Example
	public void shouldNetASendWithItsChangeIntoOneTransaction() {
		final List<WalletTransaction> history = WalletHistory.of(of(
			"Hsender", List.of(entry("aa", 5, "-540", EntryKind.SENT)),
			"Hchange", List.of(entry("aa", 5, "60", EntryKind.RECEIVED))
		));

		assertEquals(List.of(new WalletTransaction("aa", MARCH.plusSeconds(300), 5, new BigDecimal("-480"),
			Kind.SENT, new TreeSet<>(List.of("Hchange", "Hsender"))
		)), history);
	}

	@Example
	public void shouldCallASendToItselfASend() {
		final List<WalletTransaction> history = WalletHistory.of(of(
			"Hone", List.of(entry("bb", 7, "-10", EntryKind.SENT)),
			"Htwo", List.of(entry("bb", 7, "10", EntryKind.RECEIVED))
		));

		assertEquals(Kind.SENT, history.get(0).kind());
		assertEquals(0, BigDecimal.ZERO.compareTo(history.get(0).amount()));
	}

	@Example
	public void shouldLetStakingAndMiningOutrankTheSign() {
		final List<WalletTransaction> history = WalletHistory.of(of(
			"Hone", List.of(entry("st", 9, "-100", EntryKind.SENT), entry("mi", 3, "50", EntryKind.MINED)),
			"Htwo", List.of(entry("st", 9, "102", EntryKind.STAKED))
		));

		assertEquals(List.of(Kind.STAKED, Kind.MINED), history.stream().map(WalletTransaction::kind).toList());
	}

	@Example
	public void shouldPutTheNewestFirstAndBreakTiesByHeightThenTxid() {
		final List<WalletTransaction> history = WalletHistory.of(of("Hone", List.of(
			entry("b", 1, "1", EntryKind.RECEIVED), entry("a", 1, "1", EntryKind.RECEIVED),
			entry("c", 2, "1", EntryKind.RECEIVED)
		)));

		assertEquals(List.of("c", "a", "b"), history.stream().map(WalletTransaction::txid).toList());
	}

	@Example
	public void shouldHaveNothingForAWalletWithNoEntries() {
		assertEquals(List.of(), WalletHistory.of(Map.of()));
	}
}
