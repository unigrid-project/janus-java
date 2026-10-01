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

package org.unigrid.janus.ui;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import org.unigrid.janus.core.hedgehog.AddressTransaction;
import org.unigrid.janus.core.hedgehog.EntryKind;

/** What the flow tests share: ledger entries to hand the Hedgehog stand, and waiting for the window to settle. */
final class FlowSupport {
	private static final String APP = "#app";
	private static final int ASKS = 200;
	private static final long PAUSE_MILLIS = 25;

	private FlowSupport() {
	}

	/** An entry the given number of minutes into 2019, whose height is that number too. */
	static AddressTransaction entry(final String txid, final int minutes, final String amount, final EntryKind kind) {
		return new AddressTransaction(txid, Instant.parse("2019-01-01T00:00:00Z").plusSeconds(minutes * 60L),
			minutes, new BigDecimal(amount), kind
		);
	}

	/** An entry at the start of the day given, as the chain keeps time. */
	static AddressTransaction entryOn(final String txid, final LocalDate day, final String amount,
		final EntryKind kind) {

		return new AddressTransaction(txid, day.atStartOfDay(ZoneOffset.UTC).toInstant(), 1,
			new BigDecimal(amount), kind
		);
	}

	/* The window asks again every two seconds; here it asks as often as it takes. */
	static Screen settle(final Screen screen) throws Exception {
		for (int i = 0; i < ASKS && screen.find(APP).hasAttr("hx-trigger"); i++) {
			Thread.sleep(PAUSE_MILLIS);
			screen.trigger(APP, Map.of());
		}

		return screen;
	}
}
