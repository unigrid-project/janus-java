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
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import net.jqwik.api.Example;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FormattingTest {
	@Example
	public void shouldShowAtLeastTwoAndAtMostEightDecimals() {
		assertEquals("1,020.50", Amounts.plain(new BigDecimal("1020.5")));
		assertEquals("0.12345678", Amounts.plain(new BigDecimal("0.12345678")));
		assertEquals("7.00", Amounts.plain(new BigDecimal("7.00000000")));
		assertEquals("0.00", Amounts.plain(BigDecimal.ZERO));
	}

	@Example
	public void shouldSignWhatMovedWithARealMinus() {
		assertEquals("+120.00", Amounts.signed(new BigDecimal("120")));
		assertEquals("−540.25", Amounts.signed(new BigDecimal("-540.25")));
		assertEquals("0.00", Amounts.signed(BigDecimal.ZERO));
	}

	@Example
	public void shouldGroupCounts() {
		assertEquals("3,172,666", Amounts.count(3172666));
	}

	@Example
	public void shouldWriteDatesInEnglishAndTheGivenZone() {
		final Instant moment = Instant.parse("2019-03-01T10:05:00Z");

		assertEquals("1 Mar 2019, 10:05", Times.moment(moment, ZoneOffset.UTC));
		assertEquals("1 Mar 2019", Times.date(moment, ZoneOffset.UTC));
		assertEquals("1 Mar 2019, 11:05", Times.moment(moment, ZoneOffset.ofHours(1)));
		assertEquals("March 2019", Times.month(YearMonth.of(2019, 3)));
		assertEquals("Mar 2019", Times.shortMonth(YearMonth.of(2019, 3)));
	}
}
