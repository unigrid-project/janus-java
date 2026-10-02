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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The text file the legacy daemon's dumpwallet writes: a few comment lines, then a line per key that starts with
 * the private key and ends in a comment with the address. Only the private key is read, as the daemon's
 * importwallet does, so an edited or stripped comment changes nothing.
 */
public final class WalletDump {
	private static final String COMMENT = "#";

	private WalletDump() {
	}

	/** The private keys of the dump. The caller wipes them; a refusal wipes the ones read before it. */
	public static List<LegacyKey> keys(final Path wallet) {
		final List<String> lines = read(wallet);
		final List<LegacyKey> keys = new ArrayList<>();

		try {
			for (int number = 1; number <= lines.size(); number++) {
				final String line = lines.get(number - 1);

				if (!line.isBlank() && !line.startsWith(COMMENT)) {
					keys.add(key(wallet, number, line.split(" ", 2)[0]));
				}
			}

			if (keys.isEmpty()) {
				throw refusal(wallet, "it holds no private keys");
			}

			return keys;
		} catch (IllegalArgumentException e) {
			keys.forEach(LegacyKey::wipe);
			throw e;
		}
	}

	private static List<String> read(final Path wallet) {
		try {
			return Files.readAllLines(wallet, StandardCharsets.ISO_8859_1);
		} catch (IOException e) {
			throw new UncheckedIOException(wallet + " could not be read", e);
		}
	}

	/* The refusal names the line but never the key, and leaves out the cause, which may quote a character of it. */
	private static LegacyKey key(final Path wallet, final int number, final String text) {
		try {
			return LegacyKey.parse(text);
		} catch (IllegalArgumentException _) {
			throw refusal(wallet, "line " + number + " holds no private key");
		}
	}

	private static IllegalArgumentException refusal(final Path wallet, final String reason) {
		return new IllegalArgumentException(wallet + " is not a wallet dump Janus can read: " + reason);
	}
}
