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

package org.unigrid.janus.core.evm;

import java.math.BigInteger;
import java.util.HexFormat;
import java.util.List;
import net.jqwik.api.Example;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EvmAccountsTest {
	@Example
	public void shouldFindTheAccountsOtherEthereumWalletsFind() {
		assertEquals(List.of(
			"0x9858EfFD232B4033E47d90003D41EC34EcaEda94",
			"0x6Fac4D18c912343BF86fa7049364Dd4E424Ab9C0",
			"0xb6716976A3ebe8D39aCEB04372f22Ff8e6802D7A"
		), EvmAccounts.addresses(Mnemonic.parse(MnemonicTest.ABANDON), 3));
	}

	@Example
	public void shouldGiveThePrivateKeyOfTheFirstAccountAsOtherEthereumWalletsDo() {
		final byte[] key = EvmAccounts.privateKey(Mnemonic.parse(MnemonicTest.ABANDON), 0);

		assertEquals("1ab42cc412b618bdea3a599e3c9bae199ebf030895b039e9db1e30dafb12b727",
			HexFormat.of().formatHex(key)
		);
	}

	@Example
	public void shouldGiveAPrivateKeyThatIsTheOneBehindTheAddressOfTheSameIndex() {
		final Mnemonic mnemonic = Mnemonic.parse(MnemonicTest.ABANDON);

		for (int index = 0; index < 3; index++) {
			final byte[] key = EvmAccounts.privateKey(mnemonic, index);
			final ECKeyPair pair = ECKeyPair.create(new BigInteger(1, key));
			final String address = Keys.toChecksumAddress(Keys.getAddress(pair));

			assertEquals(EvmAccounts.addresses(mnemonic, 3).get(index), address);
			assertEquals(32, key.length);
		}
	}

	@Example
	public void shouldDeriveNothingWhenAskedForNoAddresses() {
		assertTrue(EvmAccounts.addresses(Mnemonic.parse(MnemonicTest.ABANDON), 0).isEmpty());
	}
}
