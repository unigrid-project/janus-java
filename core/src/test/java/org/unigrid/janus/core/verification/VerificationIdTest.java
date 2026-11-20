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

package org.unigrid.janus.core.verification;

import java.security.SecureRandom;
import java.time.Instant;
import net.jqwik.api.Example;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VerificationIdTest {
	@Example
	public void shouldBe32LowerCaseHexCharactersFreshEachTime() {
		final SecureRandom random = new SecureRandom();
		final VerificationId first = VerificationId.random(random);

		assertTrue(first.value().matches("[0-9a-f]{32}"), first.value());
		assertNotEquals(first, VerificationId.random(random));
	}

	@Example
	public void shouldRefuseAnythingElse() {
		for (final String bad : new String[] {"", "00112233445566778899aabbccddeef",
			"00112233445566778899AABBCCDDEEFF", "00112233445566778899aabbccddeeff0",
			"g0112233445566778899aabbccddeeff"}) {

			assertThrows(IllegalArgumentException.class, () -> new VerificationId(bad), bad);
		}

		assertThrows(IllegalArgumentException.class, () -> new VerificationId(null));
	}

	@Example
	public void shouldBeSignedAsVerifyThenTheIdThenTheHolderThenTheSecondsSinceTheEpoch() {
		final VerificationId id = new VerificationId("00112233445566778899aabbccddeeff");

		assertEquals("verify 00112233445566778899aabbccddeeff PERSON 1763500000",
			SignedRequest.text(id, Holder.PERSON, Instant.ofEpochSecond(1_763_500_000))
		);
		assertEquals("verify 00112233445566778899aabbccddeeff COMPANY 1763500000",
			SignedRequest.text(id, Holder.COMPANY, Instant.ofEpochSecond(1_763_500_000))
		);
	}
}
