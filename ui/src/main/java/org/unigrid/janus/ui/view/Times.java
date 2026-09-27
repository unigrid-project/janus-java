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

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Dates and times as the views show them, in the zone of the computer Janus runs on. */
public final class Times {
	private static final DateTimeFormatter MOMENT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
	private static final DateTimeFormatter SHORT_MONTH = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

	private Times() {
	}

	public static String moment(final Instant instant, final ZoneId zone) {
		return MOMENT.format(instant.atZone(zone));
	}

	public static String date(final Instant instant, final ZoneId zone) {
		return DATE.format(instant.atZone(zone));
	}

	public static String month(final YearMonth month) {
		return MONTH.format(month);
	}

	public static String shortMonth(final YearMonth month) {
		return SHORT_MONTH.format(month);
	}
}
