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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.web3j.crypto.MnemonicUtils;

/** A BIP39 recovery phrase of twelve English words, from which every key of an EVM wallet follows. */
public record Mnemonic(List<String> words) {
	public static final int WORDS = 12;

	private static final int ENTROPY_BYTES = 16;

	public Mnemonic {
		words = List.copyOf(words);

		if (words.size() != WORDS) {
			throw new IllegalArgumentException("A recovery phrase has " + WORDS + " words, not " + words.size());
		}

		words.stream().filter(word -> !MnemonicUtils.getWords().contains(word)).findFirst().ifPresent(word -> {
			throw new IllegalArgumentException("\"" + word + "\" is not a word recovery phrases use");
		});

		if (!MnemonicUtils.validateMnemonic(String.join(" ", words))) {
			throw new IllegalArgumentException("These words do not make a recovery phrase; check their order");
		}
	}

	public static Mnemonic generate(final SecureRandom random) {
		final byte[] entropy = new byte[ENTROPY_BYTES];

		random.nextBytes(entropy);
		return of(entropy);
	}

	public static Mnemonic of(final byte[] entropy) {
		return parse(MnemonicUtils.generateMnemonic(entropy));
	}

	public static Mnemonic parse(final String phrase) {
		return new Mnemonic(Arrays.asList(phrase.strip().toLowerCase(Locale.ROOT).split("\\s+")));
	}

	public String phrase() {
		return String.join(" ", words);
	}

	public byte[] entropy() {
		return MnemonicUtils.generateEntropy(phrase());
	}

	/* The words are the wallet itself, so they must never reach a log line by accident. */
	@Override
	public String toString() {
		return "Mnemonic[" + WORDS + " words]";
	}
}
