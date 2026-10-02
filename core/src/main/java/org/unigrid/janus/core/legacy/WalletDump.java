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
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.bitcoinj.base.Base58;
import org.bitcoinj.crypto.ECKey;

/**
 * The text file the legacy daemon's dumpwallet writes: a few comment lines, then a line per key that starts with
 * the private key and ends in a comment with the address. The address is worked out from the private key, as the
 * daemon's importwallet does, so an edited or stripped comment changes nothing.
 */
public final class WalletDump {
	private static final String FIRST_LINE = "# Wallet dump created by";
	private static final String COMMENT = "#";
	private static final int SECRET_VERSION = 153;
	private static final int UNCOMPRESSED_SIZE = 1 + 32;
	private static final int COMPRESSED_SIZE = UNCOMPRESSED_SIZE + 1;

	private WalletDump() {
	}

	/* A file that cannot be read is no dump; reading it as a wallet.dat then reports why it cannot be read. */
	public static boolean holds(final Path wallet) {
		try (Stream<String> lines = Files.lines(wallet, StandardCharsets.ISO_8859_1)) {
			return lines.findFirst().filter(first -> first.startsWith(FIRST_LINE)).isPresent();
		} catch (IOException | UncheckedIOException _) {
			return false;
		}
	}

	public static SortedSet<String> addresses(final Path wallet) {
		final List<String> lines = read(wallet);
		final SortedSet<String> addresses = new TreeSet<>();

		for (int number = 1; number <= lines.size(); number++) {
			final String line = lines.get(number - 1);

			if (!line.isBlank() && !line.startsWith(COMMENT)) {
				addresses.add(address(wallet, number, line.split(" ", 2)[0]));
			}
		}

		if (addresses.isEmpty()) {
			throw refusal(wallet, "it holds no private keys");
		}

		return addresses;
	}

	private static List<String> read(final Path wallet) {
		try {
			return Files.readAllLines(wallet, StandardCharsets.ISO_8859_1);
		} catch (IOException e) {
			throw new UncheckedIOException(wallet + " could not be read", e);
		}
	}

	/* The refusal names the line but never the key, and leaves out the cause, which may quote a character of it. */
	private static String address(final Path wallet, final int number, final String key) {
		try {
			return LegacyAddress.of(publicKey(key));
		} catch (IllegalArgumentException _) {
			throw refusal(wallet, "line " + number + " holds no private key");
		}
	}

	private static byte[] publicKey(final String key) {
		final byte[] decoded = Base58.decodeChecked(key);

		try {
			final boolean compressed = decoded.length == COMPRESSED_SIZE && decoded[COMPRESSED_SIZE - 1] == 1;

			if (decoded.length != UNCOMPRESSED_SIZE && !compressed || decoded[0] != (byte) SECRET_VERSION) {
				throw new IllegalArgumentException("Not a private key of this network");
			}

			return ECKey.fromPrivate(Arrays.copyOfRange(decoded, 1, UNCOMPRESSED_SIZE), compressed).getPubKey();
		} finally {
			Arrays.fill(decoded, (byte) 0);
		}
	}

	private static IllegalArgumentException refusal(final Path wallet, final String reason) {
		return new IllegalArgumentException(wallet + " is not a wallet dump Janus can read: " + reason);
	}
}
