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

package org.unigrid.janus.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.BoundingBox;
import java.util.List;
import net.jqwik.api.Example;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WindowIT extends BrowserTest {
	private static final String TITLE = ".titlebar__title";
	private static final int DRAG = 40;

	@Example
	public void shouldMaximiseOnADoubleClickOnTheTitleBar() throws Exception {
		page().dblclick(TITLE);
		assertTrue(rig().window().await(5, WAIT).contains("maximise"), rig().window().commands()::toString);
	}

	@Example
	public void shouldMoveTheWindowWhileTheTitleBarIsHeld() throws Exception {
		drag(page().locator(TITLE));
		assertEquals(List.of("move/start", "move/end"), rig().window().await(2, WAIT));
	}

	@Example
	public void shouldResizeTheWindowFromEachEdge() throws Exception {
		final List<String> edges = page().locator("[data-resize]").all().stream()
			.map(handle -> handle.getAttribute("data-resize")).toList();

		for (final String edge : edges) {
			drag(page().locator("[data-resize='" + edge + "']"));
		}

		assertEquals(edges.stream().flatMap(edge -> List.of("resize/start/" + edge, "resize/end").stream()).toList(),
			rig().window().await(edges.size() * 2, WAIT)
		);
	}

	private void drag(final Locator from) {
		final BoundingBox box = from.boundingBox();
		final double x = box.x + box.width / 2;
		final double y = box.y + box.height / 2;

		page().mouse().move(x, y);
		page().mouse().down();
		page().mouse().move(x + DRAG, y + DRAG);
		page().mouse().up();
	}
}
