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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import org.unigrid.janus.core.hedgehog.AddressTransaction;
import org.unigrid.janus.core.hedgehog.EntryKind;
import org.unigrid.janus.core.hedgehog.HedgehogStand;
import org.unigrid.janus.core.wallet.LedgerState.Phase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WalletLedgerTest {
	private static final Path WALLET = fixture("plain-wallet.dat");

	private HedgehogStand stand;
	private List<String> addresses;
	private WalletLedger ledger;

	private static Path fixture(final String name) {
		try {
			return Path.of(WalletLedgerTest.class.getResource("/org/unigrid/janus/core/legacy/" + name).toURI());
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	private static AddressTransaction entry(final String txid, final int height, final String amount,
		final EntryKind kind) {

		return new AddressTransaction(txid, Instant.parse("2019-03-01T00:00:00Z").plusSeconds(height), height,
			new BigDecimal(amount), kind
		);
	}

	@BeforeTry
	public void open() throws IOException {
		stand = HedgehogStand.start().signed();
		addresses = Files.readAllLines(fixture("plain-wallet.addresses"));
		ledger = new WalletLedger(stand.client(), ZoneOffset.UTC);
	}

	@AfterTry
	public void close() {
		ledger.stop();
		stand.close();
	}

	private LedgerState settle() throws InterruptedException {
		final Instant deadline = Instant.now().plusSeconds(20);

		while (ledger.state().phase() == Phase.LOADING && Instant.now().isBefore(deadline)) {
			Thread.sleep(20);
		}

		return ledger.state();
	}

	@Example
	public void shouldReadEveryAddressOfTheWalletAndNetItsHistory() {
		stand.address(addresses.get(0), "480", entry("aa", 5, "1020", EntryKind.RECEIVED),
			entry("bb", 9, "-540", EntryKind.SENT)
		).address(addresses.get(1), "60", entry("bb", 9, "60", EntryKind.RECEIVED));

		final WalletFunds funds = ledger.read(WALLET);

		assertEquals(0, new BigDecimal("540").compareTo(funds.total()));
		assertEquals(List.of("bb", "aa"), funds.transactions().stream().map(WalletTransaction::txid).toList());
		assertEquals(new AddressBreakdown(2, 0, addresses.size() - 2), funds.breakdown());
		assertEquals(3172666, funds.snapshot().tipHeight());
	}

	@Example
	public void shouldPageThroughALongHistory() {
		final AddressTransaction[] many = new AddressTransaction[2345];

		for (int i = 0; i < many.length; i++) {
			many[i] = entry("t" + i, i, "1", EntryKind.MINED);
		}

		stand.address(addresses.get(0), "2345", many);
		assertEquals(2345, ledger.read(WALLET).transactions().size());
	}

	@Example
	public void shouldLoadInTheBackgroundAndKeepWhatItLoaded() throws InterruptedException {
		stand.address(addresses.get(0), "1", entry("aa", 1, "1", EntryKind.RECEIVED));
		ledger.load(WALLET);

		final LedgerState loaded = settle();

		assertEquals(Phase.LOADED, loaded.phase());
		assertEquals(1, loaded.funds().transactions().size());
		assertEquals(Phase.LOADED, ledger.load(WALLET).phase());
	}

	@Example
	public void shouldFailWhenHedgehogStopsAnsweringAndStartOverOnRetry() throws InterruptedException {
		stand.address(addresses.get(0), "1", entry("aa", 1, "1", EntryKind.RECEIVED)).broken(addresses.get(0));
		ledger.load(WALLET);

		final LedgerState failed = settle();

		assertEquals(Phase.FAILED, failed.phase());
		assertEquals("Hedgehog stopped answering", failed.reason());
		assertEquals(null, failed.funds());

		stand.address(addresses.get(0), "1", entry("aa", 1, "1", EntryKind.RECEIVED));
		ledger.reset();
		ledger.load(WALLET);
		assertEquals(Phase.LOADED, settle().phase());
	}

	@Example
	public void shouldSayWhenTheWalletCannotBeRead() throws InterruptedException, IOException {
		final Path junk = Files.writeString(Files.createTempFile("junk", ".dat"), "not a wallet");

		try {
			ledger.load(junk);

			final LedgerState failed = settle();

			assertEquals(Phase.FAILED, failed.phase());
			assertTrue(failed.unreadable());
		} finally {
			Files.delete(junk);
		}
	}
}
