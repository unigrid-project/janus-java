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
import java.awt.Rectangle;

/** Where a window first appears: at the size it prefers, unless the screen has less room than that. */
public final class OpeningBounds {
	private OpeningBounds() {
	}

	public static Rectangle centred(final Dimension preferred, final Rectangle workArea) {
		final int width = Math.min(preferred.width, workArea.width);
		final int height = Math.min(preferred.height, workArea.height);

		return new Rectangle(workArea.x + (workArea.width - width) / 2, workArea.y + (workArea.height - height) / 2,
			width, height);
	}
}
