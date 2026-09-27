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
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.TreeSet;
import net.jqwik.api.Example;
import org.unigrid.janus.core.wallet.WalletTransaction.Kind;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WalletCsvTest {
	@Example
	public void shouldWriteOneLinePerTransactionUnderAHeader() throws IOException {
		final Path file = Files.createTempFile("history", ".csv");

		try {
			WalletCsv.write(file, List.of(
				new WalletTransaction("bb", Instant.parse("2019-03-02T08:00:00Z"), 9, new BigDecimal("-480"),
					Kind.SENT, new TreeSet<>(List.of("Hchange", "Hsender"))
				),
				new WalletTransaction("aa", Instant.parse("2019-03-01T08:00:00Z"), 5, new BigDecimal("0.1"),
					Kind.RECEIVED, new TreeSet<>(List.of("Hone"))
				)
			));

			assertEquals(List.of(
				"time,height,txid,kind,amount,addresses",
				"2019-03-02T08:00:00Z,9,bb,SENT,-480.00000000,Hchange Hsender",
				"2019-03-01T08:00:00Z,5,aa,RECEIVED,0.10000000,Hone"
			), Files.readAllLines(file));
		} finally {
			Files.delete(file);
		}
	}
}
