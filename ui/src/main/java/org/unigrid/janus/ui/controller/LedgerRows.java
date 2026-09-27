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

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.unigrid.janus.core.wallet.WalletTransaction;
import org.unigrid.janus.ui.view.Amounts;
import org.unigrid.janus.ui.view.RowView;
import org.unigrid.janus.ui.view.Times;

/** Transactions of the wallet as ledger lines, headed by month where a month begins. */
final class LedgerRows {
	private LedgerRows() {
	}

	/* The line before a page decides whether the page opens a month, so a month split over two pages
	   is headed once. */
	static List<RowView> of(final List<WalletTransaction> all, final int from, final int to, final int tip,
		final ZoneId zone, final boolean headed) {

		final List<RowView> rows = new ArrayList<>();

		for (int i = from; i < to; i++) {
			final WalletTransaction transaction = all.get(i);
			final YearMonth month = month(transaction, zone);
			final boolean opens = headed && (i == 0 || !month.equals(month(all.get(i - 1), zone)));

			rows.add(row(transaction, opens ? Times.month(month) : null, tip, zone));
		}

		return rows;
	}

	private static YearMonth month(final WalletTransaction transaction, final ZoneId zone) {
		return YearMonth.from(transaction.time().atZone(zone));
	}

	private static RowView row(final WalletTransaction transaction, final String heading, final int tip,
		final ZoneId zone) {

		final String kind = transaction.kind().name().toLowerCase(Locale.ROOT);
		final List<String> addresses = List.copyOf(transaction.addresses());
		final String party = addresses.get(0) + (addresses.size() > 1 ? " +" + (addresses.size() - 1) : "");

		final String confirmations = Amounts.count(tip - transaction.height() + 1L);

		return new RowView(heading, kind, icon(transaction), title(transaction), party,
			Amounts.signed(transaction.amount()), Times.moment(transaction.time(), zone), confirmations,
			Amounts.count(transaction.height()), transaction.txid(), addresses
		);
	}

	private static String icon(final WalletTransaction transaction) {
		return switch (transaction.kind()) {
			case RECEIVED -> "↓";
			case SENT -> "↑";
			case MINED -> "✦";
			case STAKED -> "◈";
		};
	}

	private static String title(final WalletTransaction transaction) {
		final String name = transaction.kind().name();

		return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
	}
}
