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

package org.unigrid.janus.core.wallet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The wallet's history in a form a spreadsheet opens. Nothing in it needs quoting: txids and addresses
 * are Base58 or hex, and amounts are plain numbers.
 */
public final class WalletCsv {
	private static final String HEADER = "time,height,txid,kind,amount,addresses";
	private static final int DECIMALS = 8;

	private WalletCsv() {
	}

	public static void write(final Path file, final List<WalletTransaction> transactions) {
		final List<String> lines = new ArrayList<>(List.of(HEADER));

		for (final WalletTransaction transaction : transactions) {
			lines.add(String.join(",", transaction.time().toString(), Integer.toString(transaction.height()),
				transaction.txid(), transaction.kind().name(),
				transaction.amount().setScale(DECIMALS, RoundingMode.UNNECESSARY).toPlainString(),
				String.join(" ", transaction.addresses())
			));
		}

		try {
			Files.write(file, lines, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException("The history could not be saved to " + file, e);
		}
	}
}
