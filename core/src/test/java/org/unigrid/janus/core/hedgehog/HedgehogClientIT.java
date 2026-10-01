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
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every way Hedgehog can answer wrongly, asked of each call the client makes, over a real socket. */
public class HedgehogClientIT {
	private static final String ADDRESS = "HQPjfHhSHs2rt97BrdGjT1BdL3Yg43yhXe";
	private static final String BALANCE_PATH = "/bootstrap/address/" + ADDRESS;
	private static final Duration PATIENCE = Duration.ofSeconds(1);

	/* What each call reads, and the start of an answer that cannot be completed by a Hedgehog that hangs. */
	private record Call(String path, String start, Consumer<HedgehogClient> ask) {
	}

	private static final List<Call> READS = List.of(
		new Call("/bootstrap", "{\"tipHash\":", HedgehogClient::snapshot),
		new Call(BALANCE_PATH, "{\"address\":", client -> client.balance(ADDRESS)),
		new Call(BALANCE_PATH + "/transactions", "[{\"transaction\":",
			client -> client.transactions(ADDRESS, 0, 10)
		),
		new Call("/gridspork/mint-storage", "{\"data\":", HedgehogClient::mints)
	);
	private static final Call STOP = new Call("/stop", "", HedgehogClient::stop);
	private static final List<Call> ALL = Stream.concat(READS.stream(), Stream.of(STOP)).toList();

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

	private static URI nothingListening() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return URI.create("http://127.0.0.1:" + socket.getLocalPort());
		}
	}

	@Example
	public void shouldTurnEachStatusHedgehogGivesIntoItsOwnFailure() {
		final Map<Integer, Class<? extends RuntimeException>> failures = Map.of(
			503, SnapshotMissing.class, 400, IllegalArgumentException.class, 500, IllegalStateException.class
		);

		for (final Call call : ALL) {
			failures.forEach((status, failure) -> {
				hedgehog.answer(call.path(), status, "");
				assertThrows(failure, () -> call.ask().accept(client), call.path() + " answered " + status);
			});
		}
	}

	@Example
	public void shouldRefuseAnAnswerThatIsNotJsonFromEveryCallThatReadsOne() {
		for (final Call call : READS) {
			hedgehog.answer(call.path(), 200, "<html><body>Something else lives here</body></html>");
			assertThrows(IllegalStateException.class, () -> call.ask().accept(client), call.path());
		}
	}

	@Example
	public void shouldSayHedgehogIsUnavailableFromEveryCallWhenNothingListens() throws IOException {
		try (HedgehogClient nowhere = new HedgehogClient(nothingListening(), Duration.ofSeconds(2))) {
			for (final Call call : ALL) {
				assertThrows(HedgehogUnavailable.class, () -> call.ask().accept(nowhere), call.path());
			}
		}
	}

	@Example
	public void shouldSayHedgehogIsUnavailableWhenAnAnswerIsCutOffMidway() {
		try (HedgehogClient impatient = new HedgehogClient(hedgehog.uri(), PATIENCE)) {
			for (final Call call : READS) {
				hedgehog.stallMidAnswer(call.path(), call.start(), Duration.ofSeconds(3));
				assertThrows(HedgehogUnavailable.class, () -> call.ask().accept(impatient), call.path());
			}
		}
	}

	@Example
	public void shouldFindNoMintsWhereTheSporkHoldsNone() {
		for (final String spork : List.of("{}", "{\"data\":null}", "{\"data\":{}}", "{\"data\":{\"mints\":null}}")) {
			hedgehog.answer("/gridspork/mint-storage", 200, spork);
			assertEquals(List.of(), client.mints(), spork);
		}
	}

	@Example
	public void shouldGiveNoVersionWhenTheAnswerHoldsNone() {
		for (final String answer : List.of("{}", "{\"version\":null}", "[]", "null")) {
			hedgehog.answer("/version", 202, answer);
			assertEquals(Optional.empty(), client.version(), answer);
		}
	}

	@Example
	public void shouldGiveNoVersionOrStatusWhateverFailureHedgehogAnswersWith() {
		for (final int status : List.of(400, 500, 503)) {
			hedgehog.answer("/version", status, "");
			hedgehog.answer("/status", status, "");

			assertEquals(Optional.empty(), client.version(), "version after " + status);
			assertEquals(Optional.empty(), client.status(), "status after " + status);
		}
	}

	@Example
	public void shouldGiveNoStatusWhenTheAnswerIsNotJson() {
		hedgehog.answer("/status", 200, "<html><body>Something else lives here</body></html>");

		assertEquals(Optional.empty(), client.status());
	}

	@Example
	public void shouldAskToStopWithAPostThatCarriesTheToken() {
		hedgehog.answer("/stop", 202, "");

		client.stop();

		assertEquals(List.of("POST"), hedgehog.methods());
		assertEquals(List.of("Bearer " + client.token()), hedgehog.authorizations());
	}

	@Example
	public void shouldAskForEverythingElseWithAGet() {
		for (final Call call : READS) {
			try {
				call.ask().accept(client);
			} catch (RuntimeException e) {
				assertTrue(e instanceof IllegalStateException, "only the missing answer may fail, not " + e);
			}
		}

		assertEquals(List.of("GET", "GET", "GET", "GET"), hedgehog.methods());
	}

	@Example
	public void shouldRefuseEveryHostButTheLoopbackAddress() {
		for (final String host : List.of("https://localhost:52884", "https://[::1]:52884", "https://0.0.0.0:52884",
			"/no-host-at-all")) {

			final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
				() -> new HedgehogClient(URI.create(host), Duration.ofSeconds(2)), host
			);

			assertTrue(thrown.getMessage().contains(host), thrown.getMessage());
		}
	}
}
