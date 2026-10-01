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

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import org.junit.jupiter.api.Assumptions;
import org.unigrid.janus.core.ReleaseKey;
import org.unigrid.janus.core.SigningKey;
import org.unigrid.janus.core.hedgehog.HedgehogState.Phase;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.unigrid.janus.core.hedgehog.HedgehogServiceTest.await;
import static org.unigrid.janus.core.hedgehog.HedgehogServiceTest.gone;
import static org.unigrid.janus.core.hedgehog.HedgehogServiceTest.settle;

/** The service from the outside in: the container that builds it, real processes, and a Hedgehog that goes wrong. */
public class HedgehogServiceIT {
	private static final String ASSET = Releases.here().asset(Releases.VERSION);
	private static final String HOME = "user.home";
	private static final String SIGNED = "SIGNED";
	private static final Duration SHORT = Duration.ofSeconds(2);

	private Path home;
	private StubHedgehog running;
	private HedgehogService service;
	private String realHome;

	@BeforeTry
	public void makeAHome() throws IOException {
		home = Files.createTempDirectory("hedgehog");
		running = new StubHedgehog();
		realHome = System.getProperty(HOME);
	}

	@AfterTry
	public void cleanUp() throws IOException {
		System.setProperty(HOME, realHome);
		System.clearProperty(HedgehogLocation.PROPERTY);
		System.clearProperty(HedgehogService.NETWORK_KEYS);

		if (service != null) {
			service.stop();
		}

		running.close();

		try (Stream<Path> paths = Files.walk(home)) {
			paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
		}
	}

	private HedgehogService reusing(final Duration timeout) {
		service = new HedgehogService(new HedgehogLocation(null, null, "", "Linux", Releases.unpinned(home)),
			new HedgehogInstaller(Releases.unpinned(home)), new HedgehogClient(running.uri(), SHORT),
			running.uri(), home.resolve("hedgehog.log"), timeout
		);
		return service;
	}

	private HedgehogService launching(final Path script, final Path log, final Duration timeout) throws IOException {
		final HedgehogLocation location = new HedgehogLocation(script.toString(), null, "", "Linux",
			Releases.unpinned(home)
		);

		return launching(location, new HedgehogInstaller(Releases.unpinned(home)), log, timeout);
	}

	private HedgehogService launching(final HedgehogLocation location, final HedgehogInstaller installer, final Path log,
		final Duration timeout) throws IOException {

		final URI launchedAt;

		try (ServerSocket socket = new ServerSocket(0)) {
			launchedAt = URI.create("http://127.0.0.1:" + socket.getLocalPort());
		}

		service = new HedgehogService(location, installer, new HedgehogClient(launchedAt, SHORT), launchedAt, log,
			timeout
		);
		return service;
	}

	private HedgehogService launching(final Duration timeout) throws IOException {
		return launching(FakeHedgehog.install(home), home.resolve("hedgehog.log"), timeout);
	}

	private List<String> starts() throws IOException {
		final Path starts = home.resolve(FakeHedgehog.STARTS);

		return Files.exists(starts) ? Files.readAllLines(starts) : List.of();
	}

