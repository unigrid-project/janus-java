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
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OpeningBoundsTest {
	private static final Dimension PREFERRED = new Dimension(1240, 800);

	@Test
	public void shouldOpenAtThePreferredSizeInTheMiddleOfARoomyScreen() {
		assertEquals(
			new Rectangle(340, 120, 1240, 800),
			OpeningBounds.centred(PREFERRED, new Rectangle(0, 0, 1920, 1040))
		);
	}

	@Test
	public void shouldShrinkToTheHeightOfAShortScreen() {
		assertEquals(
			new Rectangle(63, 0, 1240, 728),
			OpeningBounds.centred(PREFERRED, new Rectangle(0, 0, 1366, 728))
		);
	}

	@Test
	public void shouldShrinkToTheWidthOfANarrowScreen() {
		assertEquals(
			new Rectangle(0, 20, 1024, 800),
			OpeningBounds.centred(PREFERRED, new Rectangle(0, 0, 1024, 840))
		);
	}

	@Test
	public void shouldStayClearOfATaskbarAtTheTopOrLeft() {
		assertEquals(
			new Rectangle(48, 32, 1232, 688),
			OpeningBounds.centred(PREFERRED, new Rectangle(48, 32, 1232, 688))
		);
	}
}
