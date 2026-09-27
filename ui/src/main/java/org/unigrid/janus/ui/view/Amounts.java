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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Amounts of UGD as people read them: grouped, with cents always and satoshis only when there are any. */
public final class Amounts {
	private static final String MINUS = "−";
	private static final String PLUS = "+";
	private static final String PATTERN = "#,##0.00######";

	private Amounts() {
	}

	public static String plain(final BigDecimal amount) {
		return format().format(amount.abs());
	}

	public static String signed(final BigDecimal amount) {
		return (amount.signum() > 0 ? PLUS : amount.signum() < 0 ? MINUS : "") + plain(amount);
	}

	public static String count(final long count) {
		return String.format(Locale.ENGLISH, "%,d", count);
	}

	/* DecimalFormat is not thread safe, and the views are drawn on the server's threads, so each use has its own. */
	private static DecimalFormat format() {
		return new DecimalFormat(PATTERN, DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	}
}
