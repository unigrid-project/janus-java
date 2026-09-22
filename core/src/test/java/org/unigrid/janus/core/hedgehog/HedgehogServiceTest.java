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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import org.unigrid.janus.core.hedgehog.HedgehogState.Phase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HedgehogServiceTest {
	private static final Set<Phase> SETTLED = Set.of(Phase.READY, Phase.FAILED);

	private Path home;
	private StubHedgehog running;
	private HedgehogService service;

	@BeforeTry
	public void makeAHome() throws IOException {
		home = Files.createTempDirectory("hedgehog");
		running = new StubHedgehog();
	}

	@AfterTry
	public void cleanUp() throws IOException {
		if (service != null) {
			service.stop();
		}

		running.close();

		try (Stream<Path> paths = Files.walk(home)) {
			paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
		}
	}

	private HedgehogService service(final HedgehogClient client, final HedgehogLocation location) {
		service = new HedgehogService(location, client, running.uri(), home.resolve("hedgehog.log"),
			Duration.ofSeconds(20)
		);
		return service;
	}

	private HedgehogService reusing() {
		return service(new HedgehogClient(running.uri(), Duration.ofSeconds(2)), nowhere());
	}

	static HedgehogLocation nowhere() {
		return new HedgehogLocation(null, null, "", "Linux");
	}

	static HedgehogState settle(final HedgehogService service) {
		final Instant deadline = Instant.now().plusSeconds(30);

		try {
			while (!SETTLED.contains(service.state().phase()) && Instant.now().isBefore(deadline)) {
				Thread.sleep(50);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		return service.state();
	}

	@Example
	public void shouldBeIdleUntilAsked() {
		assertEquals(HedgehogState.IDLE, reusing().state());
	}

	@Example
	public void shouldBeReadyWithARunningHedgehogAndItsSignedLedger() {
		running.answer("/version", 202, "{\"version\":\"0.0.8\"}");
		running.answer("/bootstrap", 200, FakeHedgehog.snapshot("SIGNED"));

		final HedgehogService service = reusing();

		service.prepare();

		final HedgehogState state = settle(service);

		assertEquals(Phase.READY, state.phase(), state.reason());
		assertEquals(3172666, state.snapshot().tipHeight());
	}

	@Example
	public void shouldRefuseALedgerThatIsNotSigned() {
		running.answer("/version", 202, "{\"version\":\"0.0.8\"}");
		running.answer("/bootstrap", 200, FakeHedgehog.snapshot("UNSIGNED"));

		final HedgehogService service = reusing();

		service.prepare();
		assertEquals(HedgehogState.failed("The ledger is not signed by the Unigrid Foundation"), settle(service));
	}

	@Example
	public void shouldTryAgainAfterAFailure() {
		running.answer("/version", 202, "{\"version\":\"0.0.8\"}");
		running.answer("/bootstrap", 200, FakeHedgehog.snapshot("UNSIGNED"));

		final HedgehogService service = reusing();

		service.prepare();
		settle(service);
		running.answer("/bootstrap", 200, FakeHedgehog.snapshot("SIGNED"));
		service.prepare();
		assertEquals(Phase.READY, settle(service).phase());
	}

	private URI launchedAt;

	private HedgehogService launching() throws IOException {
		return launching(FakeHedgehog.install(home));
	}

	private HedgehogService launching(final Path script) throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			launchedAt = URI.create("http://127.0.0.1:" + socket.getLocalPort());
		}

		service = new HedgehogService(new HedgehogLocation(script.toString(), null, "", "Linux"),
			new HedgehogClient(launchedAt, Duration.ofSeconds(2)), launchedAt, home.resolve("hedgehog.log"),
			Duration.ofSeconds(20)
		);
		return service;
	}

	private List<String> starts() throws IOException {
		final Path starts = home.resolve(FakeHedgehog.STARTS);

		return Files.exists(starts) ? Files.readAllLines(starts) : List.of();
	}

	@Example
	public void shouldStartAHedgehogWhenNoneAnswers() throws IOException {
		Files.writeString(home.resolve(FakeHedgehog.LEDGER), "SIGNED");

		final HedgehogService service = launching();

		service.prepare();
		assertEquals(Phase.READY, settle(service).phase(), service.state().reason());
		assertEquals(1, starts().size());
	}

	@Example
	public void shouldStartOnlyOneHedgehogHoweverOftenAsked() throws IOException {
		Files.writeString(home.resolve(FakeHedgehog.LEDGER), "SIGNED");

		final HedgehogService service = launching();

		for (int i = 0; i < 5; i++) {
			service.prepare();
		}

		settle(service);
		assertEquals(1, starts().size());
	}

	@Example
	public void shouldFailWhenNoHedgehogIsInstalled() {
		final HedgehogService service = service(new HedgehogClient(running.uri(), Duration.ofSeconds(2)), nowhere());

		service.prepare();
		assertEquals(HedgehogState.failed("Hedgehog is not installed on this computer"), settle(service));
	}

	@Example
	public void shouldFailWhenHedgehogStopsAsItStarts() throws IOException {
		Files.createFile(home.resolve(FakeHedgehog.DIES));

		final HedgehogService service = launching();

		service.prepare();
		assertTrue(settle(service).reason().startsWith("Hedgehog stopped as it started; see "),
			service.state().reason()
		);
	}

	@Example
	public void shouldStopTheHedgehogItStarted() throws IOException {
		Files.writeString(home.resolve(FakeHedgehog.LEDGER), "SIGNED");

		final HedgehogService service = launching();

		service.prepare();
		settle(service);

		final Instant asked = Instant.now();

		service.stop();
		assertTrue(Duration.between(asked, Instant.now()).compareTo(Duration.ofSeconds(3)) < 0,
			"Hedgehog should leave when asked, well before it has to be forced"
		);

		try (HedgehogClient afterwards = new HedgehogClient(launchedAt, Duration.ofSeconds(1))) {
			assertEquals(Optional.empty(), afterwards.version());
		}
	}

	@Example
	public void shouldLeaveAHedgehogItDidNotStartRunning() {
		running.answer("/version", 202, "{\"version\":\"0.0.8\"}");
		running.answer("/bootstrap", 200, FakeHedgehog.snapshot("SIGNED"));

		final HedgehogService service = reusing();

		service.prepare();
		settle(service);
		service.stop();
		assertTrue(running.requests().stream().noneMatch(request -> "/stop".equals(request.getRawPath())));
	}

	@Example
	public void shouldFetchTheLedgerWhenHedgehogHasNone() throws IOException {
		Files.writeString(home.resolve(FakeHedgehog.SIGNATURE), "SIGNED");

		final HedgehogService service = launching();

		service.prepare();
		assertEquals(Phase.READY, settle(service).phase(), service.state().reason());
		assertTrue(Files.exists(home.resolve(FakeHedgehog.LEDGER)));
	}

	@Example
	public void shouldRefuseAFetchedLedgerThatIsNotSigned() throws IOException {
		Files.writeString(home.resolve(FakeHedgehog.SIGNATURE), "UNSIGNED");

		final HedgehogService service = launching();

		service.prepare();
		assertEquals(HedgehogState.failed("The ledger is not signed by the Unigrid Foundation"), settle(service));
	}

	@Example
	public void shouldFailWhenTheLedgerCannotBeFetched() throws IOException {
		Files.createFile(home.resolve(FakeHedgehog.FETCH_FAILS));

		final HedgehogService service = launching();

		service.prepare();
		assertTrue(settle(service).reason().startsWith("The legacy ledger could not be downloaded; see "),
			service.state().reason()
		);
	}

	@Example
	public void shouldFailWhenThereIsNothingHereToFetchWith() {
		running.answer("/version", 202, "{\"version\":\"0.0.8\"}");
		running.answer("/bootstrap", 503, "");

		final HedgehogService service = reusing();

		service.prepare();
		final String reason = "Hedgehog has no legacy ledger, and there is no Hedgehog here to fetch one";

		assertEquals(HedgehogState.failed(reason), settle(service));
	}

	@Example
	public void shouldStopAFetchUnderWayWhenJanusCloses() throws IOException, InterruptedException {
		Files.createFile(home.resolve(FakeHedgehog.FETCH_HANGS));

		final HedgehogService service = launching();
		final Path pid = home.resolve(FakeHedgehog.FETCH_PID);

		service.prepare();

		for (int i = 0; i < 100 && !(Files.exists(pid) && Files.size(pid) > 0); i++) {
			Thread.sleep(100);
		}

		assertEquals(Phase.FETCHING, service.state().phase());
		service.stop();
		Thread.sleep(500);
		assertEquals(Optional.of(false), ProcessHandle.of(Long.parseLong(Files.readString(pid).trim()))
			.map(ProcessHandle::isAlive).or(() -> Optional.of(false))
		);
	}

	static boolean gone(final Path pidFile) throws IOException {
		final long pid = Long.parseLong(Files.readString(pidFile).trim());

		return ProcessHandle.of(pid).map(process -> process.onExit().completeOnTimeout(null, 5, TimeUnit.SECONDS)
			.thenApply(exited -> !process.isAlive()).join()).orElse(true);
	}

	@Example
	public void shouldEndItsOwnHedgehogEvenWhenItRefusesToStop() throws IOException {
		Files.writeString(home.resolve(FakeHedgehog.LEDGER), "SIGNED");
		Files.createFile(home.resolve(FakeHedgehog.STOP_REFUSED));

		final HedgehogService service = launching();

		service.prepare();
		settle(service);
		service.stop();
		assertTrue(gone(home.resolve(FakeHedgehog.DAEMON_PID)));
	}

	@Example
	public void shouldEndTheHedgehogALauncherStartedAsWell() throws IOException {
		Files.writeString(home.resolve(FakeHedgehog.LEDGER), "SIGNED");
		Files.createFile(home.resolve(FakeHedgehog.STOP_REFUSED));

		final HedgehogService service = launching(FakeHedgehog.installAsLauncher(home));

		service.prepare();
		settle(service);
		service.stop();
		assertTrue(gone(home.resolve(FakeHedgehog.DAEMON_PID)));
	}

	@Example
	public void shouldEndAFetchALauncherStartedWhenJanusCloses() throws IOException, InterruptedException {
		Files.createFile(home.resolve(FakeHedgehog.FETCH_HANGS));

		final HedgehogService service = launching(FakeHedgehog.installAsLauncher(home));
		final Path pid = home.resolve(FakeHedgehog.FETCH_PID);

		service.prepare();

		for (int i = 0; i < 100 && !(Files.exists(pid) && Files.size(pid) > 0); i++) {
			Thread.sleep(100);
		}

		service.stop();
		assertTrue(gone(pid));
	}

	@Example
	public void shouldStartNothingOnceStopped() {
		final HedgehogService service = reusing();

		service.stop();
		assertEquals(HedgehogState.IDLE, service.prepare());
	}
}