	private static boolean free(final int port) {
		try (ServerSocket socket = new ServerSocket(port, 1, InetAddress.getLoopbackAddress())) {
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	private void waitForTheDaemon() throws InterruptedException {
		final Instant deadline = Instant.now().plusSeconds(30);

		while (!Files.exists(home.resolve(FakeHedgehog.DAEMON_PID)) && Instant.now().isBefore(deadline)) {
			Thread.sleep(25);
		}
	}

	@Example
	public void shouldBeBuiltByTheContainerAndStopTheHedgehogItStartedWhenTheContainerCloses() throws IOException {
		Assumptions.assumeTrue(free(HedgehogClient.LOCAL.getPort()), "A Hedgehog of its own already holds the port");
		Files.writeString(home.resolve(FakeHedgehog.LEDGER), SIGNED);
		Files.createFile(home.resolve(FakeHedgehog.TLS));
		System.setProperty(HOME, home.toString());
		System.setProperty(HedgehogLocation.PROPERTY, FakeHedgehog.install(home).toString());

		try (SeContainer container = SeContainerInitializer.newInstance().initialize()) {
			final HedgehogService built = container.select(HedgehogService.class).get();

			built.prepare();
			assertEquals(Phase.READY, settle(built).phase(), built.state().reason());
			assertTrue(Files.exists(home.resolve(".janus").resolve("hedgehog.log")), "its log is in the home");
			assertEquals(List.of("started"), starts());
		}

		assertTrue(gone(home.resolve(FakeHedgehog.DAEMON_PID)), "the container ends what it started");
	}

	@Example
	public void shouldFailWhenTheHedgehogItIsToRunCannotBeExecuted() throws IOException {
		final Path script = Files.writeString(home.resolve(Releases.here().executable()),
			"#!/nonexistent/interpreter\n"
		);

		script.toFile().setExecutable(true);
		launching(script, home.resolve("hedgehog.log"), SHORT).prepare();
		assertTrue(settle(service).reason().startsWith("Hedgehog could not be run from " + script),
			service.state().reason()
		);
	}

	@Example
	public void shouldFailWhenItsLogCannotBeMade() throws IOException {
		final Path inTheWay = Files.createFile(home.resolve("blocked"));

		launching(FakeHedgehog.install(home), inTheWay.resolve("hedgehog.log"), SHORT).prepare();
		assertTrue(settle(service).reason().startsWith("Hedgehog could not be run from "), service.state().reason());
		assertEquals(List.of(), starts());
	}

	@Example
	public void shouldEndAHedgehogThatStaysAliveWithoutAnswering() throws IOException {
		Files.createFile(home.resolve(FakeHedgehog.SILENT));
		launching(SHORT).prepare();

		assertEquals(HedgehogState.failed("Hedgehog did not answer within 2 seconds"), settle(service));
		assertTrue(gone(home.resolve(FakeHedgehog.DAEMON_PID)));
	}

	@Example
	public void shouldStartAgainAfterAStartThatFailed() throws IOException {
		final Path silent = Files.createFile(home.resolve(FakeHedgehog.SILENT));

		launching(SHORT).prepare();
		assertEquals(Phase.FAILED, settle(service).phase());

		Files.delete(silent);
		Files.writeString(home.resolve(FakeHedgehog.LEDGER), SIGNED);
		service.prepare();

		assertEquals(Phase.READY, settle(service).phase(), service.state().reason());
		assertEquals(2, starts().size());
	}

	@Example
	public void shouldLeaveNothingRunningWhenStoppedWhileHedgehogIsStarting() throws IOException, InterruptedException {
		Files.createFile(home.resolve(FakeHedgehog.SILENT));
		launching(Duration.ofSeconds(20)).prepare();
		waitForTheDaemon();

		service.stop();

		assertEquals(Phase.FAILED, settle(service).phase());
		assertNotNull(service.state().reason());
		assertTrue(gone(home.resolve(FakeHedgehog.DAEMON_PID)));
		assertDoesNotThrow(service::stop);
	}

	@Example
	public void shouldPassTheNetworkKeysOnToTheHedgehogItStarts() throws IOException {
		Files.writeString(home.resolve(FakeHedgehog.LEDGER), SIGNED);
		System.setProperty(HedgehogService.NETWORK_KEYS, "ab,cd");
		launching(Duration.ofSeconds(20)).prepare();

		assertEquals(Phase.READY, settle(service).phase(), service.state().reason());
		assertTrue(Files.readString(home.resolve(FakeHedgehog.ARGUMENTS)).endsWith("--network-keys=ab,cd"));
	}

	@Example
	public void shouldLeaveAFailureAloneWhenAskedToCheckAgain() {
		running.answer("/version", 202, "{\"version\":\"0.0.8\"}");
		running.answer("/bootstrap", 200, FakeHedgehog.snapshot("UNSIGNED"));
		reusing(SHORT).prepare();

		final HedgehogState failed = settle(service);

		service.recheck();
		assertEquals(Phase.FAILED, failed.phase());
		assertEquals(failed, service.state());
	}

	@Example
	public void shouldRefuseALedgerWhoseSignatureIsInvalid() {
		running.answer("/version", 202, "{\"version\":\"0.0.8\"}");
		running.answer("/bootstrap", 200, FakeHedgehog.snapshot("INVALID"));
		reusing(SHORT).prepare();

		assertEquals(HedgehogState.failed("The ledger is not signed by the Unigrid Foundation"), settle(service));
	}

	@Example
	public void shouldFailWhenHedgehogCannotGiveTheLedgerItHolds() {
		running.answer("/version", 202, "{\"version\":\"0.0.8\"}");
		running.answer("/bootstrap", 500, "");
		reusing(SHORT).prepare();

		assertEquals(HedgehogState.failed("Hedgehog answered 500"), settle(service));
	}

	@Example
	public void shouldFailWhenTheLedgerStopsComingAfterItHasBeenComing() {
		running.answer("/version", 202, "{\"version\":\"0.0.8\"}");
		running.answer("/bootstrap", 503, "");
		running.answer("/status", 200, "{\"status\":\"downloading\",\"progress\":10}");
		reusing(Duration.ofSeconds(1)).prepare();

		assertEquals(HedgehogState.fetching(10), await(service, HedgehogState.fetching(10)));

		running.answer("/status", 200, "{\"status\":\"running\",\"progress\":100}");
		assertTrue(settle(service).reason().startsWith("The legacy ledger could not be downloaded; see "),
			service.state().reason()
		);
	}

	@Example
	public void shouldFailWhenHedgehogVanishesWhileTheLedgerIsComing() {
		running.answer("/version", 202, "{\"version\":\"0.0.8\"}");
		running.answer("/bootstrap", 503, "");
		running.answer("/status", 200, "{\"status\":\"downloading\",\"progress\":10}");
		reusing(Duration.ofSeconds(20)).prepare();

		assertEquals(HedgehogState.fetching(10), await(service, HedgehogState.fetching(10)));

		running.close();
		assertTrue(settle(service).reason().startsWith("Hedgehog does not answer at "), service.state().reason());
	}

	@Example
	public void shouldSayItIsDownloadingHedgehogWhileTheDownloadIsUnderWay() throws IOException {
		final byte[] executable = FakeHedgehog.executable(home);
		final SigningKey key = new SigningKey();
		final CountDownLatch release = new CountDownLatch(1);

		Files.writeString(home.resolve(FakeHedgehog.LEDGER), SIGNED);

		try (ReleaseServer page = new ReleaseServer().serve(ASSET, executable)
			.serve(ASSET + ".asc", key.sign(executable)).holdingBack(release)) {

			final HedgehogRelease pinned = Releases.pinning(home, executable, Releases.here());
			final HedgehogInstaller installer = new HedgehogInstaller(pinned, page.uri(),
				(file, signature) -> ReleaseKey.verify(file, signature, key.ring())
			);

			launching(new HedgehogLocation(null, null, "", System.getProperty("os.name"), pinned), installer,
				home.resolve("hedgehog.log"), Duration.ofSeconds(20)
			).prepare();

			try {
				assertEquals(HedgehogState.downloadingHedgehog(null),
					await(service, HedgehogState.downloadingHedgehog(null))
				);
			} finally {
				release.countDown();
			}

			assertEquals(Phase.READY, settle(service).phase(), service.state().reason());
			assertTrue(service.downloadedHedgehog());
		}
	}
}
