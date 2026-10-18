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

import java.time.Duration;
import java.time.Instant;
import net.jqwik.api.Example;
import org.junit.jupiter.api.Assumptions;
import org.unigrid.janus.core.hedgehog.HedgehogState.Phase;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Downloads the real Hedgehog and the real legacy ledger, the way a user's computer does. It moves hundreds of
 * megabytes, so it runs only where janus.real-ledger is true, which only the workflow sets.
 */
public class RealLedgerDownloadIT {
	private static final Duration LIMIT = Duration.ofMinutes(25);
	private static final Duration REPORT = Duration.ofSeconds(15);

	@Example
	public void shouldDownloadTheLedgerToTheEnd() throws InterruptedException {
		Assumptions.assumeTrue(Boolean.getBoolean("janus.real-ledger"));

		final HedgehogRelease release = new HedgehogRelease();
		final HedgehogService service = new HedgehogService(new HedgehogLocation(release),
			new HedgehogInstaller(release)
		);

		try {
			service.prepare();
			follow(service);
			assertEquals(Phase.READY, service.state().phase(), service.state().reason());
		} finally {
			service.stop();
		}
	}

	private static void follow(final HedgehogService service) throws InterruptedException {
		final Instant deadline = Instant.now().plus(LIMIT);
		HedgehogState seen = null;
		Instant reported = Instant.now();

		while (!settled(service.state()) && Instant.now().isBefore(deadline)) {
			if (!service.state().equals(seen) || Instant.now().isAfter(reported.plus(REPORT))) {
				seen = service.state();
				reported = Instant.now();
				System.out.println(Instant.now() + " " + seen.phase() + " " + seen.progress() + "%");
			}

			Thread.sleep(500);
		}
	}

	private static boolean settled(final HedgehogState state) {
		return state.phase() == Phase.READY || state.phase() == Phase.FAILED;
	}
}
