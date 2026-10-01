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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShutdownDeadlineTest {
	private static final long WAIT_SECONDS = 5;

	@Test
	public void shouldEndTheProcessOnceTheGraceHasPassed() throws Exception {
		final CompletableFuture<Boolean> ended = new CompletableFuture<>();

		ShutdownDeadline.start(Duration.ofMillis(50), () -> ended.complete(Thread.currentThread().isDaemon()));
		assertTrue(ended.get(WAIT_SECONDS, TimeUnit.SECONDS), "The deadline would keep the process alive");
	}

	@Test
	public void shouldWaitOutTheGraceFirst() throws Exception {
		final CompletableFuture<Boolean> ended = new CompletableFuture<>();

		ShutdownDeadline.start(Duration.ofSeconds(WAIT_SECONDS), () -> ended.complete(true));
		Thread.sleep(100);
		assertFalse(ended.isDone());
	}
}
