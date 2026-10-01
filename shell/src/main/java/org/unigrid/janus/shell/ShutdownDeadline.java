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

package org.unigrid.janus.shell;

import java.time.Duration;

/** Ends the process once a shutdown has had its time, for the times it never finishes on its own. */
final class ShutdownDeadline {
	private ShutdownDeadline() {
	}

	/* A daemon, so a shutdown that does finish in time is never held up by the wait. */
	static void start(final Duration grace, final Runnable end) {
		final Thread deadline = new Thread(() -> {
			try {
				Thread.sleep(grace.toMillis());
				end.run();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, "shutdown-deadline");

		deadline.setDaemon(true);
		deadline.start();
	}
}
