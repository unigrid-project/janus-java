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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.unigrid.janus.core.hedgehog.AddressTransaction;
import org.unigrid.janus.core.hedgehog.EntryKind;
import org.unigrid.janus.core.wallet.WalletTransaction.Kind;

/**
 * Turns the history Hedgehog keeps per address into the history of the wallet. A payment usually sends
 * its change back to another address of the same wallet, and counting the two apart would show money
 * leaving and arriving that never did.
 */
public final class WalletHistory {
	private static final Comparator<WalletTransaction> NEWEST_FIRST = Comparator
		.comparing(WalletTransaction::time).reversed()
		.thenComparing(Comparator.comparingInt(WalletTransaction::height).reversed())
		.thenComparing(WalletTransaction::txid);

	private WalletHistory() {
	}

	public static List<WalletTransaction> of(final Map<String, List<AddressTransaction>> entriesByAddress) {
		final Map<String, WalletTransaction> byTxid = new LinkedHashMap<>();

		entriesByAddress.forEach((address, entries) -> entries.forEach(entry ->
			byTxid.merge(entry.transaction(), single(address, entry), WalletHistory::combine)
		));

		return byTxid.values().stream().sorted(NEWEST_FIRST).toList();
	}

	private static WalletTransaction single(final String address, final AddressTransaction entry) {
		return new WalletTransaction(entry.transaction(), entry.time(), entry.height(), entry.amount(),
			kind(entry.kind(), entry.amount()), new TreeSet<>(List.of(address))
		);
	}

	private static WalletTransaction combine(final WalletTransaction one, final WalletTransaction other) {
		final BigDecimal amount = one.amount().add(other.amount());
		final TreeSet<String> addresses = new TreeSet<>(one.addresses());

		addresses.addAll(other.addresses());
		return new WalletTransaction(one.txid(), one.time(), one.height(), amount,
			rank(one.kind(), other.kind(), amount), addresses
		);
	}

	private static Kind kind(final EntryKind kind, final BigDecimal amount) {
		return switch (kind) {
			case STAKED -> Kind.STAKED;
			case MINED -> Kind.MINED;
			default -> bySign(amount);
		};
	}

	/* A reward is what the transaction was about, whatever else moved in it, so it outranks the sign. */
	private static Kind rank(final Kind one, final Kind other, final BigDecimal amount) {
		if (one == Kind.STAKED || other == Kind.STAKED) {
			return Kind.STAKED;
		}

		if (one == Kind.MINED || other == Kind.MINED) {
			return Kind.MINED;
		}

		return bySign(amount);
	}

	/* Money that only moved between the wallet's own addresses still cost a fee, so it counts as sent. */
	private static Kind bySign(final BigDecimal amount) {
		return amount.signum() > 0 ? Kind.RECEIVED : Kind.SENT;
	}
}
