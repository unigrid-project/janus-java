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
import org.unigrid.janus.ui.view.ActivityView.Summary;
import org.unigrid.janus.web.Templates;
import org.unigrid.janus.web.action.View;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ActivityViewTest {
	private static final RowView ROW = new RowView("March 2019", "sent", "↑", "Sent", "Hone", "−480.00",
		"2 Mar 2019, 08:00", "9", "3,172,658", "bb01", List.of("Hone")
	);
	private static final List<Summary> SUMMARIES = List.of(
		new Summary(Filter.RECEIVED, "Received", "+1,020.00", "received"),
		new Summary(Filter.SENT, "Sent", "−480.00", "sent"),
		new Summary(Filter.REWARDS, "Rewards", "+0.00", "rewards"),
		new Summary(Filter.ALL, "Net", "+540.00", "net")
	);

	private final Templates templates = new Templates(false);

	private Document render(final View view) {
		return Jsoup.parseBodyFragment(templates.render(view));
	}

	@Example
	public void shouldMarkTheFilterAndKeepWhatWasSearchedFor() {
		final Document html = render(new ActivityView(Filter.SENT, "<\"", SUMMARIES,
			new RowsView(List.of(ROW), 100, true, false), ExportNoteView.NONE
		));

		assertEquals(List.of("All", "Received", "Sent", "Rewards"), html.select(".filter").eachText());
		assertEquals("Sent", html.selectFirst(".filter--active").text());
		assertEquals("{\"filter\":\"SENT\"}", html.selectFirst(".filter--active").attr("hx-vals"));
		assertEquals("<\"", html.selectFirst("input[name=q]").attr("value"));
		assertTrue(templates.render(new ActivityView(Filter.SENT, "<\"", SUMMARIES,
			new RowsView(List.of(ROW), 100, true, false), ExportNoteView.NONE
		)).contains("value=\"&lt;&quot;\""));
		assertEquals(List.of("+1,020.00", "−480.00", "+0.00", "+540.00"),
			html.select(".summary__value").eachText()
		);
		assertTrue(html.selectFirst(".summary[hx-vals*=SENT]").hasClass("summary--active"));
	}

	@Example
	public void shouldOfferMoreWhenThereIsMore() {
		final Document html = render(new ActivityView(Filter.ALL, "", SUMMARIES,
			new RowsView(List.of(ROW), 100, false, false), ExportNoteView.NONE
		));

		assertEquals("{\"offset\":\"100\"}", html.selectFirst("#rows .ledger__more").attr("hx-vals"));
		assertEquals("this", html.selectFirst(".ledger__more").attr("hx-target"));
	}

	@Example
	public void shouldContinueTheListWithoutWrappingItAgain() {
		final Document html = render(new RowsView(List.of(ROW), null, false, true));

		assertTrue(html.select("#rows").isEmpty());
		assertEquals(1, html.select("details.ledger__row").size());
		assertTrue(html.select(".ledger__more").isEmpty());
	}

	@Example
	public void shouldTellAnEmptyWalletFromAFilterThatMatchedNothing() {
		assertEquals("This wallet has no transactions.",
			render(new RowsView(List.of(), null, false, false)).selectFirst(".ledger__empty").text()
		);
		assertEquals("No transactions match your filter.",
			render(new RowsView(List.of(), null, true, false)).selectFirst(".ledger__empty").text()
		);
	}

	@Example
	public void shouldSaveTheHistoryThroughTheHostsDialog() {
		final Document html = render(new ActivityView(Filter.ALL, "", SUMMARIES,
			new RowsView(List.of(ROW), null, false, false), new ExportNoteView("Saved to /tmp/h.csv", false)
		));

		assertEquals("unigrid-legacy-history.csv", html.selectFirst("[data-save-file]").attr("data-file-name"));
		assertEquals("#export-note", html.selectFirst("[data-save-file]").attr("hx-target"));
		assertEquals("Saved to /tmp/h.csv", html.selectFirst("#export-note").text());
		assertFalse(html.selectFirst("#export-note").hasClass("export-note--failed"));
	}
}
