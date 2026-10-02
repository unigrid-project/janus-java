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
import org.unigrid.janus.web.action.View;

/**
 * The wallet at a glance: what it holds, how that grew, how its addresses stand and what happened last.
 * The addresses holding its funds open over it on request.
 */
public record DashboardView(String total, String awaitingMint, List<Bar> bars, String firstMonth, String lastMonth,
	String tip, List<Holding> funded, int historyOnly, int neverUsed, String transactions, String active,
	List<RowView> recent, boolean listingFunded) implements View {

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
