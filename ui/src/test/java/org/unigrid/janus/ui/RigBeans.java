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

package org.unigrid.janus.ui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import java.util.Set;
import org.unigrid.janus.core.DataDirectory;
import org.unigrid.janus.core.hedgehog.HedgehogService;

/**
 * The two beans of the container that the container could only get from the person's computer: Hedgehog,
 * which lives at a fixed port there, and the folder the legacy daemon left its wallet in, which on Windows
 * is named by an environment variable no test can set. The rig that starts the container hands over its own,
 * and {@link Replacing} keeps the container from building the real ones beside them.
 */
public class RigBeans {
	private static volatile HedgehogService hedgehog;
	private static volatile DataDirectory data;

	/** Takes the real beans out of the container, as a producer is no alternative a container will prefer. */
	public static class Replacing implements Extension {
		private static final Set<Class<?>> REAL = Set.of(HedgehogService.class, DataDirectory.class);

		<T> void veto(@Observes final ProcessAnnotatedType<T> type) {
			if (REAL.contains(type.getAnnotatedType().getJavaClass())) {
				type.veto();
			}
		}
	}

	static void provide(final HedgehogService service, final DataDirectory directory) {
		hedgehog = service;
		data = directory;
	}

	@Produces
	@ApplicationScoped
	HedgehogService hedgehog() {
		return hedgehog;
	}

	void stop(@Disposes final HedgehogService service) {
		service.stop();
	}

	@Produces
	@ApplicationScoped
	DataDirectory data() {
		return data;
	}
}
