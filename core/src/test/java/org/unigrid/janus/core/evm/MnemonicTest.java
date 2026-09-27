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
import java.util.HexFormat;
import net.jqwik.api.Example;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MnemonicTest {
	static final String ABANDON = "abandon abandon abandon abandon abandon abandon abandon abandon abandon "
		+ "abandon abandon about";

	@Example
	public void shouldTurnTheBip39VectorIntoItsEntropy() {
		assertArrayEquals(new byte[16], Mnemonic.parse(ABANDON).entropy());
	}

	@Example
	public void shouldSpellEntropyAsTheBip39VectorDoes() {
		final byte[] entropy = HexFormat.of().parseHex("7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f");

		assertEquals("legal winner thank year wave sausage worth useful legal winner thank yellow",
			Mnemonic.of(entropy).phrase()
		);
	}

	@Example
	public void shouldReadWordsTypedLooselyAsTheSamePhrase() {
		assertEquals(Mnemonic.parse(ABANDON), Mnemonic.parse("  " + ABANDON.toUpperCase().replace(" ", " \n ")));
	}

	@Example
	public void shouldGenerateTwelveWordsThatReadBack() {
		final Mnemonic generated = Mnemonic.generate(new SecureRandom());

		assertEquals(Mnemonic.WORDS, generated.words().size());
		assertEquals(generated, Mnemonic.parse(generated.phrase()));
	}

	@Example
	public void shouldRefuseAPhraseOfTheWrongLength() {
		final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> Mnemonic.parse("abandon about")
		);

		assertEquals("A recovery phrase has 12 words, not 2", thrown.getMessage());
	}

	@Example
	public void shouldNameAWordOutsideTheWordList() {
		final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> Mnemonic.parse(ABANDON.replace("about", "unigrid"))
		);

		assertEquals("\"unigrid\" is not a word recovery phrases use", thrown.getMessage());
	}

	@Example
	public void shouldRefuseWordsWhoseChecksumFails() {
		final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> Mnemonic.parse(ABANDON.replace("about", "abandon"))
		);

		assertEquals("These words do not make a recovery phrase; check their order", thrown.getMessage());
	}

	@Example
	public void shouldKeepTheWordsOutOfItsDescription() {
		assertFalse(Mnemonic.parse(ABANDON).toString().contains("abandon"));
	}
}
