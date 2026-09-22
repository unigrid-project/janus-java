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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.unigrid.janus.core.hedgehog.HedgehogState.Phase;

/** Gets a Hedgehog ready to answer about the legacy ledger, and says how far along that is. */
@Slf4j
@ApplicationScoped
public class HedgehogService {
	private static final Duration START_TIMEOUT = Duration.ofSeconds(30);

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
				throw new IllegalStateException("No Hedgehog answers on this computer");
			}

			state = HedgehogState.ready(signed(client.snapshot()));
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
}
