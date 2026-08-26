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

import jakarta.enterprise.inject.spi.CDI;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** What the interface is allowed to ask for, and the methods that carry it out. */
public final class Actions {
	private final Map<String, Method> bound;
	private final Function<Class<?>, Object> resolve;

	private Actions(final Map<String, Method> bound, final Function<Class<?>, Object> resolve) {
		this.bound = bound;
		this.resolve = resolve;
	}

	/** Everything the container found, invoked against the beans it manages. */
	public static Actions discovered(final ActionExtension extension) {
		return new Actions(extension.found(), type -> CDI.current().select(type).get());
	}

	/** Bound to the given objects, for callers that have no container. */
	public static Actions of(final Object... handlers) {
		final Map<String, Method> bound = new LinkedHashMap<>();
		final Map<Class<?>, Object> instances = new LinkedHashMap<>();

		for (final Object handler : handlers) {
			instances.put(handler.getClass(), handler);

			for (final Method method : handler.getClass().getMethods()) {
				final Action action = method.getAnnotation(Action.class);

				if (action != null) {
					bound.put(action.value(), method);
				}
			}
		}

		return new Actions(bound, instances::get);
	}

	public boolean knows(final String name) {
		return bound.containsKey(name);
	}

	/** A bare name is a fragment that needs nothing to render. */
	private static Fragment asFragment(final Object outcome) {
		return outcome instanceof Fragment fragment ? fragment : Fragment.of(outcome.toString());
	}

	/**
	 * Carries out the named action, answering with the fragment the page should show, or nothing
	 * when there is nothing to change.
	 */
	public Optional<Fragment> invoke(final String name, final Form form) {
		final Method method = bound.get(name);

		if (method == null) {
			throw new IllegalArgumentException("No action named " + name);
		}

		try {
			final Object target = resolve.apply(method.getDeclaringClass());
			final Object outcome = method.getParameterCount() == 0
				? method.invoke(target)
				: method.invoke(target, form);

			return Optional.ofNullable(outcome).map(Actions::asFragment);
		} catch (IllegalAccessException e) {
			throw new ActionFailed(name, e);
		} catch (InvocationTargetException e) {
			/* The method itself failed. A wallet must not answer as though nothing went
			   wrong, so the cause travels up rather than being swallowed here. */
			throw new ActionFailed(name, e.getCause());
		}
	}
}
