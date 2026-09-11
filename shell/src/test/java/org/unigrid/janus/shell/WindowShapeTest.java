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
import java.awt.Frame;
import java.awt.geom.RoundRectangle2D;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WindowShapeTest {
	private static final Dimension SIZE = new Dimension(1100, 720);

	@Test
	public void shouldRoundTheCornersOfANormalWindowByTwelvePixels() {
		assertEquals(
			new RoundRectangle2D.Double(0, 0, 1100, 720, 24, 24),
			WindowShape.of(Frame.NORMAL, SIZE).orElseThrow()
		);
	}

	@Test
	public void shouldKeepTheCornersOfAMaximisedWindowSquare() {
		assertTrue(WindowShape.of(Frame.MAXIMIZED_BOTH, SIZE).isEmpty());
	}

	@Test
	public void shouldStaySquareWhenMaximisedTogetherWithOtherStates() {
		assertTrue(WindowShape.of(Frame.MAXIMIZED_BOTH | Frame.ICONIFIED, SIZE).isEmpty());
	}
}
