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

package org.unigrid.janus.core.hedgehog;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HedgehogClientTest {
	private static final String ADDRESS = "HQPjfHhSHs2rt97BrdGjT1BdL3Yg43yhXe";
	private static final String BALANCE_PATH = "/bootstrap/address/" + ADDRESS;
	private static final String SNAPSHOT = """
		{"tipHash":"ffa055384cc3d357e7f3e424464f62f538f0973fc54d48eade8c5bb8ab747b52","tipHeight":3172666,
		"addressCount":28431,"entryCount":16799121,"transactionCount":9123456,
		"totalUnspent":15900032.04557988,"zerocoinMinted":67961,"built":"2026-09-14T10:15:30Z",
		"signature":"SIGNED"}""";

	private StubHedgehog hedgehog;
	private HedgehogClient client;

	@BeforeTry
	public void startAHedgehog() throws IOException {
		hedgehog = new StubHedgehog();
		client = new HedgehogClient(hedgehog.uri(), Duration.ofSeconds(2));
	}

	@AfterTry
	public void stopIt() {
		client.close();
		hedgehog.close();
	}

	@Example
	public void shouldReadTheSnapshotHedgehogHolds() {
		hedgehog.answer("/bootstrap", 200, SNAPSHOT);

		final SnapshotInfo snapshot = client.snapshot();

		assertEquals(3172666, snapshot.tipHeight());
		assertEquals(new BigDecimal("15900032.04557988"), snapshot.totalUnspent());
		assertEquals(Instant.parse("2026-09-14T10:15:30Z"), snapshot.built());
		assertEquals(SignatureStatus.SIGNED, snapshot.signature());
	}

	@Example
	public void shouldReadTheBalanceOfAnAddress() {
		hedgehog.answer(BALANCE_PATH, 200,
			"{\"address\":\"" + ADDRESS + "\",\"balance\":1250.5,\"transactionCount\":17}"
		);

		assertEquals(Optional.of(new AddressBalance(ADDRESS, new BigDecimal("1250.5"), 17)),
			client.balance(ADDRESS)
		);
	}

	@Example
	public void shouldAnswerNothingForAnAddressTheLedgerNeverSaw() {
		assertEquals(Optional.empty(), client.balance(ADDRESS));
	}

	@Example
	public void shouldReadAPageOfTransactions() {
		hedgehog.answer(BALANCE_PATH + "/transactions", 200, """
			[{"transaction":"ab01","time":"2018-09-04T12:00:00Z","height":5,"amount":100,"kind":"RECEIVED"},
			{"transaction":"cd02","time":"2018-09-05T12:00:00Z","height":9,"amount":-40.25,"kind":"SENT"}]""");

		final List<AddressTransaction> page = client.transactions(ADDRESS, 50, 25);

		assertEquals(List.of(
			new AddressTransaction("ab01", Instant.parse("2018-09-04T12:00:00Z"), 5, new BigDecimal("100"),
				EntryKind.RECEIVED
			),
			new AddressTransaction("cd02", Instant.parse("2018-09-05T12:00:00Z"), 9, new BigDecimal("-40.25"),
				EntryKind.SENT
			)
		), page);
		assertEquals("offset=50&limit=25", hedgehog.requests().get(0).getRawQuery());
	}

	@Example
	public void shouldSayTheLedgerIsMissingWhileHedgehogHasNone() {
		hedgehog.answer("/bootstrap", 503, "");

		assertThrows(SnapshotMissing.class, () -> client.snapshot());
	}

	@Example
	public void shouldRefuseAnAddressHedgehogCannotDecode() {
		hedgehog.answer(BALANCE_PATH, 400, "");

		assertThrows(IllegalArgumentException.class, () -> client.balance(ADDRESS));
	}

	@Example
	public void shouldReportAnAnswerHedgehogShouldNotGive() {
		hedgehog.answer("/bootstrap", 500, "");

		assertThrows(IllegalStateException.class, () -> client.snapshot());
	}

	@Example
	public void shouldRefuseAnAnswerThatIsNotJson() {
		hedgehog.answer("/bootstrap", 200, "<html><body>Something else lives here</body></html>");

		assertThrows(IllegalStateException.class, () -> client.snapshot());
	}

	@Example
	public void shouldNotFollowARedirectAwayFromTheHedgehogAsked() throws IOException {
		try (StubHedgehog elsewhere = new StubHedgehog()) {
			elsewhere.answer("/bootstrap", 200, SNAPSHOT);
			hedgehog.redirect("/bootstrap", elsewhere.uri().resolve("/bootstrap"));

			assertThrows(IllegalStateException.class, () -> client.snapshot());
			assertTrue(elsewhere.requests().isEmpty(), "the redirect should not have been followed");
		}
	}

	@Example
	public void shouldKeepAnOddAddressInsideItsPathSegment() {
		client.balance("a/b?c");

		assertEquals("/bootstrap/address/a%2Fb%3Fc", hedgehog.requests().get(0).getRawPath());
		assertEquals(null, hedgehog.requests().get(0).getRawQuery());
	}

	private static URI nothingListening() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return URI.create("http://127.0.0.1:" + socket.getLocalPort());
		}
	}

	@Example
	public void shouldGiveTheVersionOfTheHedgehogThatAnswers() {
		hedgehog.answer("/version", 202, "{\"version\":\"0.0.8\",\"protocols\":[\"hedgehog/0.0.2\"]}");

		assertEquals(Optional.of("0.0.8"), client.version());
	}

	@Example
	public void shouldGiveNoVersionWhenSomethingElseAnswers() {
		hedgehog.answer("/version", 200, "<html><body>Something else lives here</body></html>");

		assertEquals(Optional.empty(), client.version());
	}

	@Example
	public void shouldGiveNoVersionWhenTheAnswerIsNotHedgehogs() {
		assertEquals(Optional.empty(), client.version());
	}

	@Example
	public void shouldGiveNoVersionWhenNothingListens() throws IOException {
		try (HedgehogClient nowhere = new HedgehogClient(nothingListening(), Duration.ofSeconds(2))) {
			assertEquals(Optional.empty(), nowhere.version());
		}
	}

	@Example
	public void shouldSayHedgehogIsUnavailableWhenNothingListens() throws IOException {
		try (HedgehogClient nowhere = new HedgehogClient(nothingListening(), Duration.ofSeconds(2))) {
			assertThrows(HedgehogUnavailable.class, nowhere::snapshot);
		}
	}

	@Example
	public void shouldGiveUpOnAHedgehogThatNeverAnswers() {
		final long start = System.nanoTime();

		hedgehog.stall("/bootstrap", Duration.ofSeconds(3));

		try (HedgehogClient impatient = new HedgehogClient(hedgehog.uri(), Duration.ofSeconds(1))) {
			assertThrows(HedgehogUnavailable.class, impatient::snapshot);
		}

		assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(Duration.ofMillis(2500)) < 0);
	}

	@Example
	public void shouldTrustTheSelfSignedCertificateOfTheHedgehogHere() throws Exception {
		try (StubHedgehog secure = StubHedgehog.secure();
			HedgehogClient overTls = new HedgehogClient(secure.uri(), Duration.ofSeconds(2))) {

			secure.answer("/version", 202, "{\"version\":\"0.0.8\",\"protocols\":[]}");
			assertEquals(Optional.of("0.0.8"), overTls.version());
		}
	}

	@Example
	public void shouldRefuseAHostOtherThanThisComputer() {
		final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> new HedgehogClient(URI.create("https://hedgehog.example.org:52884"), Duration.ofSeconds(2))
		);

		assertTrue(thrown.getMessage().contains("hedgehog.example.org"), thrown.getMessage());
	}

	@Example
	public void shouldAskTheHedgehogOnThisComputerByDefault() {
		assertEquals(URI.create("https://127.0.0.1:52884"), HedgehogClient.LOCAL);
		new HedgehogClient().close();
	}
}
