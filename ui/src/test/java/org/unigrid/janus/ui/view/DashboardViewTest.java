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
import org.unigrid.janus.ui.view.DashboardView.Bar;
import org.unigrid.janus.web.Templates;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DashboardViewTest {
	private static final RowView ROW = new RowView(null, "received", "↓", "Received", "Hone", "+120.00",
		"1 Mar 2019, 10:00", "12", "3,172,655", "aa01", List.of("Hone")
	);

	private static DashboardView view(final String awaiting) {
		return new DashboardView("1,234.50", awaiting, List.of(new Bar(40, "Feb 2019: 500.00 UGD"),
			new Bar(100, "Mar 2019: 1,234.50 UGD")), "Feb 2019", "Mar 2019", "3,172,666", 2, 3, 101, "17",
			"1 Feb 2019 – 1 Mar 2019", List.of(ROW)
		);
	}

	private static Document render(final DashboardView view) {
		return Jsoup.parseBodyFragment(new Templates(false).render(view));
	}

	@Example
	public void shouldShowTheTotalWithItsHistoryAndAddresses() {
		final Document html = render(view(null));

		assertEquals("1,234.50", html.selectFirst(".dashboard__total").text());
		assertTrue(html.select(".dashboard__awaiting").isEmpty());
		assertEquals(List.of("height:40%", "height:100%"), html.select(".dashboard__bar").eachAttr("style"));
		assertEquals("Mar 2019: 1,234.50 UGD", html.select(".dashboard__bar").get(1).attr("title"));
		assertEquals("106", html.selectFirst(".dashboard__ring-count").text());
		assertEquals(List.of("2", "3", "101"), html.select(".dashboard__legend-count").eachText());
		assertTrue(html.text().contains("Ledger frozen at block 3,172,666"));
		assertEquals(1, html.select("details.ledger__row").size());
	}

	@Example
	public void shouldSayWhatIsStillAwaitingItsMint() {
		assertEquals("Includes 25.00 UGD awaiting mint",
			render(view("25.00")).selectFirst(".dashboard__awaiting").text()
		);
	}

	@Example
	public void shouldLeadEveryFigureToTheActivity() {
		final Document html = render(view(null));

		assertEquals(3, html.select(".stat[hx-post=/action/activity]").size());
		assertEquals("{\"filter\":\"ALL\"}", html.selectFirst(".dashboard__all").attr("hx-vals"));
	}

	@Example
	public void shouldDivideTheRingByTheShareOfEachKindOfAddress() {
		assertEquals("conic-gradient(var(--up) 0 1.9%, var(--accent) 1.9% 4.7%, var(--border-strong) 4.7% 100%)",
			view(null).ring()
		);
	}
}
