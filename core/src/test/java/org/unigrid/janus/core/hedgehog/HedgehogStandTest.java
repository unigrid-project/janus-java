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

package org.unigrid.janus.core.hedgehog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HedgehogStandTest {
	private static final String ADDRESS = "H6stkFpUvrJMqWigLfPwp3onsvzZzZuF7V";

	private HedgehogStand stand;

	@BeforeTry
	public void open() throws Exception {
		stand = HedgehogStand.start().signed();
	}

	@AfterTry
	public void close() {
		stand.close();
	}

	private static AddressTransaction entry(final int height) {
		return new AddressTransaction("tx" + height, Instant.parse("2019-01-01T00:00:00Z").plusSeconds(height),
			height, BigDecimal.ONE, EntryKind.RECEIVED
		);
	}

	@Example
	public void shouldAnswerForTheAddressesItWasGiven() {
		stand.address(ADDRESS, "2.5", entry(1), entry(2));

		assertEquals(Optional.of(new AddressBalance(ADDRESS, new BigDecimal("2.5"), 2)),
			stand.client().balance(ADDRESS)
		);
		assertEquals(List.of(entry(1), entry(2)), stand.client().transactions(ADDRESS, 0, 1000));
		assertEquals(Optional.empty(), stand.client().balance("HelseWhere"));
	}

	@Example
	public void shouldHandOutHistoryAPageAtATime() {
		final AddressTransaction[] many = new AddressTransaction[1500];

		for (int i = 0; i < many.length; i++) {
			many[i] = entry(i);
		}

		stand.address(ADDRESS, "1500", many);
		assertEquals(1000, stand.client().transactions(ADDRESS, 0, 1000).size());
		assertEquals(entry(1000), stand.client().transactions(ADDRESS, 1000, 1000).get(0));
		assertEquals(500, stand.client().transactions(ADDRESS, 1000, 1000).size());
	}

	@Example
	public void shouldBeReusedByTheServiceRatherThanStarted() {
		final HedgehogService service = stand.service();

		service.prepare();
		assertEquals(HedgehogState.Phase.READY, HedgehogServiceTest.settle(service).phase());
		service.stop();
	}

	@Example
	public void shouldFailTheHistoryOfABrokenAddress() {
		stand.address(ADDRESS, "1", entry(1)).broken(ADDRESS);
		assertThrows(RuntimeException.class, () -> stand.client().transactions(ADDRESS, 0, 1000));
	}
}
