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

package org.unigrid.janus.web;

/**
 * The operations a page cannot perform on its own window. Implemented by whatever hosts the
 * interface, so that the pages stay unaware of whether they are in a frame or a browser tab.
 */
public interface WindowControl {
	WindowControl NONE = new WindowControl() { };

	default void minimise() {
	}

	default void toggleMaximise() {
	}

	default void close() {
	}

	/**
	 * Begins moving the window with the pointer. Only the start and the end are reported, because
	 * a request per pointer movement would make dragging as slow as the round trip.
	 */
	default void beginMove() {
	}

	default void endMove() {
	}
}
