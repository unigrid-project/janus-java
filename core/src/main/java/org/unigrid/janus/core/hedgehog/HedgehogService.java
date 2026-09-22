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

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.unigrid.janus.core.hedgehog.HedgehogState.Phase;

/** Gets a Hedgehog ready to answer about the legacy ledger, and says how far along that is. */
@Slf4j
@ApplicationScoped
public class HedgehogService {
	private static final Duration START_TIMEOUT = Duration.ofSeconds(30);
	private static final Duration POLL = Duration.ofMillis(200);
	private static final Duration STOP_TIMEOUT = Duration.ofSeconds(5);

	private final HedgehogLocation location;
	private final HedgehogClient client;
	private final URI base;
	private final Path logFile;
	private final Duration startTimeout;
	private final ExecutorService worker = Executors.newSingleThreadExecutor(work -> {
		final Thread thread = new Thread(work, "hedgehog");

		thread.setDaemon(true);
		return thread;
	});

	private volatile HedgehogState state = HedgehogState.IDLE;
	private volatile Process started;
	private volatile Process fetching;

	@Inject
	public HedgehogService(final HedgehogLocation location) {
		this(location, new HedgehogClient(), HedgehogClient.LOCAL,
			Path.of(System.getProperty("user.home"), ".janus", "hedgehog.log"), START_TIMEOUT
		);
	}

	HedgehogService(final HedgehogLocation location, final HedgehogClient client, final URI base,
		final Path logFile, final Duration startTimeout) {

		this.location = location;
		this.client = client;
		this.base = base;
		this.logFile = logFile;
		this.startTimeout = startTimeout;
	}

	public HedgehogState state() {
		return state;
	}

	/** Starts getting Hedgehog ready unless that is under way or done; after a failure it tries again. */
	public synchronized HedgehogState prepare() {
		if (state.phase() == Phase.IDLE || state.phase() == Phase.FAILED) {
			state = HedgehogState.STARTING;
			worker.execute(this::bringUp);
		}

		return state;
	}

	/* Whatever goes wrong on the worker ends as FAILED, so a view waiting on it never waits forever. */
	private void bringUp() {
		try {
			if (client.version().isEmpty()) {
				started = launch();
				awaitAnswer(started);
			}

			state = HedgehogState.ready(signed(ledger()));
		} catch (RuntimeException e) {
			log.warn("Hedgehog could not be made ready", e);
			state = HedgehogState.failed(e.getMessage());
		}
	}

	private static SnapshotInfo signed(final SnapshotInfo snapshot) {
		if (snapshot.signature() != SignatureStatus.SIGNED) {
			throw new IllegalStateException("The ledger is not signed by the Unigrid Foundation");
		}

		return snapshot;
	}

	/* Hedgehog is asked to stop first so it can close its files, and only ended by force if it will not. */
	private void end(final Process process) {
		if (process == null || !process.isAlive()) {
			return;
		}

		/* Whatever Hedgehog makes of the request, it is ended below regardless, so no answer may stop that. */
		try {
			client.stop();
		} catch (RuntimeException e) {
			log.debug("Hedgehog did not take the request to stop", e);
		}

		try {
			if (!process.waitFor(STOP_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
				process.destroyForcibly();
			}
		} catch (InterruptedException e) {
			process.destroyForcibly();
			Thread.currentThread().interrupt();
		}
	}

	private Process launch() {
		final Path executable = location.find()
			.orElseThrow(() -> new IllegalStateException("Hedgehog is not installed on this computer"));

		return run(executable.toString(), "daemon", "--restport=" + base.getPort());
	}

	private Process run(final String... command) {
		try {
			Files.createDirectories(logFile.getParent());
			return new ProcessBuilder(command).redirectErrorStream(true)
				.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile())).start();
		} catch (IOException e) {
			throw new UncheckedIOException("Hedgehog could not be run from " + command[0], e);
		}
	}

	private void awaitAnswer(final Process process) {
		final Instant deadline = Instant.now().plus(startTimeout);

		while (client.version().isEmpty()) {
			if (!process.isAlive()) {
				throw new IllegalStateException("Hedgehog stopped as it started; see " + logFile);
			}

			if (Instant.now().isAfter(deadline)) {
				process.destroyForcibly();
				throw new IllegalStateException("Hedgehog did not answer within " + startTimeout.toSeconds()
					+ " seconds"
				);
			}

			pause();
		}
	}

	private static void pause() {
		try {
			Thread.sleep(POLL.toMillis());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Janus is shutting down", e);
		}
	}

	@PreDestroy
	public void stop() {
		worker.shutdownNow();

		final Process fetch = fetching;

		if (fetch != null) {
			fetch.destroyForcibly();
		}

		end(started);
		client.close();
	}

	private SnapshotInfo ledger() {
		try {
			return client.snapshot();
		} catch (SnapshotMissing e) {
			state = HedgehogState.FETCHING;
			fetch();
			return awaitLedger();
		}
	}

	private void fetch() {
		final Path executable = location.find().orElseThrow(() -> new IllegalStateException(
			"Hedgehog has no legacy ledger, and there is no Hedgehog here to fetch one"
		));

		fetching = run(executable.toString(), "bootstrap", "fetch", "--force");

		try {
			if (fetching.waitFor() != 0) {
				throw new IllegalStateException("The legacy ledger could not be downloaded; see " + logFile);
			}
		} catch (InterruptedException e) {
			fetching.destroyForcibly();
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Janus is shutting down", e);
		}
	}

	/* Hedgehog notices the new file by itself, but not necessarily on the very first request after it lands. */
	private SnapshotInfo awaitLedger() {
		final Instant deadline = Instant.now().plus(startTimeout);

		while (true) {
			try {
				return client.snapshot();
			} catch (SnapshotMissing e) {
				if (Instant.now().isAfter(deadline)) {
					throw new IllegalStateException("Hedgehog did not take up the downloaded ledger", e);
				}

				pause();
			}
		}
	}
}
