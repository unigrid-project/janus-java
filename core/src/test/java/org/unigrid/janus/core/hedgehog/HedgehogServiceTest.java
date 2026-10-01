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
import org.unigrid.janus.core.ReleaseKey;
import org.unigrid.janus.core.SigningKey;
import org.unigrid.janus.core.hedgehog.HedgehogState.Phase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HedgehogServiceTest {
	private static final Set<Phase> SETTLED = Set.of(Phase.READY, Phase.FAILED);
	private static final String ASSET = Releases.here().asset(Releases.VERSION);
	private static final String NO_CHECKSUM = "This Janus knows no checksum of Hedgehog 0.0.8 for this platform";

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
		return service(client, location, new HedgehogInstaller(Releases.unpinned(home)), Duration.ofSeconds(20));
	}

	private HedgehogService service(final HedgehogClient client, final HedgehogLocation location,
		final HedgehogInstaller installer, final Duration timeout) {

		service = new HedgehogService(location, installer, client, running.uri(), home.resolve("hedgehog.log"),
			timeout
		);
		return service;
	}

	private HedgehogService reusing() {
		return service(new HedgehogClient(running.uri(), Duration.ofSeconds(2)), nowhere());
	}

	private HedgehogLocation nowhere() {
		return new HedgehogLocation(null, null, "", "Linux", Releases.unpinned(home));
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

	/* A Hedgehog that was ready can still die later, and asking again must then look for one anew. */
	@Example
	public void shouldLookForHedgehogAgainOnceTheOneItFoundHasGone() {
		running.answer("/version", 202, "{\"version\":\"0.0.8\"}");
		running.answer("/bootstrap", 200, FakeHedgehog.snapshot("SIGNED"));

		final HedgehogService service = reusing();

		service.prepare();
		assertEquals(Phase.READY, settle(service).phase());

		running.answer("/version", 404, "");
		service.recheck();
		service.prepare();
		assertEquals(NO_CHECKSUM, settle(service).reason());
	}

	@Example
	public void shouldKeepAHedgehogThatStillAnswers() {
		running.answer("/version", 202, "{\"version\":\"0.0.8\"}");
		running.answer("/bootstrap", 200, FakeHedgehog.snapshot("SIGNED"));

		final HedgehogService service = reusing();

		service.prepare();
		settle(service);
		service.recheck();
		assertEquals(Phase.READY, service.state().phase());
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
		return launching(new HedgehogLocation(script.toString(), null, "", "Linux", Releases.unpinned(home)),
			new HedgehogInstaller(Releases.unpinned(home)), Duration.ofSeconds(20)
		);
	}

	private HedgehogService launching(final HedgehogLocation location, final HedgehogInstaller installer,
		final Duration timeout) throws IOException {

		try (ServerSocket socket = new ServerSocket(0)) {
			launchedAt = URI.create("http://127.0.0.1:" + socket.getLocalPort());
		}

		service = new HedgehogService(location, installer, new HedgehogClient(launchedAt, Duration.ofSeconds(2)),
			launchedAt, home.resolve("hedgehog.log"), timeout
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
		assertEquals(HedgehogState.failed(NO_CHECKSUM), settle(service));
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

		assertEquals(Optional.empty(), service.client().version());
	}

	@Example
	public void shouldStartAHedgehogThatTurnsAwayEveryoneButItsClient() throws IOException {
		Files.writeString(home.resolve(FakeHedgehog.LEDGER), "SIGNED");

		final HedgehogService service = launching();

		service.prepare();
		assertEquals(Phase.READY, settle(service).phase(), service.state().reason());

		try (HedgehogClient stranger = new HedgehogClient(launchedAt, Duration.ofSeconds(2))) {
			assertEquals(Optional.empty(), stranger.version());
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

	/* The daemon fetches the ledger on its own; all Janus does is watch and say how far it has got. */
	@Example
	public void shouldSayHowFarTheLedgerHasGotWhileHedgehogDownloadsIt() throws IOException {
		Files.writeString(home.resolve(FakeHedgehog.DOWNLOADING), "40");

		final HedgehogService service = launching();

		service.prepare();
		assertEquals(HedgehogState.fetching(40), await(service, HedgehogState.fetching(40)));

		Files.writeString(home.resolve(FakeHedgehog.LEDGER), "SIGNED");
		Files.delete(home.resolve(FakeHedgehog.DOWNLOADING));
		assertEquals(Phase.READY, settle(service).phase(), service.state().reason());
	}

	@Example
	public void shouldSayNothingOfProgressWhileTheSizeIsUnknown() throws IOException {
		Files.writeString(home.resolve(FakeHedgehog.DOWNLOADING), "");

		final HedgehogService service = launching();

		service.prepare();
		assertEquals(HedgehogState.fetching(null), await(service, HedgehogState.fetching(null)));
	}

	@Example
	public void shouldFailWhenHedgehogEndsUpWithoutALedger() throws IOException {
		final String script = FakeHedgehog.install(home).toString();
		final HedgehogLocation location = new HedgehogLocation(script, null, "", "Linux", Releases.unpinned(home));
		final HedgehogService service = launching(location, new HedgehogInstaller(Releases.unpinned(home)),
			Duration.ofSeconds(5)
		);

		service.prepare();
		assertTrue(settle(service).reason().startsWith("The legacy ledger could not be downloaded; see "),
			service.state().reason()
		);
	}

	@Example
	public void shouldNotWaitForALedgerNothingIsDownloading() {
		running.answer("/version", 202, "{\"version\":\"0.0.8\"}");
		running.answer("/bootstrap", 503, "");

		final HedgehogService service = service(new HedgehogClient(running.uri(), Duration.ofSeconds(2)),
			nowhere(), new HedgehogInstaller(Releases.unpinned(home)), Duration.ofSeconds(1)
		);

		service.prepare();
		assertTrue(settle(service).reason().startsWith("The legacy ledger could not be downloaded; see "));
	}

	@Example
	public void shouldKeepWaitingWhileTheLedgerIsStillComing() {
		running.answer("/version", 202, "{\"version\":\"0.0.8\"}");
		running.answer("/bootstrap", 503, "");
		running.answer("/status", 200, "{\"status\":\"downloading\",\"progress\":10}");

		final HedgehogService service = service(new HedgehogClient(running.uri(), Duration.ofSeconds(2)),
			nowhere(), new HedgehogInstaller(Releases.unpinned(home)), Duration.ofSeconds(1)
		);

		service.prepare();
		assertEquals(HedgehogState.fetching(10), await(service, HedgehogState.fetching(10)));
		assertEquals(Phase.FETCHING, service.state().phase());
	}

	static HedgehogState await(final HedgehogService service, final HedgehogState wanted) {
		final Instant deadline = Instant.now().plusSeconds(30);

		try {
			while (!wanted.equals(service.state()) && !SETTLED.contains(service.state().phase())
				&& Instant.now().isBefore(deadline)) {

				Thread.sleep(25);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		return service.state();
	}

	private HedgehogService downloading(final ReleaseServer page, final SigningKey trusted, final byte[] executable)
		throws IOException {

		final HedgehogRelease release = Releases.pinning(home, executable, Releases.here());
		final HedgehogInstaller installer = new HedgehogInstaller(release, page.uri(),
			(file, signature) -> ReleaseKey.verify(file, signature, trusted.ring())
		);

		return launching(new HedgehogLocation(null, null, "", System.getProperty("os.name"), release), installer,
			Duration.ofSeconds(20)
		);
	}

	@Example
	public void shouldDownloadHedgehogWhenNoneIsTheReleaseAndThenStartIt() throws IOException {
		Files.writeString(home.resolve(FakeHedgehog.LEDGER), "SIGNED");

		final byte[] executable = FakeHedgehog.executable(home);
		final SigningKey key = new SigningKey();

		try (ReleaseServer page = new ReleaseServer().serve(ASSET, executable)
			.serve(ASSET + ".asc", key.sign(executable))) {

			final HedgehogService service = downloading(page, key, executable);

			service.prepare();
			assertEquals(Phase.READY, settle(service).phase(), service.state().reason());
			assertTrue(service.downloadedHedgehog());
			assertEquals(1, starts().size());
		}
	}

	@Example
	public void shouldNotDownloadHedgehogWhenTheOneOnTheComputerIsTheRelease() throws IOException {
		Files.writeString(home.resolve(FakeHedgehog.LEDGER), "SIGNED");

		final byte[] executable = FakeHedgehog.executable(home);
		final Path installed = Files.createDirectories(home.resolve(".janus/hedgehog/0.0.8"))
			.resolve(Releases.here().executable());

		Files.write(installed, executable);
		installed.toFile().setExecutable(true);

		try (ReleaseServer page = new ReleaseServer()) {
			final HedgehogService service = downloading(page, new SigningKey(), executable);

			service.prepare();
			assertEquals(Phase.READY, settle(service).phase(), service.state().reason());
			assertFalse(service.downloadedHedgehog());
		}
	}

	@Example
	public void shouldFailWhenTheDownloadedHedgehogIsNotSignedByTheFoundation() throws IOException {
		final byte[] executable = FakeHedgehog.executable(home);
		final SigningKey key = new SigningKey();

		try (ReleaseServer page = new ReleaseServer().serve(ASSET, executable)
			.serve(ASSET + ".asc", new SigningKey().sign(executable))) {

			final HedgehogService service = downloading(page, key, executable);

			service.prepare();
			assertEquals(HedgehogState.failed("The downloaded Hedgehog is not signed by the Unigrid Foundation"),
				settle(service)
			);
			assertEquals(List.of(), starts());
		}
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
	public void shouldStartNothingOnceStopped() {
		final HedgehogService service = reusing();

		service.stop();
		assertEquals(HedgehogState.IDLE, service.prepare());
	}

	@Example
	public void shouldStartHedgehogTrustingItsOwnKeys() {
		assertEquals(List.of("hedgehog", "daemon", "--restport=52884"),
			HedgehogService.command(Path.of("hedgehog"), 52884, null)
		);
	}

	@Example
	public void shouldPassOnTheNetworkKeysItWasGiven() {
		assertEquals(List.of("hedgehog", "daemon", "--restport=52884", "--network-keys=ab,cd"),
			HedgehogService.command(Path.of("hedgehog"), 52884, "ab,cd")
		);
	}
}
