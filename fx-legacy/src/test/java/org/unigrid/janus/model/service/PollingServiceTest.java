/*
	The Janus Wallet
	Copyright © 2021-2022 The Unigrid Foundation, UGD Software AB

	This program is free software: you can redistribute it and/or modify it under the terms of the
	addended GNU Affero General Public License as published by the Free Software Foundation, version 3
	of the License (see COPYING and COPYING.addendum).

	This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
	even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU Affero General Public License for more details.

	You should have received an addended copy of the GNU Affero General Public License with this program.
	If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.model.service;

import jakarta.inject.Inject;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.concurrent.atomic.AtomicInteger;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import net.jqwik.api.Property;
import net.jqwik.api.ForAll;
import net.jqwik.api.constraints.IntRange;
import org.unigrid.janus.jqwik.BaseMockedWeldTest;
import static org.awaitility.Awaitility.await;

public class PollingServiceTest extends BaseMockedWeldTest {
	@Inject
	private PollingService pollingService;

	@Mocked
	private DebugService debug;

	@Property
	public void shouldPollWithTheRightInterval(@ForAll @IntRange(min = 1, max = 4) int interval) {
		final AtomicInteger calls = new AtomicInteger();

		new MockUp<LongPollingTask>() {
			@Mock
			public void run() {
				calls.incrementAndGet();
			}
		};

		pollingService.poll(interval * 100);
		await().atLeast(interval * 100, MILLISECONDS).atMost(1, SECONDS).until(() -> calls.get() == 2);
		pollingService.stopPolling();
	}
}
