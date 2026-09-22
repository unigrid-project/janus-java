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

import java.nio.file.Path;
import java.util.Set;
import java.util.SortedSet;
import net.jqwik.api.Example;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LegacyWalletTest {
	@Example
	public void shouldFindTheAddressOfEveryKeyTheWalletHolds() {
		assertEquals(Set.of(
			"HQPjfHhSHs2rt97BrdGjT1BdL3Yg43yhXe",
			"HHNVubwUgnB59iDrDCUwE8wKAg7qwYbTAe",
			"HQrvBngyfKoiy7UzaDnFs711eqR9ysMHx5",
			"HDA4WnF84mdDUd5V2TE514MxVaekt1h6dQ"
		), LegacyWallet.addresses(BerkeleyFileTest.fixture("wallet.dat")));
	}

	@Example
	public void shouldLeaveOutAddressesOnlyInTheAddressBook() {
		final SortedSet<String> addresses = LegacyWallet.addresses(BerkeleyFileTest.fixture("wallet.dat"));

		assertFalse(addresses.contains("HVrjNTDp7PzvXmiZ1Cf4t9AFogZg5BbcAE"));
		assertFalse(addresses.contains("H6X8PLvXQDY3iLaTynKkQ1tUBBJjSZSf23"));
	}

	@Example
	public void shouldNameTheWalletWhoseRecordsItCannotRead() {
		final Path wallet = BerkeleyFileTest.fixture("short-key.dat");
		final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> LegacyWallet.addresses(wallet)
		);

		assertEquals(wallet + " is not a wallet.dat Janus can read: a public key is 10 bytes",
			thrown.getMessage()
		);
	}
}
