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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import net.jqwik.api.Example;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MasterKeyTest {
	private static final byte[] CRYPTED = new byte[48];
	private static final byte[] SALT = new byte[8];
	private static final int SHA512 = 0;
	private static final int ROUNDS = 144_109;

	/* An mkey value as the daemon writes it, with no further derivation parameters. */
	private static byte[] value(final byte[] salt, final int method, final int rounds) {
		final ByteBuffer value = ByteBuffer.allocate(1 + CRYPTED.length + 1 + salt.length + 2 * Integer.BYTES + 1);

		value.order(ByteOrder.LITTLE_ENDIAN).put((byte) CRYPTED.length).put(CRYPTED)
			.put((byte) salt.length).put(salt).putInt(method).putInt(rounds).put((byte) 0);

		return value.array();
	}

	private static String refusal(final byte[] value) {
		return assertThrows(IllegalArgumentException.class, () -> MasterKey.parse(value)).getMessage();
	}

	@Example
	public void shouldReadTheKeySaltAndRoundsTheDaemonWrote() {
		final MasterKey key = MasterKey.parse(value(SALT, SHA512, ROUNDS));

		assertEquals(CRYPTED.length, key.crypted().length);
		assertEquals(SALT.length, key.salt().length);
		assertEquals(ROUNDS, key.rounds());
	}

	@Example
	public void shouldRefuseAMethodTheLegacyWalletNeverUsed() {
		assertEquals("its keys are encrypted with method 1, which the legacy wallet never used",
			refusal(value(SALT, 1, ROUNDS))
		);
	}

	@Example
	public void shouldRefuseASaltOrRoundsTheDaemonWouldNotOpen() {
		assertEquals("its master key has a salt of 16 bytes", refusal(value(new byte[16], SHA512, ROUNDS)));
		assertEquals("its master key asks for 0 rounds", refusal(value(SALT, SHA512, 0)));
		assertEquals("its master key asks for 4294967295 rounds", refusal(value(SALT, SHA512, -1)));
	}
}
