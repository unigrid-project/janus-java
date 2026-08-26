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

import java.util.Map;
import net.jqwik.api.Example;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TemplatesTest {
	private final Templates templates = new Templates(false);

	@Example
	public void shouldResolveTemplateFromClasspath() {
		final String html = templates.render("index", Map.of("title", "Janus"));

		assertTrue(html.contains("<!DOCTYPE html>"), html);
	}

	@Example
	public void shouldEvaluateExpressionsRatherThanEchoTheFile() {
		final String html = templates.render("index", Map.of("title", "Unigrid"));

		assertTrue(html.contains("<h1>Unigrid</h1>"), html);
		assertTrue(!html.contains("th:text"), "the th: attributes should be consumed during rendering");
	}
}
