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

package org.unigrid.janus.model;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import lombok.Data;
import lombok.SneakyThrows;
import net.jqwik.api.Example;
import org.unigrid.janus.jqwik.BaseMockedWeldTest;
import org.unigrid.janus.jqwik.WeldSetup;
import org.unigrid.janus.model.ThreadTest.Dispatcher;
import org.unigrid.janus.model.ThreadTest.Observer;
import org.unigrid.janus.model.signal.NodeRequest;
import org.unigrid.janus.model.signal.State;

@WeldSetup({ Dispatcher.class, Observer.class })
public class ThreadTest extends BaseMockedWeldTest {
	@Inject private Dispatcher dispatcher;
	@Inject private Observer Observer;

	@Example
	@SneakyThrows
	public void shouldReturnTrueOnFindGridnodeOutput() {
		System.out.println("main: " + Thread.currentThread().getName());

		dispatcher.anotherThread();
		//Thread.sleep(1000);
	}

	@ApplicationScoped
	public static class Dispatcher {
		@Inject private Event<MyEvent> eventTest;

		@SneakyThrows
		public void anotherThread() {
			Thread thread = new Thread(() -> {
				System.out.println("dispatcher: " + Thread.currentThread().getName());
				eventTest.fireAsync(new MyEvent());
			});

			thread.setName("anotherThread");
			thread.start();
		}

	}

	@ApplicationScoped
	public static class Observer {
		private void eventNodeRequest(@ObservesAsync MyEvent eventTest) {
			System.out.println("observer: " + Thread.currentThread().getName());
			System.out.println("Thread: Observes");
		}
	}

	@Data
	public static class MyEvent {
		private String name;
	}
}
