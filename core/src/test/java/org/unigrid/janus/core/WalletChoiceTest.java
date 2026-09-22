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

package org.unigrid.janus.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import net.jqwik.api.Example;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WalletChoiceTest {
	@Example
	public void shouldHaveNothingChosenToBeginWith() {
		assertEquals(Optional.empty(), new WalletChoice().chosen());
	}

	@Example
	public void shouldRememberTheWalletThatWasChosen() throws IOException {
		final Path wallet = Files.createTempFile("wallet", ".dat");
		final WalletChoice choice = new WalletChoice();

		try {
			choice.choose(wallet);
			assertEquals(Optional.of(wallet), choice.chosen());
		} finally {
			Files.delete(wallet);
		}
	}

	@Example
	public void shouldRefuseAPathWithoutAWalletFile() {
		final WalletChoice choice = new WalletChoice();
		final Path missing = Path.of("/nowhere/wallet.dat");
		final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> choice.choose(missing)
		);

		assertTrue(thrown.getMessage().contains(missing.toString()), thrown.getMessage());
		assertEquals(Optional.empty(), choice.chosen());
	}
}
