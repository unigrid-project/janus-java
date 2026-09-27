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

import java.security.SecureRandom;
import net.jqwik.api.Example;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EvmWalletTest {
	static final SeedVault VAULT = new SeedVault(new SecureRandom(), SeedVaultTest.CHEAP);
	static final Mnemonic PHRASE = Mnemonic.parse(MnemonicTest.ABANDON);

	@Example
	public void shouldHoldTheFirstAddressesOfItsPhrase() {
		final EvmWallet wallet = EvmWallet.create(PHRASE, "pw", VAULT);

		assertEquals(EvmAccounts.addresses(PHRASE, EvmWallet.ADDRESSES), wallet.addresses());
		assertEquals("m/44'/60'/0'/0", wallet.path());
	}

	@Example
	public void shouldGiveBackItsPhraseForThePassword() {
		assertEquals(PHRASE, EvmWallet.create(PHRASE, "pw", VAULT).mnemonic("pw", VAULT));
	}

	@Example
	public void shouldKeepItsPhraseFromAnotherPassword() {
		final EvmWallet wallet = EvmWallet.create(PHRASE, "pw", VAULT);

		assertThrows(WrongPassword.class, () -> wallet.mnemonic("other", VAULT));
	}
}
