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

package org.unigrid.janus.core.legacy;

import java.util.Arrays;
import java.util.HexFormat;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.Size;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LegacyAddressTest {
	private static final byte[] UNCOMPRESSED = HexFormat.of().parseHex(
		"04b36556d4e6822708431cce73eaf447a0ec89a8ae6eb48aa412cb5b56bb6410"
		+ "acaa7cda7000e270b9900eb77667bb421728cab77e720c7ca2118150430c4f418a"
	);
	private static final byte[] COMPRESSED = HexFormat.of().parseHex(
		"02b36556d4e6822708431cce73eaf447a0ec89a8ae6eb48aa412cb5b56bb6410ac"
	);

	@Example
	public void shouldEncodeTheZeroHashAsHedgehogDoes() {
		assertEquals("H6X8PLvXQDY3iLaTynKkQ1tUBBJjSZSf23", LegacyAddress.encode(new byte[20]));
	}

	@Example
	public void shouldEncodeTheAllOnesHashAsHedgehogDoes() {
		final byte[] hash = new byte[20];

		Arrays.fill(hash, (byte) 0xff);
		assertEquals("HVrjNTDp7PzvXmiZ1Cf4t9AFogZg5BbcAE", LegacyAddress.encode(hash));
	}

	@Example
	public void shouldHashAnUncompressedKeyIntoItsAddress() {
		assertEquals("HQPjfHhSHs2rt97BrdGjT1BdL3Yg43yhXe", LegacyAddress.of(UNCOMPRESSED));
	}

	@Example
	public void shouldGiveTheCompressedFormOfAKeyAnAddressOfItsOwn() {
		assertEquals("HHNVubwUgnB59iDrDCUwE8wKAg7qwYbTAe", LegacyAddress.of(COMPRESSED));
	}

	@Property(tries = 100)
	public void shouldAlwaysGiveAnAddressStartingWithH(@ForAll @Size(20) final byte[] hash) {
		final String address = LegacyAddress.encode(hash);

		assertTrue(address.startsWith("H") && address.length() == 34, address);
	}
}
