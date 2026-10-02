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

import java.util.HexFormat;
import net.jqwik.api.Example;
import org.bitcoinj.base.Base58;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LegacyKeyTest {
	private static final String UNCOMPRESSED_TWO = "68QtzUftP6UedWuuhgxsw4jV7TDsTjvyPnqUKvFA6G2LWUipq9J";
	private static final String COMPRESSED_TWO = "PdiUUh8dnXB36B2XcbPdodUX5Ujoj2VDubJwcW1V8DJu6eQQxzNx";
	private static final String TESTNET_TWO = "cMahea7zqjxrtgAbB7LSGbcQUr1uX1ojuat9jZodMN87K7XCyj5v";
	private static final byte[] TWO = new byte[32];

	static {
		TWO[31] = 2;
	}

	@Example
	public void shouldReadACompressedPrivateKeyAndGiveItsAddress() {
		final LegacyKey key = LegacyKey.parse(COMPRESSED_TWO);

		assertTrue(key.compressed());
		assertArrayEquals(TWO, key.secret());
		assertEquals("H78V5Mwegfjmemi2rMuVg93c8AwjirUdQH", key.address());
	}

	@Example
	public void shouldReadAnUncompressedPrivateKeyAndGiveItsAddress() {
		final LegacyKey key = LegacyKey.parse(UNCOMPRESSED_TWO);

		assertFalse(key.compressed());
		assertArrayEquals(TWO, key.secret());
		assertEquals("HS6ofefYfBjXjqaKM4pb54a1SEmAxvGKTi", key.address());
	}

	@Example
	public void shouldRefuseWhatIsNoPrivateKeyOfThisNetwork() {
		final String damaged = COMPRESSED_TWO.substring(0, COMPRESSED_TWO.length() - 1) + "h";

		assertThrows(IllegalArgumentException.class, () -> LegacyKey.parse(TESTNET_TWO));
		assertThrows(IllegalArgumentException.class, () -> LegacyKey.parse(damaged));
		assertThrows(IllegalArgumentException.class, () -> LegacyKey.parse("3QJmnh"));
		assertThrows(IllegalArgumentException.class, () -> LegacyKey.parse(""));
	}

	@Example
	public void shouldRefuseASecretTheCurveDoesNotAllow() {
		final String one = "PdiUUh8dnXB36B2XcbPdodUX5Ujoj2VDubJwcW1V8DJu69XHZWGg";
		final byte[] order = HexFormat.of().parseHex(
			"FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141"
		);

		assertThrows(IllegalArgumentException.class, () -> LegacyKey.parse(one));
		assertThrows(IllegalArgumentException.class, () -> LegacyKey.parse(Base58.encodeChecked(153, order)));
	}

	@Example
	public void shouldZeroTheSecretWhenWiped() {
		final LegacyKey key = LegacyKey.parse(COMPRESSED_TWO);

		key.wipe();
		assertArrayEquals(new byte[32], key.secret());
	}

	@Example
	public void shouldNeverPrintTheSecret() {
		assertEquals("LegacyKey[hidden]", LegacyKey.parse(COMPRESSED_TWO).toString());
	}
}
