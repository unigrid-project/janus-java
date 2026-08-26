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

package org.unigrid.janus.web.action;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collects every {@link Action} the container sees while it starts, so that adding one is a matter
 * of annotating a method rather than remembering to register it somewhere else as well.
 */
public class ActionExtension implements Extension {
	private final Map<String, Method> found = new LinkedHashMap<>();

	<T> void collect(@Observes final ProcessAnnotatedType<T> event) {
		for (final AnnotatedMethod<? super T> method : event.getAnnotatedType().getMethods()) {
			final Action action = method.getAnnotation(Action.class);

			if (action != null) {
				found.put(action.value(), method.getJavaMember());
			}
		}
	}

	public Map<String, Method> found() {
		return Map.copyOf(found);
	}
}
