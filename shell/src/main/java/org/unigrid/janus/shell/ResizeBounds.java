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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import org.unigrid.janus.web.WindowControl.Edge;

/** Where a window ends up once one of its edges has travelled with the pointer. */
public final class ResizeBounds {
	private ResizeBounds() {
	}

	public static Rectangle resized(final Edge edge, final Rectangle origin, final Point travel,
		final Dimension minimum) {

		final int wider = Math.max(minimum.width, origin.width + travel.x);
		final int taller = Math.max(minimum.height, origin.height + travel.y);

		return switch (edge) {
			case LEFT -> {
				final int width = Math.max(minimum.width, origin.width - travel.x);

				yield new Rectangle(origin.x + origin.width - width, origin.y, width, origin.height);
			}
			case RIGHT -> new Rectangle(origin.x, origin.y, wider, origin.height);
			case BOTTOM -> new Rectangle(origin.x, origin.y, origin.width, taller);
			case BOTTOM_RIGHT -> new Rectangle(origin.x, origin.y, wider, taller);
		};
	}
}
