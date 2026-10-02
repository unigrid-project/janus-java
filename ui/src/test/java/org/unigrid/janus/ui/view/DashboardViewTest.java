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
import java.util.stream.IntStream;
import net.jqwik.api.Example;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.unigrid.janus.ui.view.DashboardView.Bar;
import org.unigrid.janus.ui.view.DashboardView.FundedPage;
import org.unigrid.janus.ui.view.DashboardView.Holding;
import org.unigrid.janus.web.Templates;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DashboardViewTest {
	private static final RowView ROW = new RowView(null, "received", "↓", "Received", "Hone", "+120.00",
		"1 Mar 2019, 10:00", "12", "3,172,655", "aa01", List.of("Hone")
	);

	private static final List<Holding> FUNDED = List.of(new Holding("Hone", "1,200.00", "97.2%", "12", "Mar 2019"),
		new Holding("Htwo", "34.50", "2.8%", "1", "Feb 2019")
	);

	private static DashboardView view(final String awaiting, final boolean listingFunded) {
		return view(awaiting, listingFunded, FUNDED, FundedPage.of(FUNDED, "", 1));
	}

	private static DashboardView view(final String awaiting, final boolean listingFunded,
		final List<Holding> funded, final FundedPage page) {

		return new DashboardView("1,234.50", awaiting, List.of(new Bar(40, "Feb 2019: 500.00 UGD"),
			new Bar(100, "Mar 2019: 1,234.50 UGD")), "Feb 2019", "Mar 2019", "3,172,666", funded, 3, 101, "17",
			"1 Feb 2019 – 1 Mar 2019", List.of(ROW), listingFunded, page
		);
	}

	private static List<Holding> holdings(final int count) {
		return IntStream.rangeClosed(1, count)
			.mapToObj(n -> new Holding("Haddr" + n, n + ".00", "1.0%", "1", "Mar 2019")).toList();
	}

	private static Element opened(final List<Holding> funded, final String query, final int page) {
		return render(view(null, true, funded, FundedPage.of(funded, query, page))).selectFirst(".funded");
	}

	private static DashboardView view(final String awaiting) {
		return view(awaiting, false);
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
	public void shouldLeadTheFiguresToTheWholeActivity() {
		final Document html = render(view(null));

		assertEquals(2, html.select(".stat[hx-post=/action/activity]").size());
		assertEquals("{\"filter\":\"ALL\",\"q\":\"\"}", html.selectFirst(".dashboard__all").attr("hx-vals"));
	}

	@Example
	public void shouldOpenTheFundedAddressesFromTheirFigure() {
		final Document html = render(view(null));
		final Element stat = html.selectFirst(".stat[hx-post=/action/dashboard]");

		assertEquals("2", stat.selectFirst(".stat__value").text());
		assertEquals("{\"funded\":\"true\"}", stat.attr("hx-vals"));
		assertTrue(html.select(".funded").isEmpty());
	}

	@Example
	public void shouldListTheFundedAddressesEachWithAWayToItsActivity() {
		final Element dialog = render(view(null, true)).selectFirst(".funded");
		final Element first = dialog.selectFirst(".funded__address");

		assertEquals("2 of 106 hold funds", dialog.selectFirst(".funded__heading").text());
		assertEquals(List.of("Hone", "Htwo"), dialog.select(".funded__text").eachText());
		assertEquals("1,200.00 UGD share 97.2% txs 12 last Mar 2019", first.selectFirst(".funded__figures").text());
		assertEquals("{\"filter\":\"ALL\",\"q\":\"Hone\"}",
			first.selectFirst(".funded__activity[hx-post=/action/activity]").attr("hx-vals")
		);
		assertEquals("Balances as of frozen block 3,172,666", dialog.selectFirst(".funded__foot").text());
	}

	@Example
	public void shouldCloseTheFundedAddressesByTheirButtonTheBackdropOrEscape() {
		final Element dialog = render(view(null, true)).selectFirst(".funded");

		assertEquals("{\"funded\":\"false\"}", dialog.selectFirst(".funded__close").attr("hx-vals"));
		assertEquals("{\"funded\":\"false\"}", dialog.attr("hx-vals"));
		assertEquals("click target:.funded, keyup[key=='Escape'] from:body", dialog.attr("hx-trigger"));
	}

	@Example
	public void shouldShowFiveFundedAddressesAtATime() {
		final Element dialog = opened(holdings(12), "", 1);

		assertEquals(List.of("Haddr1", "Haddr2", "Haddr3", "Haddr4", "Haddr5"),
			dialog.select(".funded__text").eachText()
		);
		assertEquals("12 of 116 hold funds", dialog.selectFirst(".funded__heading").text());
		assertEquals("1–5 of 12", dialog.selectFirst(".funded__range").text());
	}

	@Example
	public void shouldPageForwardAndBackThroughTheFundedAddresses() {
		final Element middle = opened(holdings(12), "", 2);
		final Element last = opened(holdings(12), "", 3);
		final Element first = opened(holdings(12), "", 1);

		assertEquals(List.of("Haddr6", "Haddr7", "Haddr8", "Haddr9", "Haddr10"),
			middle.select(".funded__text").eachText()
		);
		assertEquals("6–10 of 12", middle.selectFirst(".funded__range").text());
		assertEquals("{\"page\":\"1\"}", middle.selectFirst(".funded__previous").attr("hx-vals"));
		assertEquals("{\"page\":\"3\"}", middle.selectFirst(".funded__next").attr("hx-vals"));
		assertEquals(List.of("Haddr11", "Haddr12"), last.select(".funded__text").eachText());
		assertEquals("11–12 of 12", last.selectFirst(".funded__range").text());
		assertTrue(last.selectFirst(".funded__next").hasAttr("disabled"));
		assertFalse(last.selectFirst(".funded__previous").hasAttr("disabled"));
		assertTrue(first.selectFirst(".funded__previous").hasAttr("disabled"));
	}

	@Example
	public void shouldReplaceOnlyTheListWhenPaging() {
		final Element dialog = opened(holdings(12), "", 2);

		for (final String button : List.of(".funded__previous", ".funded__next")) {
			assertEquals("/action/funded", dialog.selectFirst(button).attr("hx-post"));
			assertEquals("#funded-body", dialog.selectFirst(button).attr("hx-target"));
			assertEquals("outerHTML", dialog.selectFirst(button).attr("hx-swap"));
		}

		assertEquals(1, dialog.select("#funded-body.funded__body").size());
		assertTrue(dialog.select("#funded-body .funded__search").isEmpty());
	}

	@Example
	public void shouldNotPageAFewFundedAddresses() {
		final Element dialog = opened(holdings(5), "", 1);

		assertEquals(5, dialog.select(".funded__text").size());
		assertTrue(dialog.select(".funded__previous, .funded__next").isEmpty());
	}

	@Example
	public void shouldFilterTheFundedAddressesByWhatTheirAddressHolds() {
		final Element dialog = opened(holdings(12), "ADDR1", 1);

		assertEquals(List.of("Haddr1", "Haddr10", "Haddr11", "Haddr12"), dialog.select(".funded__text").eachText());
		assertEquals("1–4 of 4", dialog.selectFirst(".funded__range").text());
		assertEquals("12 of 116 hold funds", dialog.selectFirst(".funded__heading").text());
	}

	@Example
	public void shouldOfferAFilterThatSendsWhatIsTypedAndStartsAtTheFirstPage() {
		final Element search = opened(holdings(12), "Haddr1", 1).selectFirst(".funded__search input[name=fq]");

		assertEquals("Haddr1", search.attr("value"));
		assertEquals("/action/funded", search.attr("hx-post"));
		assertEquals("#funded-body", search.attr("hx-target"));
		assertEquals("outerHTML", search.attr("hx-swap"));
		assertEquals("input changed delay:300ms, search", search.attr("hx-trigger"));
	}

	@Example
	public void shouldSayWhenNoFundedAddressMatchesTheFilter() {
		final Element dialog = opened(holdings(12), "zzz", 1);

		assertTrue(dialog.select(".funded__address").isEmpty());
		assertEquals("No address matches", dialog.selectFirst(".funded__none").text());
		assertTrue(dialog.select(".funded__range, .funded__previous, .funded__next").isEmpty());
	}

	@Example
	public void shouldStayWithinTheFirstAndLastPageWhateverPageIsAskedFor() {
		assertEquals(3, FundedPage.of(holdings(12), "", 99).number());
		assertEquals(1, FundedPage.of(holdings(12), "", 0).number());
		assertEquals(1, FundedPage.of(holdings(12), "", -4).number());
		assertEquals(1, FundedPage.of(List.of(), "", 5).number());
		assertEquals(1, FundedPage.of(List.of(), "", 5).pages());
	}

	@Example
	public void shouldDivideTheRingByTheShareOfEachKindOfAddress() {
		assertEquals("conic-gradient(var(--up) 0 1.9%, var(--accent) 1.9% 4.7%, var(--border-strong) 4.7% 100%)",
			view(null).ring()
		);
	}
}
