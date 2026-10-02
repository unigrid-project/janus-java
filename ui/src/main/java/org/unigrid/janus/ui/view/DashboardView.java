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

package org.unigrid.janus.ui.view;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import org.unigrid.janus.web.action.View;

/**
 * The wallet at a glance: what it holds, how that grew, how its addresses stand and what happened last.
 * The addresses holding its funds open over it on request.
 */
public record DashboardView(String total, String awaitingMint, List<Bar> bars, String firstMonth, String lastMonth,
	String tip, List<Holding> funded, int historyOnly, int neverUsed, String transactions, String active,
	List<RowView> recent, boolean listingFunded, FundedPage fundedPage) implements View {

	/** One month of the balance history, as a share of the fullest month. */
	public record Bar(int percent, String title) {
		public String style() {
			return "height:" + percent + "%";
		}
	}

	/** An address holding funds, whose activity is the history searched for it. */
	public record Holding(String address, String balance, String share, String transactions, String last) {
		public String vals() {
			return Filter.ALL.vals(address);
		}
	}

	/**
	 * One page of the funded addresses whose address holds what was typed, a few at a time. The page asked
	 * for is kept between the first and the last there are.
	 */
	public record FundedPage(List<Holding> shown, int number, int pages, int matches, String query) {
		public static final int SIZE = 5;

		public static FundedPage of(final List<Holding> funded, final String query, final int requested) {
			final String typed = query.strip();
			final String wanted = typed.toLowerCase(Locale.ROOT);
			final List<Holding> matching = funded.stream()
				.filter(holding -> holding.address().toLowerCase(Locale.ROOT).contains(wanted)).toList();
			final int pages = Math.max(1, (matching.size() + SIZE - 1) / SIZE);
			final int number = Math.min(Math.max(1, requested), pages);
			final int from = (number - 1) * SIZE;

			return new FundedPage(matching.subList(from, Math.min(matching.size(), from + SIZE)), number, pages,
				matching.size(), typed
			);
		}

		public boolean paged() {
			return pages > 1;
		}

		public boolean hasPrevious() {
			return number > 1;
		}

		public boolean hasNext() {
			return number < pages;
		}

		public boolean none() {
			return matches == 0;
		}

		public String previousVals() {
			return vals(number - 1);
		}

		public String nextVals() {
			return vals(number + 1);
		}

		/** Which of the matches are shown, as "6–10 of 12". */
		public String range() {
			final int first = (number - 1) * SIZE + 1;
			return first + "–" + (first + shown.size() - 1) + " of " + matches;
		}

		private static String vals(final int page) {
			return "{\"page\":\"" + page + "\"}";
		}
	}

	public int addresses() {
		return funded.size() + historyOnly + neverUsed;
	}

	public String ring() {
		final String funds = share(funded.size());
		final String used = share(funded.size() + historyOnly);

		return "conic-gradient(var(--up) 0 " + funds + "%, var(--accent) " + funds + "% " + used
			+ "%, var(--border-strong) " + used + "% 100%)";
	}

	/* The whole history, so a search left behind by a funded address must not narrow it. */
	public String allVals() {
		return Filter.ALL.vals("");
	}

	private String share(final int count) {
		if (addresses() == 0) {
			return "0";
		}

		return BigDecimal.valueOf(count * 100L).divide(BigDecimal.valueOf(addresses()), 1, RoundingMode.HALF_UP)
			.stripTrailingZeros().toPlainString();
	}

	@Override
	public String template() {
		return "fragments/dashboard :: dashboard";
	}
}
