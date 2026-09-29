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

import java.awt.Image;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WindowIconTest {
	@Test
	public void shouldProvideTheLogoAtEveryStandardIconSize() {
		assertEquals(
			List.of(16, 24, 32, 48, 64, 128, 256),
			WindowIcon.images().stream().map(image -> image.getWidth(null)).toList()
		);
	}

	@Test
	public void shouldDrawEverySizeSquare() {
		for (Image image : WindowIcon.images()) {
			assertEquals(image.getWidth(null), image.getHeight(null));
		}
	}
}
