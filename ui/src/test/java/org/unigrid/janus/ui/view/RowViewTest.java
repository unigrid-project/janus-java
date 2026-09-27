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

package org.unigrid.janus.ui.view;

import java.util.List;
import net.jqwik.api.Example;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.unigrid.janus.web.Templates;
import org.unigrid.janus.web.action.View;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RowViewTest {
	private record Rows(List<RowView> rows) implements View {
		@Override
		public String template() {
			return "row-test :: rows";
		}
	}

	private static final RowView SENT = new RowView("March 2019", "sent", "↑", "Sent", "Hsender +1", "−480.00",
		"2 Mar 2019, 08:00", "3,172,658", "9", "bb01", List.of("Hchange", "Hsender")
	);
	private static final RowView MINED = new RowView(null, "mined", "✦", "Mined", "Hone", "+50.00",
		"1 Mar 2019, 08:00", "3,172,664", "3", "aa01", List.of("Hone")
	);

	@Example
	public void shouldHeadEachMonthAndOpenEachRowOntoItsDetails() {
		final Document html = Jsoup.parseBodyFragment(new Templates(false).render(new Rows(List.of(SENT, MINED))));

		assertEquals(List.of("March 2019"), html.select(".ledger__month").eachText());
		assertEquals(2, html.select("details.ledger__row").size());
		assertTrue(html.selectFirst("details").hasClass("ledger__row--sent"));
		assertEquals("−480.00", html.selectFirst(".ledger__amount").text());
		assertEquals("Hsender +1", html.selectFirst(".ledger__party").text());
		assertEquals("3,172,658", html.selectFirst(".ledger__confirmations").text());
		assertEquals(List.of("Hchange", "Hsender"),
			html.selectFirst("details").select(".ledger__address").eachText()
		);
		assertEquals("bb01", html.selectFirst(".ledger__txid").text());
	}
}
